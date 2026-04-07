package io.droidevs.mclub.service;
import io.droidevs.mclub.domain.User;
import io.droidevs.mclub.dto.*;
import io.droidevs.mclub.repository.UserRepository;
import io.droidevs.mclub.security.JwtTokenProvider;
import io.droidevs.mclub.security.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthResponse authenticateUser(AuthRequest request) {
        Authentication auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        return new AuthResponse(tokenProvider.generateToken(auth));
    }
    public void registerUser(RegisterRequest req) {
        if(userRepository.findByEmail(req.getEmail()).isPresent()) throw new RuntimeException("Email already exists");
        Role r = req.getRole() != null && req.getRole().equalsIgnoreCase("ADMIN") ? Role.ADMIN : Role.MEMBER;
        User user = User.builder().email(req.getEmail()).password(passwordEncoder.encode(req.getPassword()))
                .fullName(req.getFullName()).role(r).build();
        userRepository.save(user);
    }
}
