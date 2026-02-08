package ru.netology.homework_61.model;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "files_data")
public class FileData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(name = "local_name", nullable = false)
    private String localName;

    @Column(nullable = false)
    private String checksum;

    @Column(nullable = false)
    private long size;

    public FileData() {
        this(new User(), "", "", "", 0);
    }

    public FileData(User user, String name, String localName, String checksum, long size) {
        this(0, user, name, localName, checksum, size);
    }

    public FileData(long id, User user, String name, String localName, String checksum, long size) {
        this.id = id;
        this.user = user;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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
        if (!(o instanceof FileData fileData)) return false;
        return id == fileData.id && size == fileData.size && Objects.equals(user, fileData.user) && Objects.equals(name, fileData.name) && Objects.equals(localName, fileData.localName) && Objects.equals(checksum, fileData.checksum);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, user, name, localName, checksum, size);
    }
}
