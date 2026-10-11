
package com.gearshare.backend.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(
        origins = "http://localhost:5173",
        allowCredentials = "true"
)
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(
            @Valid @RequestBody RegisterRequest request) {

        String name = request.name() == null
                ? ""
                : request.name().trim();

        String email = request.email() == null
                ? ""
                : request.email().trim().toLowerCase(Locale.ROOT);

        String phoneNumber = request.phoneNumber() == null
                ? ""
                : request.phoneNumber().trim();

        if (name.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Name is required"
            );
        }

        if (email.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email is required"
            );
        }

        if (phoneNumber.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Phone number is required"
            );
        }

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An account with this email already exists"
            );
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        user.setPasswordHash(
                passwordEncoder.encode(request.password())
        );
        user.setRole("USER");

        User savedUser = userRepository.save(user);

        return toUserResponse(savedUser);
    }

    @PostMapping("/login")
    public UserResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String email = request.email() == null
                ? ""
                : request.email().trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid email or password"
                ));

        if (!passwordEncoder.matches(
                request.password(),
                user.getPasswordHash())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password"
            );
        }

        String role = user.getRole() == null
                || user.getRole().isBlank()
                ? "USER"
                : user.getRole().trim().toUpperCase(Locale.ROOT);

        Authentication authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        user.getEmail(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );

        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        HttpSessionSecurityContextRepository repository =
                new HttpSessionSecurityContextRepository();

        repository.saveContext(
                context,
                httpRequest,
                httpResponse
        );

        return toUserResponse(user);
    }

    @GetMapping("/me")
    public UserResponse currentUser(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "You are not logged in"
            );
        }

        User user = userRepository.findByEmail(
                authentication.getName()
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "User account not found"
        ));

        return toUserResponse(user);
    }
    
@PutMapping("/profile")
public UserResponse updateProfile(
        Authentication authentication,
        @Valid @RequestBody UpdateProfileRequest request) {

    if (authentication == null
            || !authentication.isAuthenticated()) {
        throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "You are not logged in"
        );
    }

    User user = userRepository.findByEmail(
            authentication.getName()
    ).orElseThrow(() -> new ResponseStatusException(
            HttpStatus.UNAUTHORIZED,
            "User account not found"
    ));

    String name = request.name().trim();
    String phoneNumber = request.phoneNumber() == null
            ? ""
            : request.phoneNumber().trim();

    if (name.isBlank()) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Name is required"
        );
    }

    user.setName(name);
    user.setPhoneNumber(
            phoneNumber.isBlank() ? null : phoneNumber
    );

    User savedUser = userRepository.save(user);

    return toUserResponse(savedUser);
}

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            HttpServletRequest request,
            HttpServletResponse response) {

        SecurityContextHolder.clearContext();

        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}