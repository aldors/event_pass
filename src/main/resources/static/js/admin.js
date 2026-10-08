import { apiFetch } from "./api.js";
import { AUTH_CHANGED_EVENT } from "./auth.js";
import {
    escaparHtml,
    obtenerTituloError,
    obtenerListaErrores,
    agregarDetalleErrores
} from "./utils.js";

const btnAdmin = document.getElementById("btnAdmin");
const panelAdmin = document.getElementById("panelAdmin");

const tabAdminEventos = document.getElementById("tabAdminEventos");
const tabAdminScanner = document.getElementById("tabAdminScanner");

const adminEventos = document.getElementById("adminEventos");
const adminScanner = document.getElementById("adminScanner");

const listaAdminEventos = document.getElementById("listaAdminEventos");
const detalleAdminEvento = document.getElementById("detalleAdminEvento");

const btnCrearEvento = document.getElementById("btnCrearEvento");
const btnCerrarCrearEvento = document.getElementById("btnCerrarCrearEvento");
const btnCancelarCrearEvento = document.getElementById("btnCancelarCrearEvento");

const formCrearEvento = document.getElementById("crearEventoForm");
const formCrearEventoContainer = document.getElementById("formCrearEvento");
const mensajeCrearEvento = document.getElementById("mensajeCrearEvento");

const btnGuardarEvento = document.getElementById("btnGuardarEvento");

const eventoNombre = document.getElementById("eventoNombre");
const eventoDescripcion = document.getElementById("eventoDescripcion");
const eventoUbicacion = document.getElementById("eventoUbicacion");
const eventoFechaInicio = document.getElementById("eventoFechaInicio");
const eventoFechaFin = document.getElementById("eventoFechaFin");
const eventoMaxBoletos = document.getElementById("eventoMaxBoletos");

const scannerContainer = document.getElementById("scannerContainer");
const resultadoScanner = document.getElementById("resultadoScanner");
const btnIniciarScanner = document.getElementById("btnIniciarScanner");

let qrScanner = null;
let scannerActivo = false;
let codigoQrActual = null;

let adminInitDone = false;
let eventoAdminActualId = null;

/* Claves de confirmación pendientes (doble clic, revierten a los 6s). */
const confirmacionesAdmin = new Map();

/*
 * Patrón de confirmación en 2 clics (igual que el pago):
 * primer clic arma, segundo ejecuta. Devuelve true si ya estaba armado.
 */
function confirmarAccionAdmin(clave, boton, textoConfirmar) {
    if (confirmacionesAdmin.has(clave)) {
        confirmacionesAdmin.delete(clave);
        return true;
    }

    confirmacionesAdmin.set(clave, true);

    const textoOriginal = boton?.textContent;

    if (boton) boton.textContent = textoConfirmar;

    setTimeout(() => {
        if (!confirmacionesAdmin.delete(clave)) return;
        if (boton?.isConnected && textoOriginal !== undefined) {
            boton.textContent = textoOriginal;
        }
    }, 6000);

    return false;
}

export function initAdmin() {
    if (adminInitDone) return;
    adminInitDone = true;

    btnAdmin?.addEventListener("click", mostrarPanelAdmin);
    tabAdminEventos?.addEventListener("click", mostrarEventosAdmin);
    tabAdminScanner?.addEventListener("click", mostrarScannerAdmin);

    listaAdminEventos?.addEventListener("click", manejarClickEvento);

    btnCrearEvento?.addEventListener("click", mostrarFormularioCrearEvento);

    btnCerrarCrearEvento?.addEventListener("click", ocultarFormularioCrearEvento);
    btnCancelarCrearEvento?.addEventListener("click", ocultarFormularioCrearEvento);
    formCrearEvento?.addEventListener("submit", manejarCrearEvento);

    btnIniciarScanner?.addEventListener("click", iniciarScanner);

    window.addEventListener(AUTH_CHANGED_EVENT, manejarCambioAutenticacion);

    ocultarAdmin();
}

