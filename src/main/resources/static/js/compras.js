import {
    apiFetch,
    ApiError,
    getAccessToken
} from "./api.js";

import {
    AUTH_CHANGED_EVENT
} from "./auth.js";

import {
    formatearFechaCompleta,
    formatearPrecio,
    setButtonLoading,
    obtenerListaErrores,
    agregarDetalleErrores
} from "./utils.js";


const modalCompras = document.getElementById("modalCompras");
const btnMisCompras = document.getElementById("btnMisCompras");
const cerrarMisCompras = document.getElementById("cerrarMisCompras");
const listaMisCompras = document.getElementById("listaMisCompras");

const modalBoletos = document.getElementById("modalBoletos");
const cerrarBoletos = document.getElementById("cerrarBoletos");
const listaBoletos = document.getElementById("listaBoletos");
const subtituloBoletos = document.getElementById("subtituloBoletos");

let comprasInitDone = false;
let pagoEnProceso = false;
let confirmTimer = null;


/* =========================
   INIT
========================= */

export function initCompras() {
    if (comprasInitDone) return;
    comprasInitDone = true;

    btnMisCompras?.addEventListener("click", abrirModalCompras);
    cerrarMisCompras?.addEventListener("click", cerrarModalCompras);
    cerrarBoletos?.addEventListener("click", cerrarModalBoletos);

    modalCompras?.addEventListener("click", (event) => {
        if (event.target === modalCompras) {
            cerrarModalCompras();
        }
    });

    modalBoletos?.addEventListener("click", (event) => {
        if (event.target === modalBoletos) {
            cerrarModalBoletos();
        }
    });

    document.addEventListener("keydown", (event) => {
        if (event.key !== "Escape") return;
        if (modalBoletos && !modalBoletos.classList.contains("hidden")) {
            cerrarModalBoletos();
        } else if (
            modalCompras &&
            !modalCompras.classList.contains("hidden")
        ) {
            cerrarModalCompras();
        }
    });

    window.addEventListener(AUTH_CHANGED_EVENT, manejarCambioAutenticacion);
}


/* =========================
   CAMBIO DE AUTENTICACIÓN
========================= */

function manejarCambioAutenticacion(event) {
    const autenticado = event.detail?.autenticado === true;

    btnMisCompras?.classList.toggle("hidden", !autenticado);

    if (!autenticado) {
        cerrarModalCompras();
    }
}


/* =========================
   ABRIR / CERRAR
========================= */

async function abrirModalCompras() {
    if (!getAccessToken()) return;

    modalCompras?.classList.remove("hidden");
    document.body.style.overflow = "hidden";

    await cargarMisCompras();
}

function cerrarModalCompras() {
    if (!modalCompras) return;

    modalCompras.classList.add("hidden");
    // No liberar el scroll si el modal de boletos sigue abierto encima.
    if (!modalBoletos || modalBoletos.classList.contains("hidden")) {
        document.body.style.overflow = "";
    }
}


/* =========================
   CARGAR COMPRAS
========================= */

async function cargarMisCompras() {
    if (!listaMisCompras) return;

    mostrarCargandoCompras();

    try {
        const compras = await apiFetch("/compras/mis-compras", {
            auth: true
        });

        if (!Array.isArray(compras) || compras.length === 0) {
            mostrarComprasVacias();
            return;
        }

        listaMisCompras.innerHTML = "";

        compras.forEach((compra) => {
            listaMisCompras.appendChild(crearTarjetaCompra(compra));
        });

    } catch (error) {
        console.error("Error al cargar mis compras:", error);
        mostrarErrorCompras(error);
    }
}


/* =========================
   TARJETA DE COMPRA
========================= */

