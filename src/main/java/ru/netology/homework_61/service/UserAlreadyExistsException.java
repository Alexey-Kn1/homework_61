package ru.netology.homework_61.service;

public class UserAlreadyExistsException extends CloudServiceException {
    private final String userLogin;

    public UserAlreadyExistsException(String userLogin) {
        super(String.format("User with login '%s' already exists", userLogin));

        this.userLogin = userLogin;
    }

    public String getUserLogin() {
        return userLogin;
    }
}
