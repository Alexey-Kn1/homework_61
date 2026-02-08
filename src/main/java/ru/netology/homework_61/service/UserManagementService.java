package ru.netology.homework_61.service;

import jakarta.transaction.Transactional;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.netology.homework_61.model.User;
import ru.netology.homework_61.repository.CloudServiceRepository;

import java.security.SecureRandom;
import java.util.Random;

@Service
public class UserManagementService {
    private final CloudServiceRepository repository;
    private final Random rand;
    private final PasswordEncoder passwordEncoder;

    public UserManagementService(CloudServiceRepository repository, PasswordEncoder encoder) {
        this.repository = repository;
        rand = new SecureRandom();
        passwordEncoder = encoder;
    }

    // Creates a user, returns access token.
    @Transactional
    public void registerNewUser(String login, String password) throws UserAlreadyExistsException {
        if (repository.findUserByLogin(login).isPresent()) {
            throw new UserAlreadyExistsException(login);
        }

        repository.addUser(
                new User(0, login, passwordEncoder.encode(password))
        );
    }

    // Returns access token.
    public String login(String login, String password) throws UserNotFoundException, PasswordMismatchException {
        var user = repository.findUserByLogin(login);

        if (user.isEmpty()) {
            throw new UserNotFoundException(login);
        }

        if (!passwordEncoder.matches(password, user.get().getPasswordHash())) {
            throw new PasswordMismatchException();
        }

        var token = generateAccessToken(login, password);

        repository.saveUserSession(token, user.get());

        return token;
    }

    public void logout(String authToken) {
        repository.deleteUserSession(authToken);
    }

    private String generateAccessToken(String login, String password) {
        return RandomStringUtils.random(100, 0, 0, true, true, null, rand);
    }
}
