package ru.netology.homework_61.service;

public class AuthorizationException extends CloudServiceException {
    public AuthorizationException() {
        super("Unauthorized");
    }
}
