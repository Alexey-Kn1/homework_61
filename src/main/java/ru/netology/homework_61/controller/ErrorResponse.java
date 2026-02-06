package ru.netology.homework_61.controller;

import java.util.Objects;

public class ErrorResponse {
    private String message;

    public ErrorResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ErrorResponse that = (ErrorResponse) o;
        return Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(message);
    }
}
