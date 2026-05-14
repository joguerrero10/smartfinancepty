package com.smartfinancepty.finance.service.auth;

import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.smartfinancepty.finance.domain.RefreshToken;
import com.smartfinancepty.finance.domain.Role;
import com.smartfinancepty.finance.domain.User;
import com.smartfinancepty.finance.dto.AuthResponse;
import com.smartfinancepty.finance.dto.LoginRequest;
import com.smartfinancepty.finance.dto.RefreshTokenRequest;
import com.smartfinancepty.finance.dto.RegisterRequest;
import com.smartfinancepty.finance.exception.EmailAlreadyExistsException;
import com.smartfinancepty.finance.exception.InvalidTokenException;
import com.smartfinancepty.finance.repository.RefreshTokenRepository;
import com.smartfinancepty.finance.repository.UserRepository;
import com.smartfinancepty.finance.security.JwtService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;


    @Value("${application.security.jwt.refresh-token.expiration}")
    private long refreshTokenExpiration;


    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(
                    "El email ya está registrado: " + request.getEmail());
        }

        User user = User.builder().fullName(request.getFullName()).email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword())).role(Role.USER)
                .enabled(true).build();

        userRepository.save(user);

        String accessToken = jwtService.generateToken(user);
        String refreshToken = createRefreshToken(user);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Revocar tokens anteriores
        refreshTokenRepository.revokeAllUserTokens(user);

        String accessToken = jwtService.generateToken(user);
        String refreshToken = createRefreshToken(user);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new InvalidTokenException("Refresh token no encontrado"));

        if (refreshToken.isRevoked()) {
            throw new InvalidTokenException("Refresh token revocado");
        }

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new InvalidTokenException("Refresh token expirado, inicia sesión nuevamente");
        }

        User user = refreshToken.getUser();

        // Rotar refresh token (buena práctica de seguridad)
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        String newAccessToken = jwtService.generateToken(user);
        String newRefreshToken = createRefreshToken(user);

        return buildAuthResponse(user, newAccessToken, newRefreshToken);
    }

    @Transactional
    public void logout(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        refreshTokenRepository.revokeAllUserTokens(user);
    }

    private String createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder().token(UUID.randomUUID().toString())
                .user(user).expiryDate(Instant.now().plusMillis(refreshTokenExpiration))
                .revoked(false).build();

        refreshTokenRepository.save(refreshToken);
        return refreshToken.getToken();
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder().accessToken(accessToken).refreshToken(refreshToken)
                .tokenType("Bearer").email(user.getEmail()).fullName(user.getFullName())
                .role(user.getRole().name()).build();
    }

}
