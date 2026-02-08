package ru.netology.homework_61;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.netology.homework_61.model.User;
import ru.netology.homework_61.repository.CloudServiceRepository;
import ru.netology.homework_61.service.PasswordMismatchException;
import ru.netology.homework_61.service.UserManagementService;
import ru.netology.homework_61.service.UserNotFoundException;

import java.util.Optional;

public class UserManagementServiceTest {
    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder();
    private static String LOGIN = "user";
    private static final String PASSPHRASE = "passphrase";

    @Test
    public void checkUserManagement() throws Exception {
        var repoMock = Mockito.mock(CloudServiceRepository.class);

        Mockito.when(repoMock.findUserByLogin(Mockito.anyString()))
                .thenReturn(Optional.empty());

        var service = new UserManagementService(repoMock, ENCODER);

        service.registerNewUser(LOGIN, PASSPHRASE);

        Mockito.verify(repoMock, Mockito.times(1))
                .addUser(Mockito.any());

        Mockito.when(repoMock.findUserByLogin(LOGIN))
                .thenReturn(
                        Optional.of(
                                new User(
                                        0, LOGIN, ENCODER.encode(PASSPHRASE)
                                )
                        )
                );

        var token = service.login(LOGIN, PASSPHRASE);

        Mockito.verify(repoMock, Mockito.times(1))
                .saveUserSession(Mockito.eq(token), Mockito.any());

        Assertions.assertThrows(
                UserNotFoundException.class,
                () -> service.login("wrong login", PASSPHRASE)
        );

        Assertions.assertThrows(
                PasswordMismatchException.class,
                () -> service.login(LOGIN, "wrong passphrase")
        );

        service.logout(token);

        Mockito.verify(repoMock, Mockito.times(1))
                .deleteUserSession(Mockito.eq(token));
    }
}