function crearTarjetaCompra(compra) {

    const tarjeta = document.createElement("article");
    tarjeta.className = "purchase-history-card";

    const contenido = document.createElement("div");
    contenido.className = "purchase-history-main";

    const encabezado = document.createElement("div");
    encabezado.className = "purchase-history-heading";

    const evento = document.createElement("div");

    const etiqueta = document.createElement("span");
    etiqueta.className = "eyebrow";
    etiqueta.textContent = "EVENTO";

    const nombreEvento = document.createElement("h3");
    nombreEvento.textContent = compra?.nombreEvento ?? "Evento sin nombre";

    evento.append(etiqueta, nombreEvento);

    const estado = crearEstadoCompra(compra?.estado);

    encabezado.append(evento, estado);

    const datos = document.createElement("div");
    datos.className = "purchase-history-data";

    datos.append(
        crearDatoCompra(
            "Boletos",
            `${compra?.cantidadBoletos ?? 0}`
        )
    );

    datos.append(
        crearDatoCompra(
            "Total",
            formatearPrecio(compra?.total)
        )
    );

    datos.append(
        crearDatoCompra(
            "Reserva",
            formatearFechaCompleta(compra?.fechaReserva)
        )
    );

    if (compra?.estado === "RESERVADA") {
        datos.append(
            crearDatoCompra(
                "Expira",
                formatearFechaCompleta(compra?.fechaExpiracion)
            )
        );
    }

    contenido.append(encabezado, datos);

    const acciones = crearAccionesCompra(compra);

    tarjeta.append(contenido, acciones);

    return tarjeta;
}


/* =========================
   ESTADO
========================= */

function crearEstadoCompra(estado) {

    const elemento = document.createElement("span");
    elemento.className = "purchase-status";

    elemento.textContent = estado ?? "DESCONOCIDO";

    if (estado) {
        elemento.classList.add(
            `purchase-status-${estado.toLowerCase()}`
        );
    }

    return elemento;
}


/* =========================
   ACCIONES
========================= */

function crearAccionesCompra(compra) {

    const acciones = document.createElement("div");
    acciones.className = "purchase-history-actions";

    if (compra?.estado === "RESERVADA") {

        const botonPagar = document.createElement("button");

        botonPagar.type = "button";
        botonPagar.className = "btn btn-primary";
        botonPagar.textContent = "Pagar ahora";

        botonPagar.addEventListener("click", () => {
            pagarCompra(compra.compraId, formatearPrecio(compra.total), botonPagar);
        });

        acciones.appendChild(botonPagar);
    }

    if (compra?.estado === "RESERVADA" || compra?.estado === "PAGADA") {

        const botonBoletos = document.createElement("button");

        botonBoletos.type = "button";
        botonBoletos.className = "btn btn-secondary";
        botonBoletos.textContent = "Ver boletos";

        botonBoletos.addEventListener("click", () => {
            abrirModalBoletos(compra.compraId);
        });

        acciones.appendChild(botonBoletos);
    }

    return acciones;
}


/* =========================
   PAGAR
========================= */

async function pagarCompra(compraId, totalTexto, boton) {

    if (pagoEnProceso || !compraId || !boton) return;

    // Confirmación en dos pasos: el primer clic pide confirmar con el
    // monto visible, el segundo ejecuta el pago. Evita cobros accidentales.
    if (boton.dataset.confirm !== "pending") {
        boton.dataset.confirm = "pending";
        if (boton.dataset.originalText === undefined) {
            boton.dataset.originalText = boton.textContent;
        }
        boton.textContent = "⚠ Confirmar pago";
        boton.classList.add("btn-confirm");

        clearTimeout(confirmTimer);
        confirmTimer = setTimeout(() => {
            delete boton.dataset.confirm;
            boton.classList.remove("btn-confirm");
            boton.textContent = boton.dataset.originalText ?? "Pagar ahora";
        }, 6000);

        return;
    }

    clearTimeout(confirmTimer);
    delete boton.dataset.confirm;
    boton.classList.remove("btn-confirm");

    pagoEnProceso = true;

    setButtonLoading(
        boton,
        true,
        "Procesando..."
    );

    try {

        await apiFetch(`/compras/${compraId}/pagar`, {
            method: "POST",
            auth: true
        });

        await cargarMisCompras();

    } catch (error) {

        console.error("Error al pagar compra:", error);

        mostrarErrorPago(error);

        setButtonLoading(
            boton,
            false,
            "Pagar ahora"
        );

    } finally {
        pagoEnProceso = false;
    }
}


