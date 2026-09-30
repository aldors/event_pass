import { API_BASE_URL, REQUEST_TIMEOUT_MS, STORAGE_KEYS } from "./config.js";

export class ApiError extends Error {
    constructor(message, { status = 0, data = null } = {}) {
        super(message);
        this.name = "ApiError";
        this.status = status;
        this.data = data;
    }
}

export function getAccessToken() {
    return localStorage.getItem(STORAGE_KEYS.accessToken);
}

export function getRefreshToken() {
    return localStorage.getItem(STORAGE_KEYS.refreshToken);
}

export function setTokens({ accessToken, refreshToken }) {
    if (accessToken) localStorage.setItem(STORAGE_KEYS.accessToken, accessToken);
    if (refreshToken) localStorage.setItem(STORAGE_KEYS.refreshToken, refreshToken);
}

export function clearTokens() {
    localStorage.removeItem(STORAGE_KEYS.accessToken);
    localStorage.removeItem(STORAGE_KEYS.refreshToken);
}

async function parseJsonSafe(response) {
    const contentType = response.headers.get("content-type") || "";
    if (!contentType.includes("json")) return null;
    try {
        const text = await response.text();
        if (!text) return null;
        return JSON.parse(text);
    } catch {
        return null;
    }
}

export async function apiFetch(path, { method = "GET", body, auth = false, timeoutMs = REQUEST_TIMEOUT_MS } = {}) {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), timeoutMs);

    try {
        const headers = {};
        if (body !== undefined) headers["Content-Type"] = "application/json";
        if (auth) {
            const accessToken = getAccessToken();
            if (accessToken) headers["Authorization"] = `Bearer ${accessToken}`;
        }

        const response = await fetch(`${API_BASE_URL}${path}`, {
            method,
            headers,
            body: body !== undefined ? JSON.stringify(body) : undefined,
            signal: controller.signal
        });

        const data = await parseJsonSafe(response);

        if (!response.ok) {
            throw new ApiError(data?.titulo || `Error ${response.status}`, {
                status: response.status,
                data
            });
        }

        return data;
    } catch (error) {
        if (error instanceof ApiError) throw error;
        if (error?.name === "AbortError") {
            throw new ApiError("La petición tardó demasiado. Intenta de nuevo.", { status: 0 });
        }
        throw new ApiError("No se pudo conectar con el servidor.", { status: 0 });
    } finally {
        clearTimeout(timeoutId);
    }
}

/*
 * Intenta renovar la sesión con el refresh token.
 * Devuelve true si se renovó, false si no (sesión inválida o sin red).
 */
export async function refreshTokens() {
    const refreshToken = getRefreshToken();
    if (!refreshToken) return false;

    try {
        const data = await apiFetch("/auth/refresh-token", {
            method: "POST",
            body: { refreshToken }
        });
        if (!data?.accessToken) return false;
        setTokens({ accessToken: data.accessToken, refreshToken: data.refreshToken });
        return true;
    } catch {
        return false;
    }
}
