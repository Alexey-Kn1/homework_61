package ru.netology.homework_61.service;

public class UserNotFoundException extends CloudServiceException {
    private final String userLogin;

    public UserNotFoundException(String userLogin) {
        super(String.format("User not found bu login '%s'", userLogin));

        this.userLogin = userLogin;
    }

    public String getUserLogin() {
        return userLogin;
    }
}