/* =========================
   DATOS
========================= */

function crearDatoCompra(etiqueta, valor) {

    const elemento = document.createElement("div");
    elemento.className = "purchase-history-item";

    const label = document.createElement("span");
    label.textContent = etiqueta;

    const value = document.createElement("strong");
    value.textContent = valor ?? "—";

    elemento.append(label, value);

    return elemento;
}


/* =========================
   ESTADOS DE LA LISTA
========================= */

function mostrarCargandoCompras() {

    listaMisCompras.innerHTML = `
        <div class="purchases-state">
            <span class="loader"></span>
            <p>Cargando tus compras...</p>
        </div>
    `;
}


function mostrarComprasVacias() {

    listaMisCompras.innerHTML = `
        <div class="purchases-state">
            <h3>Aún no tienes compras</h3>
            <p>
                Cuando reserves boletos, tus compras aparecerán aquí.
            </p>
        </div>
    `;
}


function mostrarErrorCompras(error) {

    const titulo =
        error instanceof ApiError && error.status === 0
            ? "No se pudo conectar con el servidor."
            : "No fue posible cargar tus compras.";

    const errores =
        error instanceof ApiError && error.status === 0
            ? []
            : obtenerListaErrores(error?.data);

    listaMisCompras.innerHTML = "";

    const estado = document.createElement("div");
    estado.className = "purchases-state error-state";

    const encabezado = document.createElement("h3");
    encabezado.textContent = titulo;

    estado.appendChild(encabezado);

    if (errores.length === 1) {
        const texto = document.createElement("p");
        texto.textContent = errores[0];
        estado.appendChild(texto);
    } else if (errores.length > 1) {
        const lista = document.createElement("ul");
        lista.className = "purchases-error-list";
        errores.forEach((item) => {
            const li = document.createElement("li");
            li.textContent = item;
            lista.appendChild(li);
        });
        estado.appendChild(lista);
    } else {
        const texto = document.createElement("p");
        texto.textContent = "Intenta de nuevo más tarde.";
        estado.appendChild(texto);
    }

    listaMisCompras.appendChild(estado);
}


function mostrarErrorPago(error) {

    const titulo =
        error instanceof ApiError && error.status === 0
            ? "No se pudo conectar con el servidor."
            : "No fue posible procesar el pago.";

    const errores =
        error instanceof ApiError && error.status === 0
            ? []
            : obtenerListaErrores(error?.data);

    const existente = listaMisCompras.querySelector(
        ".purchase-history-error"
    );

    existente?.remove();

    const mensajeElemento = document.createElement("div");

    mensajeElemento.className =
        "purchase-message error purchase-history-error";

    const tituloElemento = document.createElement("strong");
    tituloElemento.textContent = titulo;

    mensajeElemento.appendChild(tituloElemento);

    agregarDetalleErrores(mensajeElemento, errores);

    listaMisCompras.prepend(mensajeElemento);
}


/* =========================
   VER BOLETOS
========================= */

