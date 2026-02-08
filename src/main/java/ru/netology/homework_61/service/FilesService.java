package ru.netology.homework_61.service;

import jakarta.transaction.Transactional;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.core.env.Environment;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.netology.homework_61.model.FileData;
import ru.netology.homework_61.model.User;
import ru.netology.homework_61.repository.CloudServiceRepository;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class FilesService {
    private final CloudServiceRepository repository;
    private final String filesDir;
    private final Random rand;

    public FilesService(CloudServiceRepository repository, Environment env) {
        this.repository = repository;
        filesDir = env.getProperty("files_directory", ".");
        rand = new Random();
    }

    public void uploadFile(String authToken, String fileName, MultipartFile file) throws IOException, AuthorizationException {
        var user = findUserIdByAccessToken(authToken);

        if (user.isEmpty()) {
            throw new AuthorizationException();
        }

        var existingFileDataFromDB = repository.getFileData(user.get(), fileName);

        var fileExists = existingFileDataFromDB.isPresent();

        if (fileExists) {
            var existingFileData = existingFileDataFromDB.get();

            Files.delete(Path.of(filesDir, existingFileData.getLocalName()));

            repository.deleteFileData(existingFileData.getUser(), existingFileData.getName());
        }

        String extension = null;

        var extensionOfGivenName = extractExtension(fileName);

        if (extensionOfGivenName.isPresent()) {
            extension = extensionOfGivenName.get();
        }

        var localName = generateLocalFileName(extension);

        var filePath = Path.of(filesDir, localName);

        String hash;

        try (
                var localFile = new FileOutputStream(filePath.toFile());
                var receivedFile = file.getInputStream()
        ) {
            hash = calculateChecksumAndCopy(localFile, receivedFile);
        }

        try {
            repository.saveFileData(
                    new FileData(user.get(), fileName, localName, hash, file.getSize())
            );
        } catch (Throwable e) {
            filePath.toFile().delete();

            throw e;
        }
    }

    public void deleteFile(String authToken, String fileName) throws IOException, AuthorizationException, FileNotFoundException {
        var user = findUserIdByAccessToken(authToken);

        if (user.isEmpty()) {
            throw new AuthorizationException();
        }

        var fileData = repository.getFileData(user.get(), fileName);

        if (fileData.isEmpty()) {
            throw new FileNotFoundException(fileName);
        }

        if (repository.deleteFileData(user.get(), fileName)) {
            Files.delete(Path.of(filesDir, fileData.get().getLocalName()));
        } else {
            // File was deleted by other request (erasing file in transaction is too slow).
            throw new FileNotFoundException(fileName);
        }
    }

    public UrlResource downloadFile(String authToken, String fileName) throws AuthorizationException, FileNotFoundException {
        var user = findUserIdByAccessToken(authToken);

        if (user.isEmpty()) {
            throw new AuthorizationException();
        }

        var fileDataFromDb = repository.getFileData(user.get(), fileName);

        if (fileDataFromDb.isEmpty()) {
            throw new FileNotFoundException(fileName);
        }

        var fileData = fileDataFromDb.get();

        var filePath = Path.of(filesDir, fileData.getLocalName());

        try {
            return new UrlResourceWithSpecifiedFilename(filePath.toFile().toURI(), fileData.getName());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public void renameFile(String authToken, String fileName, String newFileName) throws AuthorizationException, FileNotFoundException, FileAlreadyExistsException {
        var userFromDb = findUserIdByAccessToken(authToken);

        if (userFromDb.isEmpty()) {
            throw new AuthorizationException();
        }

        var user = userFromDb.get();

        var fileDataFromDBByNewName = repository.getFileData(user, newFileName);

        if (fileDataFromDBByNewName.isPresent()) {
            throw new FileAlreadyExistsException(fileName);
        }

        var fileDataFromDB = repository.getFileData(user, fileName);

        if (fileDataFromDB.isEmpty()) {
            throw new FileNotFoundException(fileName);
        }

        var fileData = fileDataFromDB.get();

        fileData.setName(newFileName);

        repository.saveFileData(fileData);
    }

    public List<FileData> getAllFiles(String authToken, int limit) throws AuthorizationException {
        var user = findUserIdByAccessToken(authToken);

        if (user.isEmpty()) {
            throw new AuthorizationException();
        }

        return repository.listFilesByUser(user.get(), limit);
    }

    private Optional<User> findUserIdByAccessToken(String authToken) {
        if (authToken.startsWith("Bearer ")) {
            authToken = authToken.replaceFirst("Bearer ", "");
        }

        return repository.findUserIdByAccessToken(authToken);
    }

    private static String calculateChecksumAndCopy(OutputStream dest, InputStream src) throws IOException {
        try (
                var inputStreamForCalculation = new DigestInputStream(
                        src,
                        MessageDigest.getInstance("SHA-256")
                );
        ) {
            inputStreamForCalculation.transferTo(dest);

            var hashCalcObject = inputStreamForCalculation.getMessageDigest();

            var hash = hashCalcObject.digest();

            StringBuilder res = new StringBuilder(hash.length);

            for (byte b : hash) {
                res.append(String.format("%02x", b));
            }

            return res.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static Optional<String> extractExtension(String fileName) {
        try {
            var extension = FilenameUtils.getExtension(fileName);

            return extension.isEmpty() ? Optional.empty() : Optional.of(extension);
        } catch (Throwable e) {
            return Optional.empty();
        }
    }

    private String generateLocalFileName(String extension) {
        var name = RandomStringUtils.random(100, 0, 0, true, true, null, rand);

        return extension == null || extension.isEmpty() ? name : name + "." + extension;
    }
}
