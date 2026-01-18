package ru.netology.homework_61.model;

import java.util.Objects;

public class FileData {
    private long id;
    private long userId;
    private String name;
    private String localName;
    private String checksum;
    private long size;

    public FileData() {
        this(0, "", "", "", 0);
    }

    public FileData(long userId, String name, String localName, String checksum, long size) {
        this(0, userId, name, localName, checksum, size);
    }

    public FileData(long id, long userId, String name, String localName, String checksum, long size) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.localName = localName;
        this.checksum = checksum;
        this.size = size;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocalName() {
        return localName;
    }

    public void setLocalName(String localName) {
        this.localName = localName;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checkSum) {
        this.checksum = checkSum;
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
        FileData fileData = (FileData) o;
        return id == fileData.id && userId == fileData.userId && size == fileData.size && Objects.equals(name, fileData.name) && Objects.equals(localName, fileData.localName) && Objects.equals(checksum, fileData.checksum);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, name, localName, checksum, size);
    }
}
