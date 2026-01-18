package ru.netology.homework_61.service;

import jakarta.annotation.Nullable;
import org.springframework.core.io.UrlResource;

import java.net.MalformedURLException;
import java.net.URI;

// UrlResource takes file name from its path. This class is needed to
// send file and specify its name without relation to its path on disc.
class UrlResourceWithSpecifiedFilename extends UrlResource {
    private String customName;

    public UrlResourceWithSpecifiedFilename(URI uri, String customName) throws MalformedURLException {
        super(uri);

        this.customName = customName;
    }

    public void setFilename(String customName) {
        this.customName = customName;
    }

    @Override
    public @Nullable String getFilename() {
        return customName == null ? super.getFilename() : customName;
    }
}
