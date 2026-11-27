import { apiFetch, ApiError } from "./api.js";
import { AUTH_CHANGED_EVENT, AUTH_REQUIRED_EVENT, obtenerPermisos } from "./auth.js";
import {
    escaparHtml,
    obtenerTituloError,
    obtenerListaErrores,
    agregarDetalleErrores
} from "./utils.js";

const btnValidar = document.getElementById("btnValidar");
const panelValidacion = document.getElementById("panelValidacion");
const scannerContainer = document.getElementById("scannerContainer");
const resultadoScanner = document.getElementById("resultadoScanner");
const btnIniciarScanner = document.getElementById("btnIniciarScanner");

let scannerInitDone = false;
let qrScanner = null;
let scannerActivo = false;
let codigoQrActual = null;
let puedeEscanear = false;

const confirmacionesScanner = new WeakMap();

function confirmarAccionScanner(boton, textoConfirmar) {
    if (!boton) return false;

    const confirmacion = confirmacionesScanner.get(boton);

    if (confirmacion) {
        clearTimeout(confirmacion.timer);
        confirmacionesScanner.delete(boton);
        return true;
    }

    const textoOriginal = boton.textContent;
    boton.textContent = textoConfirmar;

    const timer = setTimeout(() => {
        if (!confirmacionesScanner.has(boton)) return;
        confirmacionesScanner.delete(boton);
        if (boton.isConnected && textoOriginal !== undefined) {
            boton.textContent = textoOriginal;
        }
    }, 6000);

    confirmacionesScanner.set(boton, { timer });

    return false;
}

export function initScanner() {
    if (scannerInitDone) return;
    scannerInitDone = true;

    btnValidar?.addEventListener("click", mostrarPanelValidacion);
    btnIniciarScanner?.addEventListener("click", iniciarScanner);

    window.addEventListener(AUTH_CHANGED_EVENT, manejarCambioAutenticacion);

    ocultarPanelValidacion();
}

function manejarCambioAutenticacion(event) {
    const autenticado = event.detail?.autenticado === true;
    const permisos = event.detail?.permisos ?? obtenerPermisos(event.detail?.usuario);

    puedeEscanear = autenticado && permisos.puedeEscanear === true;

    if (!puedeEscanear) {
        ocultarPanelValidacion();
    }
}

function ocultarPanelValidacion() {
    panelValidacion?.classList.add("hidden");
    detenerScanner();
}

async function mostrarPanelValidacion() {
    if (!puedeEscanear) return;

    panelValidacion?.classList.remove("hidden");

    panelValidacion?.scrollIntoView({
        behavior: "smooth",
        block: "start"
    });
}

async function iniciarScanner() {
    if (!puedeEscanear || scannerActivo) return;

    limpiarResultadoScanner();

    if (!scannerContainer) return;

    if (typeof Html5Qrcode === "undefined") {
        scannerContainer.innerHTML = `
            <div class="scanner-placeholder">
                <span>No se pudo cargar la librería del escáner. Revisa tu conexión e inténtalo de nuevo.</span>
            </div>
        `;
        return;
    }

    scannerContainer.innerHTML = `
        <div id="lectorQr" class="qr-reader"></div>
    `;

    qrScanner = new Html5Qrcode("lectorQr");
    scannerActivo = true;

    try {
        await qrScanner.start(
            { facingMode: "environment" },
            {
                fps: 10,
                qrbox: {
                    width: 250,
                    height: 250
                }
            },
            manejarQrDetectado,
            () => {}
        );
    } catch {
        scannerActivo = false;
        qrScanner = null;

        scannerContainer.innerHTML = `
            <div class="scanner-placeholder">
                <span>No fue posible acceder a la cámara. La cámara requiere HTTPS o localhost.</span>
                <button id="btnReintentarScanner" class="btn btn-primary" type="button">
                    Intentar nuevamente
                </button>
            </div>
        `;

        scannerContainer
            .querySelector("#btnReintentarScanner")
            ?.addEventListener("click", iniciarScanner);
    }
}

