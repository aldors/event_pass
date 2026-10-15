import { initAuth, inicializarSesion } from "./auth.js";
import { initEventos, cargarEventos } from "./event.js";
import { initCompra } from "./compra.js";
import { initCompras } from "./compras.js";
import { initAdmin } from "./admin.js";
import { procesarRetornoPago } from "./payment-return.js";


initAuth();
initCompra();
initEventos();
initCompras();
initAdmin();

// Lanzamos ambas tareas en paralelo; ninguna depende de la otra.
await Promise.allSettled([cargarEventos(), inicializarSesion()]);

await procesarRetornoPago();