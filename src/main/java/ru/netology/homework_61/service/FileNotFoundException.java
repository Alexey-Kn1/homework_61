package ru.netology.homework_61.service;

public class FileNotFoundException extends CloudServiceException {
    private final String fileName;

    public FileNotFoundException(String fileName) {
        super(String.format("File '%s' not found", fileName));

        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}
