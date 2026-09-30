import { initAuth, inicializarSesion } from "./auth.js";
import { initEventos, cargarEventos } from "./event.js";

initAuth();
initEventos();

// Los módulos ES ya esperan al DOM (defer por defecto).
// Lanzamos ambas tareas en paralelo; ninguna depende de la otra.
await Promise.allSettled([cargarEventos(), inicializarSesion()]);
