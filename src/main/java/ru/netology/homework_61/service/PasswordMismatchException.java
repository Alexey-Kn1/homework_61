package ru.netology.homework_61.service;

public class PasswordMismatchException extends CloudServiceException {
    public PasswordMismatchException() {
        super("Password doesn't match");
    }
}
