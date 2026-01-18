package ru.netology.homework_61.service;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class SuccessfulLoginResponse {
    @JsonProperty("auth-token")
    private String authToken;

    public SuccessfulLoginResponse(String authToken) {
        this.authToken = authToken;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SuccessfulLoginResponse that = (SuccessfulLoginResponse) o;
        return Objects.equals(authToken, that.authToken);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(authToken);
    }
}
