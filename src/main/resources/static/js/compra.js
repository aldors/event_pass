import { apiFetch, ApiError } from "./api.js";
import { formatearPrecio, setButtonLoading, obtenerListaErrores, agregarDetalleErrores } from "./utils.js";

const modalCompra = document.getElementById("modalCompra");
const cerrarCompraBtn = document.getElementById("cerrarCompra");

const eventoCompraNombre = document.getElementById("eventoCompraNombre");
const carritoCompra = document.getElementById("carritoCompra");

const mensajeCompra = document.getElementById("mensajeCompra");

const btnAgregarOtroBoleto = document.getElementById("btnAgregarOtroBoleto");
const btnReservarBoletos = document.getElementById("btnReservarBoletos");

const cantidadTotalCompra = document.getElementById("cantidadTotalCompra");
const totalCompra = document.getElementById("totalCompra");

let compraInitDone = false;

let carrito = [];
let eventoActual = null;
let reservaEnProceso = false;
let pagoEnProceso = false;

/*
 * Cada elemento del carrito:
 *
 * {
 *   tipoBoletoId,
 *   nombre,
 *   precio,
 *   disponibles,
 *   cantidad,
 *   titulares: ["", ""]
 * }
 */

export function initCompra() {
    if (compraInitDone) return;
    compraInitDone = true;

    cerrarCompraBtn?.addEventListener("click", cerrarModalCompra);

    btnAgregarOtroBoleto?.addEventListener("click", agregarOtroBoleto);

    btnReservarBoletos?.addEventListener("click", reservarBoletos);

    modalCompra?.addEventListener("click", (event) => {
        if (event.target === modalCompra) {
            cerrarModalCompra();
        }
    });

    document.addEventListener("keydown", (event) => {
        if (
            event.key === "Escape" &&
            modalCompra &&
            !modalCompra.classList.contains("hidden")
        ) {
            cerrarModalCompra();
        }
    });
}

/**
 * Abre el modal de compra.
 */
export function abrirCompra(evento, tipoBoleto) {
    if (!evento || !tipoBoleto) return;

    eventoActual = evento;

    btnAgregarOtroBoleto?.classList.remove("hidden");
    btnReservarBoletos?.classList.remove("hidden");

    limpiarMensajeCompra();

    const existente = carrito.find(
        (item) => item.tipoBoletoId === tipoBoleto.id
    );

    if (existente) {
        aumentarCantidad(existente.tipoBoletoId);
    } else {
        carrito.push({
            tipoBoletoId: tipoBoleto.id,
            nombre: tipoBoleto.nombre,
            precio: Number(tipoBoleto.precio),
            disponibles: Number(tipoBoleto.disponibles),
            cantidad: 1,
            titulares: [""]
        });
    }

    eventoCompraNombre.textContent = evento.nombre ?? "Evento";

    renderizarCarrito();

    modalCompra?.classList.remove("hidden");
    document.body.style.overflow = "hidden";

    const primerInput = carritoCompra?.querySelector(".titular-input");

    primerInput?.focus();
}

/**
 * Cierra el modal conservando el carrito actual.
 */
function cerrarModalCompra() {
    if (!modalCompra) return;

    modalCompra.classList.add("hidden");
    document.body.style.overflow = "";
    limpiarMensajeCompra();
}

/**
 * Permite volver al detalle del evento.
 */
function agregarOtroBoleto() {
    cerrarModalCompra();

    document.getElementById("detalleEvento")?.scrollIntoView({
        behavior: "smooth",
        block: "start"
    });
}

/**
 * Renderiza el carrito completo.
 */
function renderizarCarrito() {
    if (!carritoCompra) return;

    carritoCompra.innerHTML = "";

    if (carrito.length === 0) {
        mostrarCarritoVacio();
        actualizarResumen();
        return;
    }

    carrito.forEach((item) => {
        carritoCompra.appendChild(crearBloqueTipoBoleto(item));
    });

    actualizarResumen();
}

/**
 * Crea la sección visual de un tipo de boleto.
 */
