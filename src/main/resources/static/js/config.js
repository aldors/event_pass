// Base de la API.
// Vacío = mismo origen (funciona en localhost:8080 y en producción sin CORS).
// Con Live Server (puerto 5500), cambia a "http://localhost:8080".
export const API_BASE_URL = "http://localhost:8080";

export const REQUEST_TIMEOUT_MS = 10000;

export const STORAGE_KEYS = {
    accessToken: "accessToken",
    refreshToken: "refreshToken"
};