async function manejarQrDetectado(codigoQr) {
    if (!codigoQr || !scannerActivo) return;

    const codigo = extraerCodigoQr(codigoQr);

    if (!codigo) return;

    codigoQrActual = codigo;

    await detenerScanner();

    await verificarBoletoEscaneado(codigo);
}

function extraerCodigoQr(texto) {
    const valor = (texto || "").trim();

    if (!valor) return "";

    try {
        const url = new URL(valor);
        return url.searchParams.get("codigoQr")?.trim() || valor;
    } catch {
        return valor;
    }
}

async function detenerScanner() {
    if (!qrScanner) {
        scannerActivo = false;
        return;
    }

    try {
        await qrScanner.stop();
    } catch {}

    try {
        await qrScanner.clear();
    } catch {}

    qrScanner = null;
    scannerActivo = false;
}

async function verificarBoletoEscaneado(codigoQr) {
    mostrarCargandoResultadoScanner();

    try {
        const boleto = await apiFetch(
            `/boletos/verificar/${encodeURIComponent(codigoQr)}`
        );

        renderizarResultadoScanner(boleto);
    } catch (error) {
        mostrarErrorScanner(error, "No fue posible verificar el boleto");
    }
}

function mostrarCargandoResultadoScanner() {
    if (!resultadoScanner) return;

    resultadoScanner.classList.remove("hidden");

    resultadoScanner.innerHTML = `
        <div class="scanner-message">
            <p>Verificando boleto...</p>
        </div>
    `;
}

function renderizarResultadoScanner(boleto) {
    if (!resultadoScanner) return;

    resultadoScanner.classList.remove("hidden");

    const claseEstado = boleto.valido
        ? "scanner-status-valid"
        : "scanner-status-invalid";

    const textoValidez = boleto.valido
        ? "Boleto válido"
        : "Boleto no válido";

    resultadoScanner.innerHTML = `
        <div class="scanner-ticket-result">
            <div class="scanner-ticket-header">
                <span class="${claseEstado}">
                    ${escaparHtml(textoValidez)}
                </span>
            </div>

            <div class="scanner-ticket-info">
                <div>
                    <span>Evento</span>
                    <strong>${escaparHtml(boleto.evento)}</strong>
                </div>

                <div>
                    <span>Tipo de boleto</span>
                    <strong>${escaparHtml(boleto.tipoBoleto)}</strong>
                </div>

                <div>
                    <span>Titular</span>
                    <strong>${escaparHtml(boleto.titular)}</strong>
                </div>

                <div>
                    <span>Folio</span>
                    <strong>${escaparHtml(boleto.folio)}</strong>
                </div>

                <div>
                    <span>Estado</span>
                    <strong>${escaparHtml(boleto.estado)}</strong>
                </div>
            </div>

            ${
                boleto.valido && puedeEscanear
                    ? `
                        <div class="scanner-ticket-actions">
                            <button
                                id="btnUsarBoleto"
                                class="btn btn-primary"
                                type="button"
                            >
                                Usar boleto
                            </button>

                            <button
                                id="btnNuevoEscaneo"
                                class="btn btn-outline"
                                type="button"
                            >
                                Escanear otro
                            </button>
                        </div>
                    `
                    : `
                        <div class="scanner-ticket-actions">
                            <button
                                id="btnNuevoEscaneo"
                                class="btn btn-outline"
                                type="button"
                            >
                                Escanear otro
                            </button>
                        </div>
                    `
            }
        </div>
    `;

    document
        .getElementById("btnUsarBoleto")
        ?.addEventListener("click", usarBoletoEscaneado);

    document
        .getElementById("btnNuevoEscaneo")
        ?.addEventListener("click", prepararNuevoEscaneo);
}