function crearBloqueTipoBoleto(item) {
    const bloque = document.createElement("section");
    bloque.className = "purchase-ticket-group";

    const encabezado = document.createElement("div");
    encabezado.className = "purchase-ticket-header";

    const informacion = document.createElement("div");

    const etiqueta = document.createElement("span");
    etiqueta.className = "ticket-type-label";
    etiqueta.textContent = "BOLETO";

    const nombre = document.createElement("h3");
    nombre.textContent = item.nombre;

    const precio = document.createElement("span");
    precio.className = "purchase-ticket-unit-price";
    precio.textContent = `${formatearPrecio(item.precio)} por boleto`;

    informacion.append(etiqueta, nombre, precio);

    const cantidad = document.createElement("div");
    cantidad.className = "quantity-control";

    const btnMenos = document.createElement("button");
    btnMenos.type = "button";
    btnMenos.className = "quantity-button";
    btnMenos.textContent = "−";
    btnMenos.setAttribute(
        "aria-label",
        `Reducir cantidad de ${item.nombre}`
    );
    btnMenos.disabled = item.cantidad <= 1;

    btnMenos.addEventListener("click", () => {
        cambiarCantidad(item.tipoBoletoId, -1);
    });

    const cantidadTexto = document.createElement("span");
    cantidadTexto.className = "quantity-value";
    cantidadTexto.textContent = item.cantidad;

    const btnMas = document.createElement("button");
    btnMas.type = "button";
    btnMas.className = "quantity-button";
    btnMas.textContent = "+";
    btnMas.setAttribute(
        "aria-label",
        `Aumentar cantidad de ${item.nombre}`
    );

    btnMas.disabled =
        item.cantidad >= item.disponibles ||
        obtenerCantidadTotal() >= obtenerMaximoPermitido();

    btnMas.addEventListener("click", () => {
        cambiarCantidad(item.tipoBoletoId, 1);
    });

    cantidad.append(btnMenos, cantidadTexto, btnMas);

    encabezado.append(informacion, cantidad);

    const titulares = document.createElement("div");
    titulares.className = "titulares-list";

    item.titulares.forEach((titular, index) => {
        const grupo = document.createElement("div");
        grupo.className = "titular-group";

        const label = document.createElement("label");
        label.textContent = `Titular del boleto ${index + 1}`;
        label.setAttribute(
            "for",
            `titular-${item.tipoBoletoId}-${index}`
        );

        const input = document.createElement("input");
        input.type = "text";
        input.id = `titular-${item.tipoBoletoId}-${index}`;
        input.className = "titular-input";
        input.maxLength = 200;
        input.placeholder = "Nombre completo";
        input.autocomplete = "name";
        input.value = titular;

        input.addEventListener("input", (event) => {
            item.titulares[index] = event.target.value;
        });

        grupo.append(label, input);
        titulares.appendChild(grupo);
    });

    const subtotal = document.createElement("div");
    subtotal.className = "purchase-subtotal";
    subtotal.textContent = `Subtotal: ${formatearPrecio(
        item.precio * item.cantidad
    )}`;

    bloque.append(encabezado, titulares, subtotal);

    return bloque;
}

/**
 * Cambia la cantidad de un tipo de boleto.
 */
function cambiarCantidad(tipoBoletoId, cambio) {
    const item = carrito.find(
        (elemento) => elemento.tipoBoletoId === tipoBoletoId
    );

    if (!item) return;

    const nuevaCantidad = item.cantidad + cambio;

    if (nuevaCantidad < 1) return;

    if (nuevaCantidad > item.disponibles) {
        mostrarMensajeCompra(
            `No hay más boletos disponibles de tipo "${item.nombre}".`,
            "error"
        );
        return;
    }

    if (
        cambio > 0 &&
        obtenerCantidadTotal() >= obtenerMaximoPermitido()
    ) {
        mostrarMensajeCompra(
            `El máximo permitido es de ${obtenerMaximoPermitido()} boleto(s) por usuario.`,
            "error"
        );
        return;
    }

    item.cantidad = nuevaCantidad;

    if (item.titulares.length < nuevaCantidad) {
        while (item.titulares.length < nuevaCantidad) {
            item.titulares.push("");
        }
    } else {
        item.titulares.length = nuevaCantidad;
    }

    limpiarMensajeCompra();
    renderizarCarrito();
}

/**
 * Aumenta directamente un tipo existente.
 */
function aumentarCantidad(tipoBoletoId) {
    cambiarCantidad(tipoBoletoId, 1);
}

/**
 * Devuelve la cantidad total de boletos.
 */
function obtenerCantidadTotal() {
    return carrito.reduce(
        (total, item) => total + item.cantidad,
        0
    );
}

