package ru.netology.homework_61.controller;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class FileRenameRequestBody {
    @JsonProperty("filename")
    private String newName;

    public FileRenameRequestBody() {
        this("");
    }

    public FileRenameRequestBody(String newName) {
        this.newName = newName;
    }

    public String getNewName() {
        return newName;
    }

    public void setNewName(String newName) {
        this.newName = newName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        FileRenameRequestBody that = (FileRenameRequestBody) o;
        return Objects.equals(newName, that.newName);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(newName);
    }
}