async function iniciarScanner() {
    if (scannerActivo) return;

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
            () => {
                // Ignoramos errores de lectura individuales.
            }
        );
    } catch (error) {
        scannerActivo = false;
        qrScanner = null;

        scannerContainer.innerHTML = `
            <div class="scanner-placeholder">
                <span>No fue posible acceder a la cámara. La cámara requiere HTTPS o localhost.</span>
                <button id="btnIniciarScanner" class="btn btn-primary" type="button">
                    Intentar nuevamente
                </button>
            </div>
        `;

        scannerContainer
            .querySelector("#btnIniciarScanner")
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

/*
 * El QR impreso en el PDF contiene la URL completa de verificación
 * (.../verificar.html?codigoQr=<uuid>). El backend espera solo el uuid,
 * así que se extrae el parámetro; si no es URL, se usa el texto tal cual.
 */
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
    } catch {
        // El escáner puede haberse detenido previamente.
    }

    try {
        await qrScanner.clear();
    } catch {
        // Los recursos pueden haberse liberado previamente.
    }

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
                boleto.valido
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
        ?.addEventListener("click", usarBoletoAdmin);

    document
        .getElementById("btnNuevoEscaneo")
        ?.addEventListener("click", prepararNuevoEscaneo);
}

async function usarBoletoAdmin() {
    if (!codigoQrActual) return;

    const boton = document.getElementById("btnUsarBoleto");

    // Marcar UTILIZADO no tiene reversa: se exige confirmación.
    if (!confirmarAccionAdmin("usar-boleto", boton, "⚠ Confirmar uso")) {
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

    const titulo = obtenerTituloError(mensajeFallback);

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

function mostrarFormularioCrearEvento() {
    formCrearEventoContainer?.classList.remove("hidden");

    detalleAdminEvento?.classList.add("hidden");

    listaAdminEventos?.classList.add("hidden");

    limpiarFormularioCrearEvento();

    formCrearEventoContainer?.scrollIntoView({
        behavior: "smooth",
        block: "start"
    });
}

function ocultarFormularioCrearEvento() {
    formCrearEventoContainer?.classList.add("hidden");

    listaAdminEventos?.classList.remove("hidden");

    limpiarMensajeCrearEvento();
}

function limpiarFormularioCrearEvento() {
    formCrearEvento?.reset();

    if (eventoMaxBoletos) {
        eventoMaxBoletos.value = "5";
    }

    limpiarMensajeCrearEvento();
}

function limpiarMensajeCrearEvento() {
    if (!mensajeCrearEvento) return;

    mensajeCrearEvento.innerHTML = "";
    mensajeCrearEvento.classList.add("hidden");
}

async function manejarCrearEvento(event) {
    event.preventDefault();

    limpiarMensajeCrearEvento();

    // Sin validación nativa del navegador (el form ya tiene `novalidate`):
    // el backend es la única fuente de verdad y sus errores se muestran abajo.
    const datos = obtenerDatosFormularioEvento();

    if (!validarFechasEvento(datos)) {
        return;
    }

    setBotonCrearEventoLoading(true);

    try {
        const respuesta = await apiFetch("/admin/eventos/crear", {
            method: "POST",
            auth: true,
            body: datos
        });

        await cargarEventosAdmin();

        ocultarFormularioCrearEvento();

        // Llevar al admin directo al detalle del evento recién creado.
        if (respuesta?.id) {
            await cargarDetalleEventoAdmin(respuesta.id);
        }

    } catch (error) {
        mostrarErrorCrearEvento(error);

    } finally {
        setBotonCrearEventoLoading(false);
    }
}

function obtenerDatosFormularioEvento() {
    return {
        nombre: eventoNombre.value.trim(),
        descripcion: eventoDescripcion.value.trim(),
        ubicacion: eventoUbicacion.value.trim(),
        // `|| null` para que el backend responda @NotNull ("es obligatoria")
        // en vez de un error técnico de deserialización de Jackson.
        fechaInicio: eventoFechaInicio.value || null,
        fechaFin: eventoFechaFin.value || null,
        maxBoletosPorUsuario: Number(eventoMaxBoletos.value)
    };
}

function validarFechasEvento(datos) {
    // Si falta alguna fecha, que lo reporte el backend (@NotNull).
    if (!datos.fechaInicio || !datos.fechaFin) return true;

    const inicio = new Date(datos.fechaInicio);
    const fin = new Date(datos.fechaFin);

    if (Number.isNaN(inicio.getTime()) || Number.isNaN(fin.getTime())) {
        mostrarMensajeCrearEvento(
            "Las fechas del evento no son válidas.",
            true
        );

        return false;
    }

    if (fin <= inicio) {
        mostrarMensajeCrearEvento(
            "La fecha de fin debe ser posterior a la fecha de inicio.",
            true
        );

        return false;
    }

    return true;
}

function setBotonCrearEventoLoading(loading) {
    if (!btnGuardarEvento) return;

    btnGuardarEvento.disabled = loading;

    btnGuardarEvento.textContent = loading
        ? "Creando..."
        : "Crear evento";
}

function mostrarMensajeCrearEvento(mensaje, esError = false) {
    if (!mensajeCrearEvento) return;

    mensajeCrearEvento.textContent = mensaje;

    mensajeCrearEvento.classList.remove(
        "hidden",
        "admin-form-message-error",
        "admin-form-message-success"
    );

    mensajeCrearEvento.classList.add(
        esError
            ? "admin-form-message-error"
            : "admin-form-message-success"
    );
}

function mostrarErrorCrearEvento(error) {
    if (!mensajeCrearEvento) return;

    const titulo = obtenerTituloError("No fue posible crear el evento.");

    const errores = obtenerListaErrores(error?.data);

    mensajeCrearEvento.innerHTML = "";

    const tituloElemento = document.createElement("strong");
    tituloElemento.textContent = titulo;

    mensajeCrearEvento.appendChild(tituloElemento);

    agregarDetalleErrores(
        mensajeCrearEvento,
        errores,
        "admin-error-list"
    );

    mensajeCrearEvento.classList.remove(
        "hidden",
        "admin-form-message-success"
    );

    mensajeCrearEvento.classList.add(
        "admin-form-message-error"
    );
}

function manejarCambioAutenticacion(event) {
    const usuario = event.detail?.usuario;
    const autenticado = event.detail?.autenticado === true;
    const esAdmin = autenticado && usuario?.rol === "ADMIN";

    if (esAdmin) {
        mostrarAccesoAdmin();
    } else {
        ocultarAdmin();
    }
}

function mostrarAccesoAdmin() {
    btnAdmin?.classList.remove("hidden");
}

function ocultarAdmin() {
    btnAdmin?.classList.add("hidden");
    panelAdmin?.classList.add("hidden");
    // No dejar la cámara encendida al salir del panel o cerrar sesión.
    detenerScanner();
}

async function mostrarPanelAdmin() {
    if (panelAdmin?.classList.contains("hidden")) {
        panelAdmin.classList.remove("hidden");
    }

    mostrarEventosAdmin();

    await cargarEventosAdmin();

    panelAdmin?.scrollIntoView({
        behavior: "smooth",
        block: "start"
    });
}

function mostrarEventosAdmin() {
    tabAdminEventos?.classList.add("active");
    tabAdminEventos?.setAttribute("aria-selected", "true");
    tabAdminScanner?.classList.remove("active");
    tabAdminScanner?.setAttribute("aria-selected", "false");

    adminEventos?.classList.remove("hidden");
    adminScanner?.classList.add("hidden");
}

function mostrarScannerAdmin() {
    tabAdminScanner?.classList.add("active");
    tabAdminScanner?.setAttribute("aria-selected", "true");
    tabAdminEventos?.classList.remove("active");
    tabAdminEventos?.setAttribute("aria-selected", "false");

    adminScanner?.classList.remove("hidden");
    adminEventos?.classList.add("hidden");

    cerrarDetalleEventoAdmin();
}

async function cargarEventosAdmin() {
    if (!listaAdminEventos) return;

    listaAdminEventos.innerHTML = `
        <div class="admin-message">
            <p>Cargando eventos...</p>
        </div>
    `;

    try {
        const eventos = await apiFetch("/admin/eventos/obtener", {
            auth: true
        });

        renderizarEventosAdmin(eventos);

    } catch (error) {
        mostrarErrorEventos(error);
    }
}

function renderizarEventosAdmin(eventos) {
    if (!listaAdminEventos) return;

    if (!Array.isArray(eventos) || eventos.length === 0) {
        listaAdminEventos.innerHTML = `
            <div class="admin-message">
                <h4>No hay eventos registrados</h4>
                <p>
                    Crea tu primer evento para comenzar a configurar
                    la venta de boletos.
                </p>
            </div>
        `;

        return;
    }

    listaAdminEventos.innerHTML = eventos
        .map(crearTarjetaEvento)
        .join("");
}

function manejarClickEvento(event) {
    const boton = event.target.closest("[data-admin-evento-id]");

    if (!boton) return;

    const eventoId = boton.dataset.adminEventoId;

    if (!eventoId) return;

    cargarDetalleEventoAdmin(eventoId);
}

async function cargarDetalleEventoAdmin(eventoId) {
    if (!detalleAdminEvento) return;

    eventoAdminActualId = eventoId;

    detalleAdminEvento.classList.remove("hidden");

    detalleAdminEvento.innerHTML = `
        <div class="admin-message">
            <p>Cargando información del evento...</p>
        </div>
    `;

    try {
        const evento = await apiFetch(`/admin/eventos/${eventoId}/obtener`, {
            auth: true
        });

        renderizarDetalleEventoAdmin(evento);

        detalleAdminEvento?.scrollIntoView({
            behavior: "smooth",
            block: "start"
        });

    } catch (error) {
        mostrarErrorDetalleEvento(error);
    }
}

function renderizarDetalleEventoAdmin(evento) {
    if (!detalleAdminEvento) return;

    const tiposBoleto = Array.isArray(evento.tiposBoleto)
        ? evento.tiposBoleto
        : [];

    detalleAdminEvento.innerHTML = `
        <div class="admin-detail-header">
            <div>
                <span class="eyebrow">DETALLE DEL EVENTO</span>
                <h3>${escaparHtml(evento.nombre)}</h3>

                <span class="admin-event-status">
                    ${escaparHtml(evento.estado)}
                </span>
            </div>

            <button
                type="button"
                class="btn btn-outline"
                id="btnCerrarDetalleAdmin"
            >
                Cerrar
            </button>
        </div>

        <div class="admin-detail-info">

            <div class="admin-detail-field">
                <span>Descripción</span>
                <p>${escaparHtml(evento.descripcion)}</p>
            </div>

            <div class="admin-detail-field">
                <span>Ubicación</span>
                <p>${escaparHtml(evento.ubicacion)}</p>
            </div>

            <div class="admin-detail-field">
                <span>Fecha de inicio</span>
                <p>${escaparHtml(formatearFecha(evento.fechaInicio))}</p>
            </div>

            <div class="admin-detail-field">
                <span>Fecha de fin</span>
                <p>${escaparHtml(formatearFecha(evento.fechaFin))}</p>
            </div>

            <div class="admin-detail-field">
                <span>Máximo de boletos por usuario</span>
                <p>${evento.maxBoletosPorUsuario}</p>
            </div>

        </div>

        <div class="admin-detail-actions">
            ${
                evento.estado === "BORRADOR"
                    ? `
                        <button
                            type="button"
                            class="btn btn-primary"
                            id="btnPublicarEvento"
                        >
                            Publicar evento
                        </button>

                        <button
                            type="button"
                            class="btn btn-outline"
                            id="btnEliminarBorrador"
                        >
                            Eliminar borrador
                        </button>
                    `
                    : ""
            }

            ${
                evento.estado === "PUBLICADO"
                    ? `
                        <button
                            type="button"
                            class="btn btn-outline"
                            id="btnCancelarEvento"
                        >
                            Cancelar evento
                        </button>
                    `
                    : ""
            }
        </div>

        <div class="admin-detail-section">
            <div class="admin-detail-section-header">
                <div>
                    <span class="eyebrow">BOLETOS</span>
                    <h4>Tipos de boleto</h4>
                </div>

                ${
                    evento.estado === "BORRADOR"
                        ? `
                            <button
                                type="button"
                                class="btn btn-primary"
                                id="btnAgregarTipoBoleto"
                            >
                                + Agregar tipo
                            </button>
                        `
                        : ""
                }
            </div>

            ${
                tiposBoleto.length > 0
                    ? crearTiposBoletoHtml(tiposBoleto)
                    : `
                        <div class="admin-message">
                            <p>
                                Este evento todavía no tiene tipos de boleto.
                            </p>
                        </div>
                    `
            }

            ${
                evento.estado === "BORRADOR"
                    ? `
                        <div
                            id="formAgregarTipoBoleto"
                            class="admin-form-container hidden"
                        >
                            <div class="admin-detail-header">
                                <div>
                                    <span class="eyebrow">
                                        NUEVO TIPO
                                    </span>

                                    <h4>
                                        Agregar tipo de boleto
                                    </h4>
                                </div>
                            </div>

                            <form
                                id="agregarTipoBoletoForm"
                                novalidate
                            >
                                <div class="admin-form-grid">

                                    <div class="admin-form-group">
                                        <label for="tipoBoletoNombre">
                                            Nombre
                                        </label>

                                        <input
                                            id="tipoBoletoNombre"
                                            name="nombre"
                                            type="text"
                                            maxlength="100"
                                            required
                                        >
                                    </div>

                                    <div class="admin-form-group">
                                        <label for="tipoBoletoPrecio">
                                            Precio
                                        </label>

                                        <input
                                            id="tipoBoletoPrecio"
                                            name="precio"
                                            type="number"
                                            min="0.01"
                                            step="0.01"
                                            required
                                        >
                                    </div>

                                    <div class="admin-form-group">
                                        <label for="tipoBoletoCantidad">
                                            Cantidad
                                        </label>

                                        <input
                                            id="tipoBoletoCantidad"
                                            name="cantidadTotal"
                                            type="number"
                                            min="1"
                                            step="1"
                                            required
                                        >
                                    </div>

                                </div>

                                <div
                                    id="mensajeAgregarTipoBoleto"
                                    class="admin-form-message hidden"
                                    aria-live="polite"
                                ></div>

                                <div class="admin-form-actions">

                                    <button
                                        id="btnCancelarAgregarTipo"
                                        class="btn btn-outline"
                                        type="button"
                                    >
                                        Cancelar
                                    </button>

                                    <button
                                        id="btnGuardarTipoBoleto"
                                        class="btn btn-primary"
                                        type="submit"
                                    >
                                        Agregar tipo
                                    </button>

                                </div>
                            </form>
                        </div>
                    `
                    : ""
            }
        </div>
    `;

    document
        .getElementById("btnCerrarDetalleAdmin")
        ?.addEventListener("click", cerrarDetalleEventoAdmin);

    document
        .getElementById("btnPublicarEvento")
        ?.addEventListener("click", publicarEventoAdmin);

    document
        .getElementById("btnEliminarBorrador")
        ?.addEventListener("click", eliminarBorradorAdmin);

    document
        .getElementById("btnCancelarEvento")
        ?.addEventListener("click", mostrarConfirmacionCancelarEvento);

    document
        .getElementById("btnAgregarTipoBoleto")
        ?.addEventListener("click", mostrarFormularioAgregarTipo);

    document
        .getElementById("btnCancelarAgregarTipo")
        ?.addEventListener("click", ocultarFormularioAgregarTipo);

    document
        .getElementById("agregarTipoBoletoForm")
        ?.addEventListener("submit", manejarAgregarTipoBoleto);
}

function mostrarConfirmacionCancelarEvento() {
    const acciones =
        detalleAdminEvento?.querySelector(".admin-detail-actions");

    if (!acciones) return;

    if (document.getElementById("confirmacionCancelarEvento")) {
        return;
    }

    const confirmacion = document.createElement("div");

    confirmacion.id = "confirmacionCancelarEvento";
    confirmacion.className = "admin-message admin-message-error";

    confirmacion.innerHTML = `
        <p>
            <strong>¿Cancelar este evento?</strong>
        </p>

        <p>
            Esta acción invalidará los boletos activos
            y cancelará las reservas pendientes.
        </p>

        <div class="admin-form-actions">
            <button
                type="button"
                class="btn btn-outline"
                id="btnNoCancelarEvento"
            >
                No, volver
            </button>

            <button
                type="button"
                class="btn btn-primary"
                id="btnConfirmarCancelarEvento"
            >
                Sí, cancelar evento
            </button>
        </div>
    `;

    acciones.insertAdjacentElement(
        "afterend",
        confirmacion
    );

    document
        .getElementById("btnNoCancelarEvento")
        ?.addEventListener(
            "click",
            ocultarConfirmacionCancelarEvento
        );

    document
        .getElementById("btnConfirmarCancelarEvento")
        ?.addEventListener(
            "click",
            cancelarEventoAdmin
        );
}

function ocultarConfirmacionCancelarEvento() {
    document
        .getElementById("confirmacionCancelarEvento")
        ?.remove();
}

async function cancelarEventoAdmin() {
    if (!eventoAdminActualId) return;

    const boton = document.getElementById("btnCancelarEvento");

    if (!boton) return;

    boton.disabled = true;
    boton.textContent = "Cancelando...";

    try {

        await apiFetch(
            `/admin/eventos/${eventoAdminActualId}/cancelar`,
            {
                method: "POST",
                auth: true
            }
        );

        await cargarEventosAdmin();

        await cargarDetalleEventoAdmin(eventoAdminActualId);

    } catch (error) {

        mostrarErrorAccionEvento(
            error,
            "No fue posible cancelar el evento."
        );

        boton.disabled = false;
        boton.textContent = "Cancelar evento";
    }
}

async function publicarEventoAdmin() {
    if (!eventoAdminActualId) return;

    const boton = document.getElementById("btnPublicarEvento");

    if (!boton) return;

    if (!confirmarAccionAdmin(`publicar-${eventoAdminActualId}`, boton, "⚠ Confirmar publicación")) {
        return;
    }

    boton.disabled = true;
    boton.textContent = "Publicando...";

    try {

        await apiFetch(
            `/admin/eventos/${eventoAdminActualId}/publicar`,
            {
                method: "POST",
                auth: true
            }
        );

        await cargarEventosAdmin();

        await cargarDetalleEventoAdmin(eventoAdminActualId);

    } catch (error) {

        mostrarErrorAccionEvento(
            error,
            "No fue posible publicar el evento."
        );

        boton.disabled = false;
        boton.textContent = "Publicar evento";
    }
}

async function eliminarBorradorAdmin() {
    if (!eventoAdminActualId) return;

    const boton = document.getElementById("btnEliminarBorrador");

    if (!boton) return;

    // El borrador se elimina de forma definitiva: se exige confirmación.
    if (!confirmarAccionAdmin(`eliminar-${eventoAdminActualId}`, boton, "⚠ Confirmar eliminación")) {
        return;
    }

    boton.disabled = true;
    boton.textContent = "Eliminando...";

    try {

        await apiFetch(
            `/admin/eventos/${eventoAdminActualId}`,
            {
                method: "DELETE",
                auth: true
            }
        );

        cerrarDetalleEventoAdmin();

        await cargarEventosAdmin();

    } catch (error) {

        mostrarErrorAccionEvento(
            error,
            "No fue posible eliminar el borrador."
        );

        boton.disabled = false;
        boton.textContent = "Eliminar borrador";
    }
}

function mostrarErrorAccionEvento(error, mensajeFallback) {
    if (!detalleAdminEvento) return;

    const titulo = obtenerTituloError(mensajeFallback);

    const errores = obtenerListaErrores(error?.data);

    const contenedor = document.createElement("div");

    contenedor.className =
        "admin-message admin-message-error";

    const tituloElemento = document.createElement("h4");

    tituloElemento.textContent = titulo;

    contenedor.appendChild(tituloElemento);

    agregarDetalleErrores(
        contenedor,
        errores,
        "admin-error-list"
    );

    const acciones =
        detalleAdminEvento.querySelector(".admin-detail-actions");

    if (acciones) {
        acciones.insertAdjacentElement(
            "afterend",
            contenedor
        );
    } else {
        detalleAdminEvento.prepend(contenedor);
    }
}

function mostrarFormularioAgregarTipo() {
    const formulario = document.getElementById("formAgregarTipoBoleto");

    if (!formulario) return;

    formulario.classList.remove("hidden");

    formulario.scrollIntoView({
        behavior: "smooth",
        block: "start"
    });
}

function ocultarFormularioAgregarTipo() {
    const formulario = document.getElementById("formAgregarTipoBoleto");

    if (!formulario) return;

    formulario.classList.add("hidden");

    limpiarFormularioAgregarTipo();
}

function limpiarFormularioAgregarTipo() {
    const form = document.getElementById("agregarTipoBoletoForm");
    const mensaje = document.getElementById("mensajeAgregarTipoBoleto");

    form?.reset();

    if (mensaje) {
        mensaje.innerHTML = "";
        mensaje.classList.add("hidden");
    }
}

async function manejarAgregarTipoBoleto(event) {
    event.preventDefault();

    // Sin validación nativa del navegador (el form ya tiene `novalidate`):
    // el backend es la única fuente de verdad y sus errores se muestran abajo.
    if (!eventoAdminActualId) {
        return;
    }

    const datos = {
        nombre: document
            .getElementById("tipoBoletoNombre")
            .value
            .trim(),

        precio: Number(
            document.getElementById("tipoBoletoPrecio").value
        ),

        cantidadTotal: Number(
            document.getElementById("tipoBoletoCantidad").value
        )
    };

    setBotonAgregarTipoLoading(true);

    limpiarMensajeAgregarTipo();

    try {

        await apiFetch(
            `/admin/eventos/${eventoAdminActualId}/agregar-tipo-boleto`,
            {
                method: "POST",
                auth: true,
                body: datos
            }
        );

        await cargarDetalleEventoAdmin(eventoAdminActualId);

    } catch (error) {

        mostrarErrorAgregarTipo(error);

    } finally {

        setBotonAgregarTipoLoading(false);
    }
}

function limpiarMensajeAgregarTipo() {
    const mensaje = document.getElementById("mensajeAgregarTipoBoleto");

    if (!mensaje) return;

    mensaje.innerHTML = "";
    mensaje.classList.add("hidden");
}

function setBotonAgregarTipoLoading(loading) {
    const boton = document.getElementById("btnGuardarTipoBoleto");

    if (!boton) return;

    boton.disabled = loading;

    boton.textContent = loading
        ? "Agregando..."
        : "Agregar tipo";
}

function mostrarErrorAgregarTipo(error) {
    const mensaje = document.getElementById("mensajeAgregarTipoBoleto");

    if (!mensaje) return;

    const titulo = obtenerTituloError("No fue posible agregar el tipo de boleto.");

    const errores = obtenerListaErrores(error?.data);

    mensaje.innerHTML = "";

    const tituloElemento = document.createElement("strong");
    tituloElemento.textContent = titulo;

    mensaje.appendChild(tituloElemento);

    agregarDetalleErrores(
        mensaje,
        errores,
        "admin-error-list"
    );

    mensaje.classList.remove(
        "hidden",
        "admin-form-message-success"
    );

    mensaje.classList.add(
        "admin-form-message-error"
    );
}

function crearTiposBoletoHtml(tiposBoleto) {
    return `
        <div class="admin-ticket-grid">
            ${tiposBoleto
                .map((tipo) => `
                    <article class="admin-ticket-card">

                        <div class="admin-ticket-card-header">
                            <h5>
                                ${escaparHtml(tipo.nombre)}
                            </h5>

                            <span>
                                $${formatearPrecio(tipo.precio)}
                            </span>
                        </div>

                        <div class="admin-ticket-data">
                            <p>
                                <strong>Total:</strong>
                                ${tipo.cantidadTotal}
                            </p>

                            <p>
                                <strong>Disponibles:</strong>
                                ${tipo.disponibles}
                            </p>
                        </div>

                    </article>
                `)
                .join("")}
        </div>
    `;
}

function formatearPrecio(precio) {
    const numero = Number(precio);

    if (Number.isNaN(numero)) {
        return "0.00";
    }

    return numero.toLocaleString("es-MX", {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });
}

function cerrarDetalleEventoAdmin() {
    detalleAdminEvento?.classList.add("hidden");
    detalleAdminEvento.innerHTML = "";
}

function mostrarErrorDetalleEvento(error) {
    if (!detalleAdminEvento) return;

    const titulo = obtenerTituloError("No fue posible cargar el evento.");

    const errores = obtenerListaErrores(error?.data);

    detalleAdminEvento.innerHTML = `
        <div class="admin-message admin-message-error">
            <h4>${escaparHtml(titulo)}</h4>
            <div id="erroresDetalleAdmin"></div>
        </div>
    `;

    const contenedorErrores =
        document.getElementById("erroresDetalleAdmin");

    agregarDetalleErrores(
        contenedorErrores,
        errores,
        "admin-error-list"
    );
}

function crearTarjetaEvento(evento) {
    const fechaInicio = formatearFecha(evento.fechaInicio);

    return `
        <article class="admin-event-card">
            <span class="admin-event-status">
                ${escaparHtml(evento.estado)}
            </span>

            <h4>${escaparHtml(evento.nombre)}</h4>

            <p>
                <strong>Ubicación:</strong>
                ${escaparHtml(evento.ubicacion)}
            </p>

            <p>
                <strong>Inicio:</strong>
                ${escaparHtml(fechaInicio)}
            </p>

            <div class="admin-event-actions">
                <button
                    type="button"
                    class="btn btn-outline"
                    data-admin-evento-id="${evento.id}"
                >
                    Gestionar
                </button>
            </div>
        </article>
    `;
}

function formatearFecha(fecha) {
    if (!fecha) return "Sin fecha";

    const fechaObjeto = new Date(fecha);

    if (Number.isNaN(fechaObjeto.getTime())) {
        return fecha;
    }

    return fechaObjeto.toLocaleString("es-MX", {
        dateStyle: "medium",
        timeStyle: "short"
    });
}

function mostrarErrorEventos(error) {
    if (!listaAdminEventos) return;

    const titulo = obtenerTituloError("No fue posible cargar los eventos.");

    const errores = obtenerListaErrores(error?.data);

    listaAdminEventos.innerHTML = `
        <div class="admin-message admin-message-error">
            <h4>${escaparHtml(titulo)}</h4>
            <div id="erroresCargaAdmin"></div>
        </div>
    `;

    const contenedorErrores =
        document.getElementById("erroresCargaAdmin");

    agregarDetalleErrores(
        contenedorErrores,
        errores,
        "admin-error-list"
    );
}