/**
 * Devuelve el máximo permitido por evento.
 */
function obtenerMaximoPermitido() {
    return Number(eventoActual?.maxBoletosPorUsuario ?? 1);
}

/**
 * Calcula el total del carrito.
 */
function obtenerTotal() {
    return carrito.reduce(
        (total, item) => total + item.precio * item.cantidad,
        0
    );
}

/**
 * Actualiza cantidad y total.
 */
function actualizarResumen() {
    const cantidad = obtenerCantidadTotal();
    const total = obtenerTotal();

    if (cantidadTotalCompra) {
        cantidadTotalCompra.textContent = cantidad;
    }

    if (totalCompra) {
        totalCompra.textContent = formatearPrecio(total);
    }
}

/**
 * Envía la reserva al backend.
 */
async function reservarBoletos() {
    if (reservaEnProceso || pagoEnProceso) return;

    limpiarMensajeCompra();

    const validacion = validarCarrito();

    if (!validacion.valido) {
        mostrarMensajeCompra(validacion.mensaje, "error");
        return;
    }

    if (!eventoActual?.id) {
        mostrarMensajeCompra(
            "No fue posible identificar el evento.",
            "error"
        );
        return;
    }

    const boletos = [];

    carrito.forEach((item) => {
        item.titulares.forEach((titular) => {
            boletos.push({
                tipoBoletoId: item.tipoBoletoId,
                titular: titular.trim()
            });
        });
    });

    const request = {
        eventoId: eventoActual.id,
        boletos
    };

    reservaEnProceso = true;

    setButtonLoading(
        btnReservarBoletos,
        true,
        "Reservando..."
    );

    try {
        const reserva = await apiFetch("/compras/reservar-boletos", {
            method: "POST",
            auth: true,
            body: request
        });

        manejarReservaExitosa(reserva);

    } catch (error) {
        console.error("Error al reservar boletos:", error);

        if (error instanceof ApiError && error.status === 0) {
            mostrarMensajeCompra(
                "No se pudo conectar con el servidor.",
                "error"
            );
        } else {
            manejarApiErrorCompra(error?.data);
        }

    } finally {
        reservaEnProceso = false;

        setButtonLoading(
            btnReservarBoletos,
            false
        );
    }
}

/**
 * Validaciones de experiencia de usuario.
 *
 * El backend sigue siendo la autoridad.
 */
function validarCarrito() {
    if (!carrito.length) {
        return {
            valido: false,
            mensaje: "Debes seleccionar al menos un boleto."
        };
    }

    const cantidadTotal = obtenerCantidadTotal();

    if (cantidadTotal <= 0) {
        return {
            valido: false,
            mensaje: "Debes seleccionar al menos un boleto."
        };
    }

    if (cantidadTotal > obtenerMaximoPermitido()) {
        return {
            valido: false,
            mensaje:
                `El máximo permitido es de ${obtenerMaximoPermitido()} boleto(s) por usuario.`
        };
    }

    for (const item of carrito) {
        if (item.cantidad > item.disponibles) {
            return {
                valido: false,
                mensaje:
                    `El tipo de boleto "${item.nombre}" ya no tiene suficientes boletos disponibles.`
            };
        }

        for (let i = 0; i < item.titulares.length; i++) {
            if (!item.titulares[i].trim()) {
                return {
                    valido: false,
                    mensaje:
                        `Debes indicar el titular del boleto ${i + 1} de "${item.nombre}".`
                };
            }
        }
    }

    return {
        valido: true
    };
}

/**
 * Procesa la reserva exitosa y muestra la opción de pago.
 */
function manejarReservaExitosa(reserva) {
    const reservaId = reserva?.compraId;

    if (!reservaId) {
        mostrarMensajeCompra(
            "La reserva fue creada, pero no se recibió su identificador.",
            "error"
        );
        return;
    }

    const total = formatearPrecio(reserva?.total);

    let expiracion = "—";

    if (reserva?.expiraEn) {
        const fecha = new Date(reserva.expiraEn);

        if (!Number.isNaN(fecha.getTime())) {
            expiracion = fecha.toLocaleString("es-MX", {
                day: "2-digit",
                month: "long",
                year: "numeric",
                hour: "2-digit",
                minute: "2-digit"
            });
        }
    }

    // La reserva ya existe en backend.
    // El carrito deja de representar una compra pendiente.
    carrito = [];

    actualizarResumen();

    mostrarResultadoReserva({
        reservaId,
        total,
        expiracion
    });
}

