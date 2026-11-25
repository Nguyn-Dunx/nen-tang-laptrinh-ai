package vn.edu.codelearn.platform.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class AuthServiceTest {

    private final AccountRepository accounts = mock(AccountRepository.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private final AuthService auth =
            new AuthService(accounts, passwords, mock(AuthenticationManager.class));

    @Test
    void rejectsShortRegistrationPasswordBeforeHashingOrWriting() {
        var credentials = new AuthDtos.Credentials("learner@example.test", "a".repeat(14));

        assertThatThrownBy(() -> auth.register(credentials))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        error ->
                                assertThat(error.getStatusCode())
                                        .isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(accounts, passwords);
    }

    @Test
    void returnsUnauthorizedForSessionWhoseAccountNoLongerExists() {
        when(accounts.findByEmail("learner@example.test")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auth.profile("learner@example.test"))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        error ->
                                assertThat(error.getStatusCode())
                                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }
}
