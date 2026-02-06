package ru.netology.homework_61.service;

public class FileAlreadyExistsException extends CloudServiceException {
    private final String fileName;

    public FileAlreadyExistsException(String fileName) {
        super(String.format("File '%s' already exists", fileName));

        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}
