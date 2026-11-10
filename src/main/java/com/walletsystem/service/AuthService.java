package com.walletsystem.service;

import com.walletsystem.Entity.Role;
import com.walletsystem.Entity.User;
import com.walletsystem.Entity.Wallet;
import com.walletsystem.Exception.DuplicateResourceException;
import com.walletsystem.Exception.ResourceNotFoundException;
import com.walletsystem.dto.AuthResponse;
import com.walletsystem.dto.LoginRequest;
import com.walletsystem.dto.RegisterRequest;
import com.walletsystem.repository.UserRepository;
import com.walletsystem.repository.WalletRepository;
import com.walletsystem.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String Email = normalize(request.email());
        if (userRepository.existsByEmail(Email)) {
            throw new DuplicateResourceException("Email is already registered");
        }
        User user = createUserWithWallet(request.fullName().trim(), Email, request.password(), Role.USER);
        return buildResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalize(request.email());
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return buildResponse(user);
    }

    @Transactional
    public void createAdminIfMissing(String email, String rawPassword) {
        String normalized = normalize(email);
        if (userRepository.existsByEmail(normalized)) {
            return;
        }
        createUserWithWallet("Administrator", normalized, rawPassword, Role.ADMIN);
        log.info("Default admin user created: {}", normalized);
    }

    private User createUserWithWallet(String fullName, String email, String rawPassword, Role role) {
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        userRepository.save(user);

        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(BigDecimal.ZERO.setScale(2));
        walletRepository.save(wallet);

        return user;
    }

    private AuthResponse buildResponse(User user) {
        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        return new AuthResponse(token, "Bearer", jwtService.getExpirationMs(), user.getEmail(), user.getRole().name());
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