/**
 * Muestra la información de la reserva
 * y permite realizar el pago.
 */
function mostrarResultadoReserva({
    reservaId,
    total,
    expiracion
}) {
    if (!carritoCompra) return;

    carritoCompra.innerHTML = "";

    const resultado = document.createElement("div");
    resultado.className = "purchase-result";

    const icono = document.createElement("div");
    icono.className = "purchase-result-icon";
    icono.textContent = "✓";

    const titulo = document.createElement("h3");
    titulo.textContent = "Reserva realizada";

    const texto = document.createElement("p");
    texto.textContent =
        "Tus boletos fueron reservados correctamente. La reserva permanecerá activa durante el tiempo indicado. Ahora puedes realizar el pago."

    const datos = document.createElement("div");
    datos.className = "purchase-result-data";

    datos.appendChild(
        crearDatoResultado(
            "Reserva",
            `#${reservaId}`
        )
    );

    datos.appendChild(
        crearDatoResultado(
            "Total",
            total
        )
    );

    datos.appendChild(
        crearDatoResultado(
            "Expira",
            expiracion
        )
    );

    const aviso = document.createElement("div");
    aviso.className = "purchase-result-warning";
    aviso.textContent =
        "La reserva debe pagarse antes de que expire.";

    const btnPagar = document.createElement("button");
    btnPagar.type = "button";
    btnPagar.className = "btn btn-primary btn-full";
    btnPagar.textContent = "Pagar ahora";

    btnPagar.addEventListener("click", () => {
        pagarReserva(reservaId, btnPagar, total);
    });

    resultado.append(
        icono,
        titulo,
        texto,
        datos,
        aviso,
        btnPagar
    );

    carritoCompra.appendChild(resultado);

    btnAgregarOtroBoleto?.classList.add("hidden");
    btnReservarBoletos?.classList.add("hidden");

    if (cantidadTotalCompra) {
        cantidadTotalCompra.textContent = "0";
    }

    if (totalCompra) {
        totalCompra.textContent = "$0.00";
    }
}

/**
 * Realiza el pago de una reserva.
 */
async function pagarReserva(compraId, botonPago, totalTexto) {
    if (pagoEnProceso) return;

    // Confirmación en dos pasos: primer clic muestra el monto a confirmar,
    // segundo clic ejecuta el pago. Evita cargos accidentales.
    if (botonPago && botonPago.dataset.confirm !== "pending") {
        botonPago.dataset.confirm = "pending";
        if (botonPago.dataset.originalText === undefined) {
            botonPago.dataset.originalText = botonPago.textContent;
        }
        botonPago.textContent = "⚠ Confirmar pago";
        botonPago.classList.add("btn-confirm");

        clearTimeout(botonPago._confirmTimer);
        botonPago._confirmTimer = setTimeout(() => {
            delete botonPago.dataset.confirm;
            botonPago.classList.remove("btn-confirm");
            botonPago.textContent = botonPago.dataset.originalText ?? "Pagar ahora";
        }, 6000);

        return;
    }

    if (botonPago) {
        clearTimeout(botonPago._confirmTimer);
        delete botonPago.dataset.confirm;
        botonPago.classList.remove("btn-confirm");
    }

    pagoEnProceso = true;

    limpiarMensajeCompra();

    setButtonLoading(
        botonPago,
        true,
        "Procesando pago..."
    );

    try {
        const pago = await apiFetch(`/compras/${compraId}/pagar`, {
            method: "POST",
            auth: true
        });

        manejarPagoExitoso(pago);

    } catch (error) {
        console.error("Error al realizar el pago:", error);

        if (error instanceof ApiError && error.status === 0) {
            mostrarMensajeCompra(
                "No se pudo conectar con el servidor.",
                "error"
            );
        } else {
            manejarApiErrorPago(error?.data);
        }

        setButtonLoading(
            botonPago,
            false
        );

    } finally {
        pagoEnProceso = false;
    }
}

/**
 * Procesa el pago aprobado.
 */
function manejarPagoExitoso(pago) {
    mostrarResultadoPago(pago);
}

/**
 * Muestra la confirmación final.
 */
