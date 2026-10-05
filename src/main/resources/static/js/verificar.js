import { apiFetch, ApiError } from "./api.js";
import { escaparHtml, obtenerListaErrores } from "./utils.js";

const estadoVerificacion = document.getElementById("estadoVerificacion");

// Los módulos ES son diferidos por defecto: el DOM ya está listo.
verificarBoleto();

async function verificarBoleto() {

    const parametros = new URLSearchParams(window.location.search);
    const codigoQr = parametros.get("codigoQr");

    if (!codigoQr) {
        mostrarError(
            "No se encontró el código del boleto.",
            "El enlace de verificación no contiene un código válido."
        );
        return;
    }

    try {

        const boleto = await apiFetch(
            `/boletos/verificar/${encodeURIComponent(codigoQr)}`
        );

        mostrarBoleto(boleto);

    } catch (error) {

        if (error instanceof ApiError && error.status === 0) {
            mostrarError(
                "No se pudo verificar el boleto",
                "No se pudo conectar con el servidor."
            );
            return;
        }

        const detalle = obtenerListaErrores(error?.data)[0]
            ?? "El boleto no existe o el código no es válido.";

        mostrarError("No se pudo verificar el boleto", detalle);
    }
}

function mostrarBoleto(boleto) {

    document.title = boleto?.evento
        ? `${boleto.evento} — Verificación de boleto`
        : "EventPass — Verificación de boleto";

    const estadoTexto = boleto.valido
        ? "BOLETO VÁLIDO"
        : "BOLETO NO VÁLIDO";

    const estadoClase = boleto.valido
        ? "valid"
        : "invalid";

    estadoVerificacion.innerHTML = `
        <div class="verification-header">

            <span class="verification-eyebrow">
                Verificación de acceso
            </span>

            <h1 class="verification-title">
                ${escaparHtml(boleto.evento)}
            </h1>

            <p class="verification-status ${estadoClase}">
                ${estadoTexto}
            </p>

        </div>

        <div class="verification-data">

            <div class="verification-row">
                <span class="verification-label">
                    Tipo de boleto
                </span>

                <span class="verification-value">
                    ${escaparHtml(boleto.tipoBoleto)}
                </span>
            </div>

            <div class="verification-row">
                <span class="verification-label">
                    Titular
                </span>

                <span class="verification-value">
                    ${escaparHtml(boleto.titular)}
                </span>
            </div>

            <div class="verification-row">
                <span class="verification-label">
                    Folio
                </span>

                <span class="verification-value">
                    ${escaparHtml(boleto.folio)}
                </span>
            </div>

            <div class="verification-row">
                <span class="verification-label">
                    Estado
                </span>

                <span class="verification-value">
                    ${escaparHtml(boleto.estado)}
                </span>
            </div>

        </div>
    `;
}

function mostrarError(titulo, mensaje) {

    estadoVerificacion.innerHTML = `
        <div class="verification-error">

            <h1>
                ${escaparHtml(titulo)}
            </h1>

            <p>
                ${escaparHtml(mensaje)}
            </p>

        </div>
    `;
}
