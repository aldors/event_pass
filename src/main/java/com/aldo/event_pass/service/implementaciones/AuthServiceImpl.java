package com.aldo.event_pass.service.implementaciones;

import java.time.LocalDateTime;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aldo.event_pass.dto.auth.LoginRequest;
import com.aldo.event_pass.dto.auth.LoginResponse;
import com.aldo.event_pass.dto.auth.LogoutRequest;
import com.aldo.event_pass.dto.auth.MeResponse;
import com.aldo.event_pass.dto.auth.RefreshTokenRequest;
import com.aldo.event_pass.dto.auth.RegistroRequest;
import com.aldo.event_pass.dto.auth.RegistroResponse;
import com.aldo.event_pass.entity.RefreshToken;
import com.aldo.event_pass.entity.Usuario;
import com.aldo.event_pass.mapper.LoginMapper;
import com.aldo.event_pass.mapper.RegistroMapper;
import com.aldo.event_pass.repository.RefreshTokenRepository;
import com.aldo.event_pass.repository.UsuarioRepository;
import com.aldo.event_pass.security.CurrentUserService;
import com.aldo.event_pass.security.JwtService;
import com.aldo.event_pass.service.interfaces.AuthService;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CurrentUserService currentUserService;

    @Override
    public RegistroResponse registro(RegistroRequest registroRequest) {

        if(usuarioRepository.findByEmail(registroRequest.getEmail()).isPresent()){
            throw new RuntimeException("Este usuario ya existe");
        }

        Usuario usuario = RegistroMapper.toEntity(registroRequest);
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        return RegistroMapper.toResponse(usuarioRepository.save(usuario));
    }

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                loginRequest.getEmail(),
                loginRequest.getPassword()
            )
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getEmail());

        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        Usuario usuario = usuarioRepository.findByEmail(loginRequest.getEmail())
            .orElseThrow(() -> new RuntimeException());

        RefreshToken refreshTokenEntity = RefreshToken.builder()
            .token(refreshToken)
            .usuario(usuario)
            .build();

        refreshTokenRepository.save(refreshTokenEntity);

        return LoginMapper.toResponse(accessToken, refreshToken);
    }

    @Override
    public LoginResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        
        RefreshToken refreshTokenEntity = refreshTokenRepository.findByToken(refreshTokenRequest.getRefreshToken())
            .orElseThrow(() -> new RuntimeException());

        if(refreshTokenEntity.getExpirationDate().isBefore(LocalDateTime.now())){
            refreshTokenRepository.deleteByToken(refreshTokenRequest.getRefreshToken());
            throw new RuntimeException();
        }

        Usuario usuario = refreshTokenEntity.getUsuario();
        String email = usuario.getEmail();

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        if(!jwtService.isValid(refreshTokenRequest.getRefreshToken(), userDetails)){
            throw new RuntimeException();
        }

        String nuevoAccesToken = jwtService.generateAccessToken(userDetails);

        return LoginMapper.toResponse(nuevoAccesToken, refreshTokenRequest.getRefreshToken());
    }

    @Override
    @Transactional
    public String logout(LogoutRequest logoutRequest) {

        if(!refreshTokenRepository.findByToken(logoutRequest.getRefreshToken()).isPresent()){
            throw new RuntimeException();
        }

        refreshTokenRepository.deleteByToken(logoutRequest.getRefreshToken());

        return "Sesión cerrada...";
    }

    @Override
    public MeResponse me() {

        Usuario usuario = currentUserService.obtenerUsuarioActual();
        return new MeResponse(usuario.getNombre(), usuario.getEmail());
    }
    
}
