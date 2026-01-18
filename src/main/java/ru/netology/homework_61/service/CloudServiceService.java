package ru.netology.homework_61.service;

import jakarta.transaction.Transactional;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.netology.homework_61.model.FileData;
import ru.netology.homework_61.model.User;
import ru.netology.homework_61.repository.CloudServiceRepository;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;

@Service
public class CloudServiceService {
    private final CloudServiceRepository repository;
    private final String filesDir;
    private final Random rand;
    private final PasswordEncoder passwordEncoder;

    public CloudServiceService(CloudServiceRepository repository, Environment env, PasswordEncoder encoder) {
        this.repository = repository;
        filesDir = env.getProperty("files_directory", ".");
        rand = new SecureRandom();
        passwordEncoder = encoder;
    }

    @Transactional
    public ResponseEntity<Object> registerNewUser(UserCredentials credentials) {
        if (repository.findUserByLogin(credentials.getLogin()).isPresent()) {
            return new ResponseEntity<>(
                    new ErrorResponse(String.format("User with login '%s' already exists", credentials.getLogin())),
                    HttpStatus.BAD_REQUEST
            );
        }

        var userId = repository.addUser(
                new User(0, credentials.getLogin(), passwordEncoder.encode(credentials.getPassword()))
        );

        var token = generateAccessToken(credentials);

        repository.saveUserSession(token, userId);

        return new ResponseEntity<>(
                new SuccessfulLoginResponse(token),
                HttpStatus.OK
        );
    }

    public ResponseEntity<Object> login(UserCredentials credentials) {
        var user = repository.findUserByLogin(credentials.getLogin());

        if (user.isEmpty() || !passwordEncoder.matches(credentials.getPassword(), user.get().getPasswordHash())) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        var token = generateAccessToken(credentials);

        repository.saveUserSession(token, user.get().getId());

        return new ResponseEntity<>(
                new SuccessfulLoginResponse(token),
                HttpStatus.OK
        );
    }

    public ResponseEntity<Object> logout(String authToken) {
        repository.deleteUserSession(authToken);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    public ResponseEntity<Object> uploadFile(String authToken, String fileName, MultipartFile file) throws IOException {
        var user = findUserIdByAccessToken(authToken);

        if (user.isEmpty()) {
            return new ResponseEntity<>(
                    new ErrorResponse("Unauthorized"),
                    HttpStatus.UNAUTHORIZED
            );
        }

        var existingFileDataFromDB = repository.getFileData(user.get().getId(), fileName);

        var fileExists = existingFileDataFromDB.isPresent();

        if (fileExists) {
            var existingFileData = existingFileDataFromDB.get();

            Files.delete(Path.of(filesDir, existingFileData.getLocalName()));

            repository.deleteFileData(existingFileData.getUserId(), existingFileData.getName());
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
                    new FileData(user.get().getId(), fileName, localName, hash, file.getSize())
            );
        } catch (Throwable e) {
            filePath.toFile().delete();

            throw e;
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    public ResponseEntity<Object> deleteFile(String authToken, String fileName) throws IOException {
        var user = findUserIdByAccessToken(authToken);

        if (user.isEmpty()) {
            return new ResponseEntity<>(
                    new ErrorResponse("Unauthorized"),
                    HttpStatus.UNAUTHORIZED
            );
        }

        var fileData = repository.getFileData(user.get().getId(), fileName);

        if (fileData.isEmpty()) {
            return new ResponseEntity<>(
                    new ErrorResponse(String.format("File '%s' does not exist", fileName)),
                    HttpStatus.BAD_REQUEST
            );
        }

        if (repository.deleteFileData(user.get().getId(), fileName)) {
            Files.delete(Path.of(filesDir, fileData.get().getLocalName()));

            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity<>(
                    new ErrorResponse(String.format("file '%s' not found", fileName)),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    public ResponseEntity<Object> downloadFile(String authToken, String fileName) throws Exception {
        var user = findUserIdByAccessToken(authToken);

        if (user.isEmpty()) {
            return new ResponseEntity<>(
                    new ErrorResponse("Unauthorized"),
                    HttpStatus.UNAUTHORIZED
            );
        }

        var fileDataFromDb = repository.getFileData(user.get().getId(), fileName);

        if (fileDataFromDb.isEmpty()) {
            return new ResponseEntity<>(
                    new ErrorResponse(String.format("File '%s' does not exist", fileName)),
                    HttpStatus.BAD_REQUEST
            );
        }

        var fileData = fileDataFromDb.get();

        var filePath = Path.of(filesDir, fileData.getLocalName());

//        var responseBody = new LinkedMultiValueMap<>();
//        responseBody.add("hash", fileData.getChecksum());
//        responseBody.add("file", new UrlResourceWithCustomFileName(filePath.toFile().toURI(), fileData.getName())); // Doesn't work (breaks files).
//        responseBody.add("file", Base64.getEncoder().encodeToString(Files.readAllBytes(filePath))); // Doesn't work also.

        // Works perfectly even without header CONTENT_TYPE setup, but it breaks API documentation.
        // https://github.com/netology-code/jd-homeworks/blob/cc050f58f553c82c5311d10b6df8e181eb3624c3/diploma/CloudServiceSpecification.yaml#L261C1-L261C10
        var responseBody = new UrlResourceWithSpecifiedFilename(filePath.toFile().toURI(), fileData.getName());

        return ResponseEntity.ok()
//                .header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE)
                .body(responseBody);
    }

    @Transactional
    public ResponseEntity<Object> renameFile(String authToken, String fileName, String newFileName) throws IOException {
        var userFromDb = findUserIdByAccessToken(authToken);

        if (userFromDb.isEmpty()) {
            return new ResponseEntity<>(
                    new ErrorResponse("Unauthorized"),
                    HttpStatus.UNAUTHORIZED
            );
        }

        var user = userFromDb.get();

        var fileDataFromDBByNewName = repository.getFileData(user.getId(), newFileName);

        if (fileDataFromDBByNewName.isPresent()) {
            return new ResponseEntity<>(
                    new ErrorResponse(String.format("File '%s' already exists", fileName)),
                    HttpStatus.BAD_REQUEST
            );
        }

        var fileDataFromDB = repository.getFileData(user.getId(), fileName);

        if (fileDataFromDB.isEmpty()) {
            return new ResponseEntity<>(
                    new ErrorResponse(String.format("File '%s' does not exist", fileName)),
                    HttpStatus.BAD_REQUEST
            );
        }

        var fileData = fileDataFromDB.get();

        fileData.setName(newFileName);

        repository.saveFileData(fileData);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    public ResponseEntity<Object> getAllFiles(String authToken, int limit) {
        var user = findUserIdByAccessToken(authToken);

        if (user.isEmpty()) {
            return new ResponseEntity<>(
                    new ErrorResponse("Unauthorized"),
                    HttpStatus.UNAUTHORIZED
            );
        }

        var list = repository.listFilesByUser(user.get().getId(), limit);

        var res = new ArrayList<FilesListResponseElement>(list.size());

        for (var element : list) {
            res.add(
                    new FilesListResponseElement(
                            element.getName(),
                            element.getSize()
                    )
            );
        }

        return new ResponseEntity<>(res, HttpStatus.OK);
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

    private String generateAccessToken(UserCredentials userCredentials) {
        return RandomStringUtils.random(100, 0, 0, true, true, null, rand);
    }
}
