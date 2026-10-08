import {
    apiFetch,
    getAccessToken,
    getRefreshToken,
    setTokens,
    clearTokens,
    refreshTokens,
    ApiError
} from "./api.js";
import { setButtonLoading, obtenerListaErrores, agregarDetalleErrores } from "./utils.js";

export const AUTH_REQUIRED_EVENT = "auth:required";
export const AUTH_CHANGED_EVENT = "auth:changed";

const modalAuth = document.getElementById("modalAuth");
const btnAuth = document.getElementById("btnAuth");
const btnLogout = document.getElementById("btnLogout");
const saludoUsuario = document.getElementById("saludoUsuario");
const cerrarModal = document.getElementById("cerrarModal");
const btnMisCompras = document.getElementById("btnMisCompras");

const tabLogin = document.getElementById("tabLogin");
const tabRegistro = document.getElementById("tabRegistro");

const loginForm = document.getElementById("loginForm");
const registroForm = document.getElementById("registroForm");

const mensaje = document.getElementById("mensaje");
const mensajeRegistro = document.getElementById("mensajeRegistro");

let authInitDone = false;
let lastFocusedElement = null;
let usuarioActual = null;

/* =========================
   INIT (llamado una vez desde app.js)
========================= */
export function initAuth() {
    if (authInitDone) return;
    authInitDone = true;

    btnAuth?.addEventListener("click", abrirModalLogin);
    cerrarModal?.addEventListener("click", cerrarModalAuth);
    tabLogin?.addEventListener("click", mostrarLogin);
    tabRegistro?.addEventListener("click", mostrarRegistro);
    loginForm?.addEventListener("submit", iniciarSesion);
    registroForm?.addEventListener("submit", registrarUsuario);
    btnLogout?.addEventListener("click", cerrarSesion);

    modalAuth?.addEventListener("click", (event) => {
        if (event.target === modalAuth) cerrarModalAuth();
    });

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && modalAuth && !modalAuth.classList.contains("hidden")) {
            cerrarModalAuth();
        }
    });

    // Otros módulos (eventos/compra) piden login sin acoplarse a window.
    window.addEventListener(AUTH_REQUIRED_EVENT, abrirModalLogin);
}

/* =========================
   INICIALIZACIÓN
========================= */
export async function inicializarSesion() {
    const accessToken = getAccessToken();
    if (!accessToken) {
        mostrarUsuarioNoAutenticado();
        return;
    }

    try {
        const usuario = await apiFetch("/auth/me", { auth: true });
        mostrarUsuarioAutenticado(usuario);
    } catch (error) {
        if (error instanceof ApiError && error.status === 401) {
            // Access expirado: intentar refresh una vez antes de cerrar sesión.
            const renovado = await refreshTokens();
            if (renovado) {
                try {
                    const usuario = await apiFetch("/auth/me", { auth: true });
                    mostrarUsuarioAutenticado(usuario);
                    return;
                } catch {
                    // cae al logout silencioso
                }
            }
            clearTokens();
            mostrarUsuarioNoAutenticado();
            return;
        }
        // Sin red u otro error: NO borramos tokens, solo mostramos estado neutro.
        console.error("Error al verificar la sesión:", error);
        mostrarUsuarioNoAutenticado();
    }
}

/* =========================
   LOGIN
========================= */
async function iniciarSesion(event) {
    event.preventDefault();
    limpiarMensaje(mensaje);

    const email = document.getElementById("loginEmail")?.value.trim() ?? "";
    const password = document.getElementById("loginPassword")?.value ?? "";
    const submitBtn = loginForm?.querySelector('button[type="submit"]');

    // Sin validación bloqueante en frontend: el backend es la única
    // fuente de verdad y devuelve message + errors[] vía manejarApiError.
    setButtonLoading(submitBtn, true, "Ingresando...");
    try {
        const data = await apiFetch("/auth/login", {
            method: "POST",
            body: { email, password }
        });

        setTokens({ accessToken: data?.accessToken, refreshToken: data?.refreshToken });

        loginForm.reset();
        cerrarModalAuth();
        await inicializarSesion();
    } catch (error) {
        console.error("Error al iniciar sesión:", error);
        if (error instanceof ApiError && error.status === 0) {
            mostrarMensaje(mensaje, "No se pudo conectar con el servidor.", "error");
        } else {
            manejarApiError(error?.data, mensaje, "No se pudo iniciar sesión.");
        }
    } finally {
        setButtonLoading(submitBtn, false);
    }
}

/* =========================
   REGISTRO
========================= */
async function registrarUsuario(event) {
    event.preventDefault();
    limpiarMensaje(mensajeRegistro);

    const nombre = document.getElementById("nombre")?.value.trim() ?? "";
    const apellido = document.getElementById("apellido")?.value.trim() ?? "";
    const email = document.getElementById("registroEmail")?.value.trim() ?? "";
    const password = document.getElementById("registroPassword")?.value ?? "";
    const submitBtn = registroForm?.querySelector('button[type="submit"]');

    // Sin validación bloqueante en frontend: el backend es la única
    // fuente de verdad y devuelve message + errors[] vía manejarApiError.
    setButtonLoading(submitBtn, true, "Creando cuenta...");
    try {
        await apiFetch("/auth/registro", {
            method: "POST",
            body: { nombre, apellido, email, password }
        });

        mostrarMensaje(mensajeRegistro, "Cuenta creada correctamente. Ahora puedes iniciar sesión.", "success");
        registroForm.reset();

        setTimeout(() => {
            mostrarLogin();
            const loginEmail = document.getElementById("loginEmail");
            if (loginEmail) loginEmail.value = email;
            loginEmail?.focus();
        }, 1200);
    } catch (error) {
        console.error("Error al registrar usuario:", error);
        if (error instanceof ApiError && error.status === 0) {
            mostrarMensaje(mensajeRegistro, "No se pudo conectar con el servidor.", "error");
        } else {
            manejarApiError(error?.data, mensajeRegistro, "No se pudo crear tu cuenta.");
        }
    } finally {
        setButtonLoading(submitBtn, false);
    }
}

