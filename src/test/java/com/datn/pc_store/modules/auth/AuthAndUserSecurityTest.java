package com.datn.pc_store.modules.auth;

import com.datn.pc_store.modules.auth.dto.AuthResponse;
import com.datn.pc_store.modules.auth.dto.RegisterRequest;
import com.datn.pc_store.modules.auth.entity.AccountChallenge;
import com.datn.pc_store.modules.auth.enums.AccountChallengePurpose;
import com.datn.pc_store.modules.auth.repository.AccountChallengeRepository;
import com.datn.pc_store.modules.auth.security.CustomUserDetails;
import com.datn.pc_store.modules.auth.security.JwtService;
import com.datn.pc_store.modules.auth.service.AccountVerificationService;
import com.datn.pc_store.modules.auth.service.AuthService;
import com.datn.pc_store.modules.auth.service.ChallengeDeliveryService;
import com.datn.pc_store.modules.auth.service.RefreshTokenService;
import com.datn.pc_store.modules.user.entity.User;
import com.datn.pc_store.modules.user.enums.RoleType;
import com.datn.pc_store.modules.user.enums.UserStatus;
import com.datn.pc_store.modules.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthAndUserSecurityTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    @Mock
    private AccountChallengeRepository challengeRepository;
    @Mock
    private ChallengeDeliveryService challengeDeliveryService;

    @Test
    @DisplayName("CustomUserDetails.isEnabled() trả false khi UserStatus.INACTIVE và true khi ACTIVE")
    void customUserDetailsReflectsUserStatus() {
        User inactive = User.builder()
                .id(1L)
                .username("inactive")
                .role(RoleType.CUSTOMER)
                .status(UserStatus.INACTIVE)
                .build();
        User active = User.builder()
                .id(2L)
                .username("active")
                .role(RoleType.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        assertFalse(new CustomUserDetails(inactive).isEnabled());
        assertTrue(new CustomUserDetails(active).isEnabled());
    }

    @Test
    @DisplayName("JwtService.parseClaimsIfValid parse 1 lần trả về claims và extractTokenVersion chính xác")
    void jwtServiceSinglePassParsing() {
        JwtService realJwt = new JwtService("0123456789abcdef0123456789abcdef0123456789abcdef", 3_600_000L);
        User user = User.builder()
                .id(10L)
                .username("customer01")
                .role(RoleType.CUSTOMER)
                .tokenVersion(3)
                .build();

        String token = realJwt.generateToken(user);
        Optional<Claims> parsed = realJwt.parseClaimsIfValid(token);

        assertTrue(parsed.isPresent());
        assertEquals("customer01", parsed.get().getSubject());
        assertEquals(3, realJwt.extractTokenVersion(parsed.get()));
        assertTrue(realJwt.parseClaimsIfValid("invalid.jwt.token").isEmpty());
    }

    @Test
    @DisplayName("AuthService.register từ chối email trùng không phân biệt hoa thường")
    void registerRejectsCaseInsensitiveDuplicateEmail() {
        RegisterRequest req = new RegisterRequest(
                "newuser01", "Password123!", "Nguyễn A", "User@Example.com", null, null
        );
        when(userRepository.existsByUsername("newuser01")).thenReturn(false);
        when(userRepository.existsByEmail("User@Example.com")).thenReturn(false);
        when(userRepository.findByEmailIgnoreCase("User@Example.com"))
                .thenReturn(Optional.of(User.builder().id(99L).build()));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.register(req));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("AuthService.register điền fallback fullName = username khi bỏ trống fullName để không lỗi NOT NULL DB")
    void registerDefaultsBlankFullNameToUsername() {
        RegisterRequest req = new RegisterRequest(
                "  customer02  ", "Password123!", "   ", "c02@example.com", null, null
        );
        when(userRepository.existsByUsername("customer02")).thenReturn(false);
        when(userRepository.existsByEmail("c02@example.com")).thenReturn(false);
        when(userRepository.findByEmailIgnoreCase("c02@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Password123!")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(5L);
            return u;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("access-jwt");
        when(refreshTokenService.issue(5L)).thenReturn("refresh-raw");

        AuthResponse response = authService.register(req);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("customer02", captor.getValue().getUsername());
        assertEquals("customer02", captor.getValue().getFullName());
        assertNotNull(response);
    }

    @Test
    @DisplayName("AccountVerificationService.verifyEmail khớp destination không phân biệt hoa thường")
    void verifyEmailMatchesDestinationCaseInsensitively() {
        AccountVerificationService verificationService = new AccountVerificationService(
                userRepository, challengeRepository, passwordEncoder, challengeDeliveryService, refreshTokenService
        );
        User user = User.builder()
                .id(7L)
                .username("user7")
                .email("User7@Example.com")
                .emailVerified(false)
                .build();
        AccountChallenge challenge = new AccountChallenge();
        challenge.setId(1L);
        challenge.setUserId(7L);
        challenge.setPurpose(AccountChallengePurpose.VERIFY_EMAIL);
        challenge.setDestination("user7@example.com");
        challenge.setCodeHash("encoded");
        challenge.setExpiresAt(Instant.now().plusSeconds(300));
        challenge.setConsumed(false);

        when(userRepository.findLockedById(7L)).thenReturn(Optional.of(user));
        when(challengeRepository.findByUserIdAndPurpose(7L, AccountChallengePurpose.VERIFY_EMAIL))
                .thenReturn(Optional.of(challenge));
        when(passwordEncoder.matches("123456", "encoded")).thenReturn(true);

        verificationService.verifyEmail(7L, "123456");

        assertTrue(user.isEmailVerified());
        assertTrue(challenge.isConsumed());
    }
}
