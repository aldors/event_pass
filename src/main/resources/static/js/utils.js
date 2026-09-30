export function escaparHtml(valor) {
    const div = document.createElement("div");
    div.textContent = valor ?? "";
    return div.innerHTML;
}

function toDate(fecha) {
    const d = new Date(fecha);
    return Number.isNaN(d.getTime()) ? null : d;
}

export function formatearFecha(fecha) {
    const d = toDate(fecha);
    if (!d) return "Fecha por definir";
    return d.toLocaleDateString("es-MX", { day: "2-digit", month: "long", year: "numeric" });
}

export function formatearFechaCompleta(fecha) {
    const d = toDate(fecha);
    if (!d) return "Fecha por definir";
    return d.toLocaleString("es-MX", {
        day: "2-digit",
        month: "long",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit"
    });
}

export function formatearDia(fecha) {
    const d = toDate(fecha);
    if (!d) return "--";
    return d.toLocaleDateString("es-MX", { day: "2-digit" });
}

export function formatearMes(fecha) {
    const d = toDate(fecha);
    if (!d) return "---";
    return d.toLocaleDateString("es-MX", { month: "short" }).replace(".", "").toUpperCase();
}

export function formatearPrecio(precio) {
    const numero = Number(precio);
    if (!Number.isFinite(numero)) return "$0.00";
    return new Intl.NumberFormat("es-MX", { style: "currency", currency: "MXN" }).format(numero);
}

/*
 * Deshabilita un botón mientras hay una petición en curso
 * y restaura su texto original al terminar. Evita doble submit.
 */
export function setButtonLoading(button, loading, loadingText = "Cargando...") {
    if (!button) return;
    if (button.dataset.originalText === undefined) {
        button.dataset.originalText = button.textContent;
    }
    button.disabled = loading;
    button.classList.toggle("is-loading", loading);
    button.textContent = loading ? loadingText : button.dataset.originalText;
}
