package li.sebastianmueller.planner.service;

import li.sebastianmueller.planner.dto.LoginRequest;
import li.sebastianmueller.planner.dto.LoginResponse;
import li.sebastianmueller.planner.dto.MeResponse;
import li.sebastianmueller.planner.dto.RefreshTokenRequest;
import li.sebastianmueller.planner.entity.User;
import li.sebastianmueller.planner.repository.UserRepository;
import li.sebastianmueller.planner.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String accessToken = jwtUtil.generateAccessToken(
                user.getId(), user.getDisplayName(), user.getEmail());

        String refreshToken = UUID.randomUUID().toString();
        user.setGoogleRefreshTokenEncrypted(refreshToken);
        userRepository.save(user);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(accessTokenExpirationMs / 1000)
                .build();
    }

    @Transactional
    public LoginResponse refresh(RefreshTokenRequest request) {
        User user = userRepository.findByGoogleRefreshTokenEncrypted(request.getRefreshToken())
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired refresh token"));

        String accessToken = jwtUtil.generateAccessToken(
                user.getId(), user.getDisplayName(), user.getEmail());

        // Rotate the refresh token on each use
        String newRefreshToken = UUID.randomUUID().toString();
        user.setGoogleRefreshTokenEncrypted(newRefreshToken);
        userRepository.save(user);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(accessTokenExpirationMs / 1000)
                .build();
    }

    @Transactional
    public void logout() {
        String userId = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        userRepository.findById(UUID.fromString(userId)).ifPresent(user -> {
            user.setGoogleRefreshTokenEncrypted(null);
            userRepository.save(user);
        });
    }

    @Transactional(readOnly = true)
    public MeResponse me() {
        String userId = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        return MeResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .build();
    }
}