/* =========================
   LOGOUT (sin reload)
========================= */
async function cerrarSesion() {
    const accessToken = getAccessToken();
    const refreshToken = getRefreshToken();
    setButtonLoading(btnLogout, true, "Cerrando...");

    try {
        if (accessToken && refreshToken) {
            await apiFetch("/auth/logout", {
                method: "POST",
                auth: true,
                body: { refreshToken }
            });
        }
    } catch (error) {
        console.error("Error al cerrar sesión:", error);
    } finally {
        clearTokens();
        setButtonLoading(btnLogout, false);
        mostrarUsuarioNoAutenticado();
    }
}

/* =========================
   MODAL
========================= */
export function abrirModalLogin() {
    if (!modalAuth) return;
    lastFocusedElement = document.activeElement;
    modalAuth.classList.remove("hidden");
    document.body.style.overflow = "hidden";
    mostrarLogin();
    document.getElementById("loginEmail")?.focus();
}

function cerrarModalAuth() {
    if (!modalAuth) return;
    modalAuth.classList.add("hidden");
    document.body.style.overflow = "";
    limpiarMensaje(mensaje);
    limpiarMensaje(mensajeRegistro);
    if (lastFocusedElement instanceof HTMLElement) lastFocusedElement.focus();
}

function mostrarLogin() {
    loginForm?.classList.remove("hidden");
    registroForm?.classList.add("hidden");
    tabLogin?.classList.add("active");
    tabRegistro?.classList.remove("active");
    tabLogin?.setAttribute("aria-selected", "true");
    tabRegistro?.setAttribute("aria-selected", "false");
    limpiarMensaje(mensaje);
    limpiarMensaje(mensajeRegistro);
}

function mostrarRegistro() {
    loginForm?.classList.add("hidden");
    registroForm?.classList.remove("hidden");
    tabLogin?.classList.remove("active");
    tabRegistro?.classList.add("active");
    tabLogin?.setAttribute("aria-selected", "false");
    tabRegistro?.setAttribute("aria-selected", "true");
    limpiarMensaje(mensaje);
    limpiarMensaje(mensajeRegistro);
}

/* =========================
   ESTADO DE SESIÓN
========================= */
function mostrarUsuarioAutenticado(usuario) {

    usuarioActual = usuario ?? null;

    btnAuth?.classList.add("hidden");

    if (saludoUsuario) {
        saludoUsuario.textContent = usuario?.nombre ? `Hola, ${usuario.nombre}` : "Hola";
        saludoUsuario.classList.remove("hidden");
    }
    btnMisCompras?.classList.remove("hidden");
    btnLogout?.classList.remove("hidden");

    window.dispatchEvent(
        new CustomEvent(AUTH_CHANGED_EVENT, {
            detail: {
                autenticado: true,
                usuario: usuarioActual
            }
        })
    );
}

function mostrarUsuarioNoAutenticado() {

    usuarioActual = null;

    saludoUsuario?.classList.add("hidden");
    btnMisCompras?.classList.add("hidden");
    btnLogout?.classList.add("hidden");
    btnAuth?.classList.remove("hidden");

    window.dispatchEvent(
        new CustomEvent(AUTH_CHANGED_EVENT, {
            detail: {
                autenticado: false,
                usuario: null
            }
        })
    );
}

/* =========================
   ERRORES DE API
========================= */
/*
 * El título es fijo por contexto (no se muestra el `message` del backend).
 * El detalle sí viene del backend: un error como texto simple,
 * varios errores como lista.
 */
function manejarApiError(data, contenedor, tituloFijo = "Ocurrió un error.") {
    if (!contenedor) return;
    limpiarMensaje(contenedor);

    const mensajeTitulo = document.createElement("strong");
    mensajeTitulo.textContent = tituloFijo;
    contenedor.appendChild(mensajeTitulo);

    agregarDetalleErrores(contenedor, obtenerListaErrores(data));

    contenedor.classList.remove("hidden");
    contenedor.classList.add("error");

    const reduceMotion = window.matchMedia?.("(prefers-reduced-motion: reduce)").matches;
    contenedor.scrollIntoView({ block: "nearest", behavior: reduceMotion ? "auto" : "smooth" });
}

function mostrarMensaje(contenedor, texto, tipo) {
    if (!contenedor) return;
    limpiarMensaje(contenedor);
    const textoElemento = document.createElement("span");
    textoElemento.textContent = texto;
    contenedor.appendChild(textoElemento);
    contenedor.classList.remove("hidden");
    contenedor.classList.add(tipo);
}

function limpiarMensaje(contenedor) {
    if (!contenedor) return;
    contenedor.innerHTML = "";
    contenedor.classList.add("hidden");
    contenedor.classList.remove("error", "success");
}