function mostrarResultadoPago(pago) {
    if (!carritoCompra) return;

    carritoCompra.innerHTML = "";

    const resultado = document.createElement("div");
    resultado.className = "purchase-result";

    const icono = document.createElement("div");
    icono.className = "purchase-result-icon";
    icono.textContent = "✓";

    const titulo = document.createElement("h3");
    titulo.textContent = "¡Compra realizada!";

    const texto = document.createElement("p");
    texto.textContent =
        "Tu pago fue aprobado y tus boletos han sido adquiridos correctamente.";

    const datos = document.createElement("div");
    datos.className = "purchase-result-data";

    datos.appendChild(
        crearDatoResultado(
            "Compra",
            `#${pago?.compraId ?? "—"}`
        )
    );

    datos.appendChild(
        crearDatoResultado(
            "Estado",
            pago?.estado ?? "—"
        )
    );

    datos.appendChild(
        crearDatoResultado(
            "Total pagado",
            formatearPrecio(pago?.monto)
        )
    );

    const aviso = document.createElement("div");
    aviso.className = "purchase-result-success";
    aviso.textContent =
        "Tu compra ha sido confirmada.";

    resultado.append(
        icono,
        titulo,
        texto,
        datos,
        aviso
    );

    carritoCompra.appendChild(resultado);

    btnAgregarOtroBoleto?.classList.add("hidden");
    btnReservarBoletos?.classList.add("hidden");

    if (cantidadTotalCompra) {
        cantidadTotalCompra.textContent = "0";
    }

    if (totalCompra) {
        totalCompra.textContent = "$0.00";
    }
}

/**
 * Crea una fila de información del resultado.
 */
function crearDatoResultado(labelTexto, valorTexto) {
    const dato = document.createElement("div");
    dato.className = "purchase-result-row";

    const label = document.createElement("span");
    label.textContent = labelTexto;

    const valor = document.createElement("strong");
    valor.textContent = valorTexto;

    dato.append(label, valor);

    return dato;
}

function mostrarCarritoVacio() {
    const estado = document.createElement("div");
    estado.className = "purchase-empty";

    const titulo = document.createElement("h3");
    titulo.textContent = "No hay boletos seleccionados";

    const texto = document.createElement("p");
    texto.textContent =
        "Regresa al detalle del evento y selecciona los boletos que deseas reservar.";

    estado.append(titulo, texto);
    carritoCompra.appendChild(estado);
}

/**
 * Muestra un mensaje simple.
 */
function mostrarMensajeCompra(texto, tipo) {
    if (!mensajeCompra) return;

    limpiarMensajeCompra();

    const textoElemento = document.createElement("span");
    textoElemento.textContent = texto;

    mensajeCompra.appendChild(textoElemento);
    mensajeCompra.classList.remove("hidden");
    mensajeCompra.classList.add(tipo);

    mensajeCompra.scrollIntoView({
        behavior: "smooth",
        block: "nearest"
    });
}

/**
 * Muestra errores provenientes del backend.
 */
function manejarApiErrorCompra(data) {
    if (!mensajeCompra) return;

    limpiarMensajeCompra();

    const titulo = document.createElement("strong");
    titulo.textContent = "No fue posible reservar los boletos.";

    mensajeCompra.appendChild(titulo);

    agregarDetalleErrores(mensajeCompra, obtenerListaErrores(data));

    mensajeCompra.classList.remove("hidden");
    mensajeCompra.classList.add("error");

    mensajeCompra.scrollIntoView({
        behavior: "smooth",
        block: "nearest"
    });
}

/**
 * Muestra errores provenientes del pago.
 */
function manejarApiErrorPago(data) {
    if (!mensajeCompra) return;

    limpiarMensajeCompra();

    const titulo = document.createElement("strong");
    titulo.textContent = "No fue posible realizar el pago.";

    mensajeCompra.appendChild(titulo);

    agregarDetalleErrores(mensajeCompra, obtenerListaErrores(data));

    mensajeCompra.classList.remove("hidden");
    mensajeCompra.classList.add("error");

    mensajeCompra.scrollIntoView({
        behavior: "smooth",
        block: "nearest"
    });
}

function limpiarMensajeCompra() {
    if (!mensajeCompra) return;

    mensajeCompra.innerHTML = "";
    mensajeCompra.classList.add("hidden");
    mensajeCompra.classList.remove("error", "success");
}