async function abrirModalBoletos(compraId) {
    if (!compraId || !modalBoletos || !listaBoletos) return;

    modalBoletos.classList.remove("hidden");
    document.body.style.overflow = "hidden";

    if (subtituloBoletos) subtituloBoletos.textContent = `Compra #${compraId}`;

    listaBoletos.innerHTML = `
        <div class="purchases-state">
            <span class="loader"></span>
            <p>Cargando boletos...</p>
        </div>
    `;

    try {
        const detalle = await apiFetch(`/compras/${compraId}`, { auth: true });
        mostrarBoletos(detalle);
    } catch (error) {
        console.error("Error al cargar boletos:", error);
        const tituloBoletos =
            error instanceof ApiError && error.status === 0
                ? "No se pudo conectar con el servidor."
                : "No fue posible cargar los boletos.";
        const erroresBoletos =
            error instanceof ApiError && error.status === 0
                ? []
                : obtenerListaErrores(error?.data);
        listaBoletos.innerHTML = "";
        const estado = document.createElement("div");
        estado.className = "purchases-state error-state";
        const titulo = document.createElement("h3");
        titulo.textContent = tituloBoletos;
        estado.appendChild(titulo);
        if (erroresBoletos.length === 1) {
            const texto = document.createElement("p");
            texto.textContent = erroresBoletos[0];
            estado.appendChild(texto);
        } else if (erroresBoletos.length > 1) {
            const lista = document.createElement("ul");
            lista.className = "purchases-error-list";
            erroresBoletos.forEach((item) => {
                const li = document.createElement("li");
                li.textContent = item;
                lista.appendChild(li);
            });
            estado.appendChild(lista);
        }
        listaBoletos.appendChild(estado);
    }
}

function cerrarModalBoletos() {
    if (!modalBoletos) return;

    modalBoletos.classList.add("hidden");
    // Mantener el bloqueo de scroll si Mis compras sigue abierto detrás.
    if (!modalCompras || modalCompras.classList.contains("hidden")) {
        document.body.style.overflow = "";
    }
}

function mostrarBoletos(detalle) {
    if (!listaBoletos) return;
    listaBoletos.innerHTML = "";

    const boletos = detalle?.boletos;

    if (subtituloBoletos) {
        const nombre = detalle?.nombreEvento ?? "Tu compra";
        subtituloBoletos.textContent = `${nombre} · Compra #${detalle?.compraId ?? ""}`;
    }

    if (!Array.isArray(boletos) || boletos.length === 0) {
        listaBoletos.innerHTML = `
            <div class="purchases-state">
                <h3>Sin boletos por mostrar</h3>
                <p>Esta compra aún no tiene boletos generados.</p>
            </div>
        `;
        return;
    }

    boletos.forEach((boleto) => {
        listaBoletos.appendChild(crearTarjetaBoleto(boleto));
    });

    const nota = document.createElement("p");
    nota.className = "boletos-note";
    nota.textContent = "La descarga en PDF estará disponible próximamente desde aquí.";
    listaBoletos.appendChild(nota);
}

function crearTarjetaBoleto(boleto) {
    const tarjeta = document.createElement("article");
    tarjeta.className = "boleto-card";

    const principal = document.createElement("div");
    principal.className = "boleto-main";

    const tipo = document.createElement("h3");
    tipo.textContent = boleto?.tipoBoletoNombre ?? "Boleto";

    const titular = document.createElement("p");
    titular.className = "boleto-titular";
    titular.textContent = boleto?.titularNombre ?? "Sin titular";

    const folio = document.createElement("span");
    folio.className = "boleto-folio";
    folio.textContent = `Folio: ${boleto?.folio ?? "—"}`;

    principal.append(tipo, titular, folio);

    const lateral = document.createElement("div");
    lateral.className = "boleto-side";

    const estado = document.createElement("span");
    estado.className = "purchase-status";
    estado.textContent = boleto?.estado ?? "—";

    const botonPdf = document.createElement("button");
    botonPdf.type = "button";
    botonPdf.className = "btn btn-secondary";
    botonPdf.textContent = "PDF";
    botonPdf.disabled = true;
    botonPdf.title = "La descarga en PDF estará disponible próximamente";

    lateral.append(estado, botonPdf);
    tarjeta.append(principal, lateral);

    return tarjeta;
}