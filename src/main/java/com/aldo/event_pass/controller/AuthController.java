package com.aldo.event_pass.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aldo.event_pass.dto.auth.LoginRequest;
import com.aldo.event_pass.dto.auth.LoginResponse;
import com.aldo.event_pass.dto.auth.LogoutRequest;
import com.aldo.event_pass.dto.auth.MeResponse;
import com.aldo.event_pass.dto.auth.RefreshTokenRequest;
import com.aldo.event_pass.dto.auth.RegistroRequest;
import com.aldo.event_pass.dto.auth.RegistroResponse;
import com.aldo.event_pass.exception.ApiError.ApiError;
import com.aldo.event_pass.service.interfaces.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Autenticación", description = "Registro, inicio de sesión y gestión de sesión con JWT")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor 
public class AuthController {
    
    private final AuthService authService;

    @Operation(summary = "Registrar usuario", description = "Crea una cuenta nueva con rol USER. Endpoint público.", security = {})
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Usuario registrado",
            content = @Content(schema = @Schema(implementation = RegistroResponse.class))),
        @ApiResponse(responseCode = "400", description = "Datos inválidos",
            content = @Content(schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(name = "Error 400", value = "{\"status\":400,\"message\":\"Error de validación\",\"timestamp\":\"2026-10-08T10:00:00\",\"errors\":[\"La solicitud contiene datos inválidos\"]}"))),
        @ApiResponse(responseCode = "409", description = "Email ya registrado",
            content = @Content(schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(name = "Error 409", value = "{\"status\":409,\"message\":\"Conflicto\",\"timestamp\":\"2026-10-08T10:00:00\",\"errors\":[\"Conflicto con el estado actual\"]}")))
    })
    @PostMapping("/registro")
    public ResponseEntity<RegistroResponse> registro(@Valid @RequestBody RegistroRequest registroRequest){
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registro(registroRequest));
    }

    @Operation(summary = "Iniciar sesión", description = "Autentica con email y password, devuelve access y refresh token. Endpoint público.", security = {})
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login exitoso",
            content = @Content(schema = @Schema(implementation = LoginResponse.class))),
        @ApiResponse(responseCode = "400", description = "Datos inválidos",
            content = @Content(schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(name = "Error 400", value = "{\"status\":400,\"message\":\"Error de validación\",\"timestamp\":\"2026-10-08T10:00:00\",\"errors\":[\"La solicitud contiene datos inválidos\"]}"))),
        @ApiResponse(responseCode = "401", description = "Credenciales incorrectas")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest){
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @Operation(summary = "Renovar tokens", description = "Genera nuevos tokens a partir de un refresh token válido. Endpoint público.", security = {})
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tokens renovados",
            content = @Content(schema = @Schema(implementation = LoginResponse.class))),
        @ApiResponse(responseCode = "400", description = "Datos inválidos",
            content = @Content(schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(name = "Error 400", value = "{\"status\":400,\"message\":\"Error de validación\",\"timestamp\":\"2026-10-08T10:00:00\",\"errors\":[\"La solicitud contiene datos inválidos\"]}"))),
        @ApiResponse(responseCode = "401", description = "Refresh token inválido o expirado")
    })
    @PostMapping("/refresh-token")
    public ResponseEntity<LoginResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest){
        return ResponseEntity.ok(authService.refreshToken(refreshTokenRequest));
    }

    @Operation(summary = "Cerrar sesión", description = "Invalida el refresh token. Requiere JWT.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sesión cerrada"),
        @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@Valid @RequestBody LogoutRequest logoutRequest){
        return ResponseEntity.ok(authService.logout(logoutRequest));
    }

    @Operation(summary = "Obtener perfil actual", description = "Devuelve los datos del usuario autenticado y su rol. Requiere JWT.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Perfil obtenido",
            content = @Content(schema = @Schema(implementation = MeResponse.class))),
        @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(){
        return ResponseEntity.ok(authService.me());
    }
}
