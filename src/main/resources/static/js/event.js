import { apiFetch, getAccessToken, ApiError } from "./api.js";
import { AUTH_REQUIRED_EVENT } from "./auth.js";
import {
    formatearFecha,
    formatearFechaCompleta,
    formatearDia,
    formatearMes,
    formatearPrecio
} from "./utils.js";

let eventosInitDone = false;

export function initEventos() {
    if (eventosInitDone) return;
    eventosInitDone = true;
    document.getElementById("cerrarDetalle")?.addEventListener("click", volverAEventos);
}

/* =========================
   CARGAR EVENTOS
========================= */
export async function cargarEventos() {
    const contenedor = document.getElementById("contenedorEventos");
    if (!contenedor) return;

    mostrarCargandoEventos();

    try {
        const eventos = await apiFetch("/eventos/obtener");

        if (!Array.isArray(eventos) || eventos.length === 0) {
            mostrarEventosVacios();
            return;
        }

        contenedor.innerHTML = "";
        eventos.forEach((evento) => contenedor.appendChild(crearTarjetaEvento(evento)));
    } catch (error) {
        console.error("Error al cargar eventos:", error);
        mostrarErrorEventos(
            error instanceof ApiError && error.status === 0
                ? "No se pudo conectar con el servidor."
                : "No fue posible cargar los eventos."
        );
    }
}

/* =========================
   TARJETA EVENTO
========================= */
function crearTarjetaEvento(evento) {
    const tarjeta = document.createElement("article");
    tarjeta.classList.add("event-card");

    const fecha = document.createElement("div");
    fecha.className = "event-date";
    const dia = document.createElement("span");
    dia.textContent = formatearDia(evento?.fechaInicio);
    const mes = document.createElement("strong");
    mes.textContent = formatearMes(evento?.fechaInicio);
    fecha.append(dia, mes);

    const contenido = document.createElement("div");
    contenido.className = "event-card-content";

    const estado = document.createElement("span");
    estado.className = "event-status";
    estado.textContent = evento?.estado ?? "PUBLICADO";

    const titulo = document.createElement("h3");
    titulo.textContent = evento?.nombre ?? "Evento sin nombre";

    const ubicacion = document.createElement("p");
    ubicacion.textContent = evento?.ubicacion ?? "Ubicación por definir";

    const footer = document.createElement("div");
    footer.className = "event-card-footer";

    const fechaTexto = document.createElement("span");
    fechaTexto.textContent = formatearFecha(evento?.fechaInicio);

    const btnDetalle = document.createElement("button");
    btnDetalle.className = "btn btn-secondary btn-detail";
    btnDetalle.type = "button";
    btnDetalle.textContent = "Ver detalle";
    btnDetalle.addEventListener("click", () => {
        if (evento?.id != null) cargarDetalleEvento(evento.id);
    });

    footer.append(fechaTexto, btnDetalle);
    contenido.append(estado, titulo, ubicacion, footer);
    tarjeta.append(fecha, contenido);

    return tarjeta;
}

/* =========================
   DETALLE
========================= */
export async function cargarDetalleEvento(eventoId) {
    if (eventoId == null) return;

    const detalle = document.getElementById("detalleEvento");
    const seccionEventos = document.getElementById("eventos");
    if (!detalle) return;

    limpiarDetallePrevio();
    detalle.classList.remove("hidden");
    seccionEventos?.classList.add("hidden");
    detalle.scrollIntoView({ behavior: "smooth", block: "start" });

    try {
        const data = await apiFetch(`/eventos/${eventoId}/obtener`);
        mostrarDetalleEvento(data);
    } catch (error) {
        console.error("Error al cargar detalle:", error);
        const mensaje =
            error instanceof ApiError && error.status === 0
                ? "No se pudo conectar con el servidor."
                : error?.data?.titulo || "No fue posible cargar el evento.";
        mostrarErrorDetalle(mensaje);
    }
}

export function volverAEventos() {
    document.getElementById("detalleEvento")?.classList.add("hidden");
    const seccion = document.getElementById("eventos");
    seccion?.classList.remove("hidden");
    seccion?.scrollIntoView({ behavior: "smooth" });
}

function limpiarDetallePrevio() {
    const set = (id, texto) => {
        const el = document.getElementById(id);
        if (el) el.textContent = texto;
    };
    set("detalleNombre", "Cargando evento...");
    set("detalleDescripcion", "");
    set("detalleUbicacion", "—");
    set("detalleInicio", "—");
    set("detalleFin", "—");
    set("detalleMaxBoletos", "—");

    const contenedor = document.getElementById("tiposBoleto");
    if (contenedor) {
        contenedor.innerHTML = `
            <div class="events-state">
                <span class="loader"></span>
                <p>Cargando boletos...</p>
            </div>
        `;
    }
}

