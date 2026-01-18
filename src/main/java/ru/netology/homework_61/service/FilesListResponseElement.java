package ru.netology.homework_61.service;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class FilesListResponseElement {
    @JsonProperty("filename")
    private String name;
    private long size;

    public FilesListResponseElement() {
        this("", 0);
    }

    public FilesListResponseElement(String name, long size) {
        this.name = name;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        FilesListResponseElement that = (FilesListResponseElement) o;
        return size == that.size && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, size);
    }
}
