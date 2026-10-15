import { apiFetch } from "./api.js";

const INTENTOS_MAXIMOS = 6;
const ESPERA_MS = 2000;

export async function procesarRetornoPago() {
    const url = new URL(window.location.href);
    const resultado = url.searchParams.get("payment");

    if (!["success", "cancelled"].includes(resultado)) {
        return;
    }

    const compraId = url.searchParams.get("compra_id");

    // Quitamos los parámetros para que el mensaje no se repita al recargar la página.
    url.searchParams.delete("payment");
    url.searchParams.delete("compra_id");
    url.searchParams.delete("session_id");

    window.history.replaceState(
        {},
        document.title,
        url.pathname + url.search + url.hash
    );

    if (!compraId || !/^\d+$/.test(compraId)) {
        mostrarMensajePago(
            "No pudimos identificar la compra",
            "Consulta tus compras para verificar su estado.",
            "warning"
        );
        return;
    }

    if (resultado === "cancelled") {
        mostrarMensajePago(
            "Pago cancelado",
            "No completaste el pago en Stripe. Tu reserva puede seguir activa hasta que expire.",
            "warning"
        );
        return;
    }

    mostrarMensajePago(
        "Verificando tu pago",
        "Esto puede tardar unos segundos...",
        "info"
    );

    // Reconciliación directa contra Stripe por si el webhook aún no llegó.
    // Si falla, se sigue con el polling del estado local.
    try {
        await apiFetch(`/compras/${compraId}/verificar-pago`, {
            method: "POST",
            auth: true
        });
    } catch (error) {
        console.warn("No se pudo verificar el pago con Stripe, se usará el estado local:", error);
    }

    for (let intento = 0; intento < INTENTOS_MAXIMOS; intento++) {
        try {
            const compra = await apiFetch(`/compras/${compraId}`, {
                method: "GET",
                auth: true
            });

            if (compra.estado === "PAGADA") {
                mostrarMensajePago(
                    "¡Pago confirmado!",
                    `Tu compra #${compra.compraId} está pagada. Ya puedes ver tus boletos en Mis compras.`,
                    "success",
                    {
                        texto: "Ver mis compras",
                        alClic: () => {
                            document.getElementById("btnMisCompras")?.click();
                        }
                    }
                );
                return;
            }

            if (compra.estado === "EXPIRADA") {
                mostrarMensajePago(
                    "La reserva expiró",
                    "Consulta tus compras para verificar su estado.",
                    "warning"
                );
                return;
            }

            if (compra.estado === "CANCELADA") {
                mostrarMensajePago(
                    "Reserva cancelada",
                    "Esta reserva está cancelada y no se completó la compra.",
                    "warning"
                );
                return;
            }

            // El webhook podría estar todavía en proceso.
            if (intento < INTENTOS_MAXIMOS - 1) {
                await esperar(ESPERA_MS);
            }
        } catch (error) {
            mostrarMensajePago(
                "Verificación pendiente",
                "No pudimos verificar el pago en este momento. Revisa tus compras en unos instantes.",
                "warning"
            );
            return;
        }
    }

    mostrarMensajePago(
        "Pago pendiente de confirmación",
        "Todavía está pendiente de confirmación. Revisa tus compras en unos instantes; no intentes pagar otra vez mientras verificas el estado.",
        "info"
    );
}

function esperar(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

const ICONOS_PAGO = {
    success: "✓",
    warning: "!",
    info: "i"
};

function mostrarMensajePago(titulo, mensaje, tipo, accion = null) {
    let contenedor = document.getElementById("paymentReturnMessage");

    if (!contenedor) {
        return;
    }

    contenedor.innerHTML = "";
    contenedor.className = `payment-return-message payment-return-${tipo}`;

    const icono = document.createElement("span");
    icono.className = "payment-return-icon";
    icono.setAttribute("aria-hidden", "true");
    icono.textContent = ICONOS_PAGO[tipo] ?? ICONOS_PAGO.info;

    const cuerpo = document.createElement("div");
    cuerpo.className = "payment-return-body";

    const tituloElemento = document.createElement("strong");
    tituloElemento.className = "payment-return-title";
    tituloElemento.textContent = titulo;

    const mensajeElemento = document.createElement("p");
    mensajeElemento.className = "payment-return-text";
    mensajeElemento.textContent = mensaje;

    cuerpo.append(tituloElemento, mensajeElemento);

    if (accion) {
        const boton = document.createElement("button");
        boton.className = "payment-return-action";
        boton.type = "button";
        boton.textContent = accion.texto;
        boton.addEventListener("click", () => {
            contenedor.classList.add("hidden");
            accion.alClic?.();
        });
        cuerpo.appendChild(boton);
    }

    const cerrar = document.createElement("button");
    cerrar.className = "payment-return-close";
    cerrar.type = "button";
    cerrar.setAttribute("aria-label", "Cerrar mensaje");
    cerrar.textContent = "×";
    cerrar.addEventListener("click", () => {
        contenedor.classList.add("hidden");
    });

    contenedor.append(icono, cuerpo, cerrar);
}