/* =========================
   MOSTRAR DETALLE
========================= */
function mostrarDetalleEvento(evento) {
    if (!evento) {
        mostrarErrorDetalle("No fue posible cargar el evento.");
        return;
    }

    const set = (id, texto) => {
        const el = document.getElementById(id);
        if (el) el.textContent = texto;
    };

    set("detalleNombre", evento.nombre ?? "Evento sin nombre");
    set("detalleDescripcion", evento.descripcion || "Sin descripción.");
    set("detalleUbicacion", evento.ubicacion ?? "Ubicación por definir");
    set("detalleInicio", formatearFechaCompleta(evento.fechaInicio));
    set("detalleFin", formatearFechaCompleta(evento.fechaFin));
    set("detalleMaxBoletos", evento.maxBoletosPorUsuario ?? "—");

    mostrarTiposBoleto(evento.tiposBoleto);
}

/* =========================
   TIPOS DE BOLETO
========================= */
function mostrarTiposBoleto(tiposBoleto) {
    const contenedor = document.getElementById("tiposBoleto");
    if (!contenedor) return;
    contenedor.innerHTML = "";

    if (!Array.isArray(tiposBoleto) || tiposBoleto.length === 0) {
        contenedor.innerHTML = `
            <div class="events-state">
                <h3>Aún no hay boletos publicados</h3>
                <p>Este evento todavía no tiene tipos de boleto disponibles.</p>
            </div>
        `;
        return;
    }

    tiposBoleto.forEach((tipo) => {
        const disponibles = Number(tipo?.disponibles ?? 0);
        const agotado = disponibles <= 0;

        const tarjeta = document.createElement("article");
        tarjeta.classList.add("ticket-type-card");

        const header = document.createElement("div");
        header.className = "ticket-type-header";
        const label = document.createElement("span");
        label.className = "ticket-type-label";
        label.textContent = "BOLETO";
        const nombre = document.createElement("h4");
        nombre.textContent = tipo?.nombre ?? "Boleto";
        header.append(label, nombre);

        const precio = document.createElement("div");
        precio.className = "ticket-type-price";
        precio.textContent = formatearPrecio(tipo?.precio);

        const disponibilidad = document.createElement("div");
        disponibilidad.className = "ticket-type-availability";
        disponibilidad.textContent = agotado ? "Agotado" : `${disponibles} disponibles`;

        const btnComprar = document.createElement("button");
        btnComprar.type = "button";
        btnComprar.className = `btn ${agotado ? "" : "btn-primary"} btn-full btn-comprar`;
        btnComprar.textContent = agotado ? "Agotado" : "Comprar";
        btnComprar.disabled = agotado;
        if (!agotado) {
            btnComprar.addEventListener("click", () => intentarComprar(tipo));
        }

        tarjeta.append(header, precio, disponibilidad, btnComprar);
        contenedor.appendChild(tarjeta);
    });
}

/* =========================
   INTENTAR COMPRAR
========================= */
function intentarComprar(tipoBoleto) {
    if (!getAccessToken()) {
        window.dispatchEvent(new CustomEvent(AUTH_REQUIRED_EVENT));
        return;
    }

    // TODO: conectar con el flujo real de compra (/compras/**) cuando se defina la UI.
    console.info("Usuario autenticado. Tipo de boleto:", tipoBoleto);
}

/* =========================
   ESTADOS
========================= */
function mostrarCargandoEventos() {
    const contenedor = document.getElementById("contenedorEventos");
    if (contenedor) {
        contenedor.innerHTML = `
            <div class="events-state">
                <span class="loader"></span>
                <p>Cargando eventos...</p>
            </div>
        `;
    }
}

function mostrarEventosVacios() {
    const contenedor = document.getElementById("contenedorEventos");
    if (contenedor) {
        contenedor.innerHTML = `
            <div class="events-state">
                <h3>No hay eventos disponibles</h3>
                <p>Actualmente no hay eventos publicados.</p>
            </div>
        `;
    }
}

function mostrarErrorEventos(mensaje) {
    const contenedor = document.getElementById("contenedorEventos");
    if (!contenedor) return;
    contenedor.innerHTML = "";
    const estado = document.createElement("div");
    estado.className = "events-state error-state";
    const titulo = document.createElement("h3");
    titulo.textContent = "Ocurrió un problema";
    const texto = document.createElement("p");
    texto.textContent = mensaje;
    estado.append(titulo, texto);
    contenedor.appendChild(estado);
}

function mostrarErrorDetalle(mensaje) {
    const set = (id, texto) => {
        const el = document.getElementById(id);
        if (el) el.textContent = texto;
    };
    set("detalleNombre", "No fue posible cargar el evento");
    set("detalleDescripcion", mensaje);
    set("detalleUbicacion", "—");
    set("detalleInicio", "—");
    set("detalleFin", "—");
    set("detalleMaxBoletos", "—");

    const contenedor = document.getElementById("tiposBoleto");
    if (contenedor) {
        // escaparHtml se usa solo si se interpola en HTML; aquí usamos textContent.
        contenedor.innerHTML = "";
        const estado = document.createElement("div");
        estado.className = "events-state error-state";
        const titulo = document.createElement("h3");
        titulo.textContent = "No fue posible cargar el evento";
        const texto = document.createElement("p");
        texto.textContent = mensaje;
        estado.append(titulo, texto);
        contenedor.appendChild(estado);
    }
}