async function usarBoletoEscaneado() {
    if (!codigoQrActual || !puedeEscanear) return;

    const boton = document.getElementById("btnUsarBoleto");

    if (!confirmarAccionScanner(boton, "⚠ Confirmar uso")) {
        return;
    }

    if (boton) {
        boton.disabled = true;
        boton.textContent = "Procesando...";
    }

    try {
        const resultado = await apiFetch(
            `/boletos/${encodeURIComponent(codigoQrActual)}/usar`,
            {
                method: "POST",
                auth: true
            }
        );

        renderizarBoletoUsado(resultado);

    } catch (error) {
        mostrarErrorScanner(error, "No fue posible usar el boleto");

        if (error instanceof ApiError && error.status === 401) {
            window.dispatchEvent(new CustomEvent(AUTH_REQUIRED_EVENT));
        }

        if (boton) {
            boton.disabled = false;
            boton.textContent = "Usar boleto";
        }
    }
}

function renderizarBoletoUsado(resultado) {
    if (!resultadoScanner) return;

    resultadoScanner.classList.remove("hidden");

    resultadoScanner.innerHTML = `
        <div class="scanner-ticket-result scanner-ticket-success">
            <div class="scanner-ticket-header">
                <span class="scanner-status-valid">
                    Acceso permitido
                </span>
            </div>

            <div class="scanner-ticket-info">
                <div>
                    <span>Evento</span>
                    <strong>${escaparHtml(resultado.evento)}</strong>
                </div>

                <div>
                    <span>Titular</span>
                    <strong>${escaparHtml(resultado.titular)}</strong>
                </div>

                <div>
                    <span>Folio</span>
                    <strong>${escaparHtml(resultado.folio)}</strong>
                </div>

                <div>
                    <span>Estado</span>
                    <strong>${escaparHtml(resultado.estado)}</strong>
                </div>
            </div>

            <div class="scanner-success-message">
                <p>${escaparHtml(resultado.mensaje)}</p>
            </div>

            <div class="scanner-ticket-actions">
                <button
                    id="btnNuevoEscaneo"
                    class="btn btn-primary"
                    type="button"
                >
                    Escanear otro boleto
                </button>
            </div>
        </div>
    `;

    document
        .getElementById("btnNuevoEscaneo")
        ?.addEventListener("click", prepararNuevoEscaneo);
}

async function prepararNuevoEscaneo() {
    codigoQrActual = null;

    limpiarResultadoScanner();

    await iniciarScanner();
}

function limpiarResultadoScanner() {
    if (!resultadoScanner) return;

    resultadoScanner.innerHTML = "";
    resultadoScanner.classList.add("hidden");
}

function mostrarErrorScanner(error, mensajeFallback = "No fue posible verificar el boleto.") {
    if (!resultadoScanner) return;

    let titulo = obtenerTituloError(mensajeFallback);

    if (error instanceof ApiError && error.status === 401) {
        titulo = "Tu sesión expiró. Inicia sesión nuevamente.";
    } else if (error instanceof ApiError && error.status === 403) {
        titulo = "Tu rol no tiene permiso para usar el boleto.";
    }

    const errores = obtenerListaErrores(error?.data);

    resultadoScanner.classList.remove("hidden");
    resultadoScanner.innerHTML = "";

    const contenedor = document.createElement("div");
    contenedor.className = "scanner-message scanner-message-error";

    const tituloElemento = document.createElement("strong");
    tituloElemento.textContent = titulo;

    contenedor.appendChild(tituloElemento);

    agregarDetalleErrores(
        contenedor,
        errores,
        "admin-error-list"
    );

    const acciones = document.createElement("div");
    acciones.className = "scanner-ticket-actions";

    const boton = document.createElement("button");
    boton.id = "btnNuevoEscaneo";
    boton.type = "button";
    boton.className = "btn btn-outline";
    boton.textContent = "Escanear otro boleto";

    boton.addEventListener("click", prepararNuevoEscaneo);

    acciones.appendChild(boton);
    contenedor.appendChild(acciones);

    resultadoScanner.appendChild(contenedor);
}
