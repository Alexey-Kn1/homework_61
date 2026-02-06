package ru.netology.homework_61;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import ru.netology.homework_61.model.FileData;
import ru.netology.homework_61.model.User;
import ru.netology.homework_61.repository.CloudServiceRepository;
import ru.netology.homework_61.service.CloudServiceService;
import ru.netology.homework_61.controller.ErrorResponse;
import ru.netology.homework_61.controller.SuccessfulLoginResponse;
import ru.netology.homework_61.controller.UserCredentials;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@TestPropertySource(
        properties = {
                "files_directory=src/test/resources/storage"
        }
)
@SpringBootTest
public class SeparatedServiceTest {
    private static final Path EXAMPLE_FILE_PATH = Path.of("src", "test", "resources", "fish.JPG");

    private final Environment env;
    private final PasswordEncoder encoder;

    @Autowired
    public SeparatedServiceTest(Environment env, PasswordEncoder encoder) {
        this.env = env;
        this.encoder = encoder;
    }

    @Test
    public void checkFilesManipulations() throws Exception {
        var repositoryMock = Mockito.mock(CloudServiceRepository.class);

        var service = new CloudServiceService(repositoryMock, env, encoder);

        Mockito.when(repositoryMock.findUserIdByAccessToken(Mockito.anyString()))
                .thenReturn(Optional.empty());

        Mockito.when(repositoryMock.findUserIdByAccessToken("token"))
                .thenReturn(
                        Optional.of(
                                new User(1, "login", encoder.encode("passphrase"))
                        )
                );

        Mockito.when(repositoryMock.findUserIdByAccessToken("token1"))
                .thenReturn(
                        Optional.of(
                                new User(2, "login1", encoder.encode("passphrase1"))
                        )
                );

        Mockito.when(repositoryMock.deleteFileData(Mockito.anyLong(), Mockito.anyString()))
                .thenReturn(false);

        Mockito.when(repositoryMock.getFileData(Mockito.anyLong(), Mockito.anyString()))
                .thenReturn(Optional.empty());

        var fileName = EXAMPLE_FILE_PATH.getFileName().toString();

        ResponseEntity<Object> fileUploadResponse;

        try (var file = new FileInputStream(EXAMPLE_FILE_PATH.toFile())) {
            fileUploadResponse = service.uploadFile(
                    "wrong_token",
                    fileName,
                    new MockMultipartFile(fileName, file)
            );
        }

        Assertions.assertNotEquals(HttpStatus.OK, fileUploadResponse.getStatusCode());

        Assertions.assertInstanceOf(ErrorResponse.class, fileUploadResponse.getBody());

        try (var file = new FileInputStream(EXAMPLE_FILE_PATH.toFile())) {
            fileUploadResponse = service.uploadFile(
                    "token",
                    fileName,
                    new MockMultipartFile(fileName, file)
            );
        }

        Assertions.assertEquals(HttpStatus.OK, fileUploadResponse.getStatusCode());

        Assertions.assertNull(fileUploadResponse.getBody());

        var argc = ArgumentCaptor.forClass(FileData.class);

        Mockito.verify(repositoryMock)
                .saveFileData(argc.capture());

        var storedFileData = argc.getValue();

        Mockito.when(repositoryMock.getFileData(1, fileName))
                .thenReturn(Optional.of(storedFileData));

        Mockito.when(repositoryMock.deleteFileData(1, fileName))
                .thenReturn(true);

        var responseOfDownloadAttemptByWrongUser = service.downloadFile("token1", fileName);

        Assertions.assertNotEquals(HttpStatus.OK, responseOfDownloadAttemptByWrongUser.getStatusCode());

        Assertions.assertInstanceOf(ErrorResponse.class, responseOfDownloadAttemptByWrongUser.getBody());

        responseOfDownloadAttemptByWrongUser = service.downloadFile("wrong_token", fileName);

        Assertions.assertNotEquals(HttpStatus.OK, responseOfDownloadAttemptByWrongUser.getStatusCode());

        Assertions.assertInstanceOf(ErrorResponse.class, responseOfDownloadAttemptByWrongUser.getBody());

        var successfulDownloadResponse = service.downloadFile("token", fileName);

        Assertions.assertEquals(HttpStatus.OK, successfulDownloadResponse.getStatusCode());

        var successfulFileDownloadResponseBody = (UrlResource) successfulDownloadResponse.getBody();

        Assertions.assertTrue(successfulFileDownloadResponseBody.isFile());

        Assertions.assertTrue(successfulFileDownloadResponseBody.getFile().exists());

        Assertions.assertEquals(fileName, successfulFileDownloadResponseBody.getFilename());

        Assertions.assertEquals(-1, Files.mismatch(EXAMPLE_FILE_PATH, successfulFileDownloadResponseBody.getFile().toPath()));

        var deleteResponseByWrongUser = service.deleteFile("token1", fileName);

        Assertions.assertNotEquals(HttpStatus.OK, deleteResponseByWrongUser.getStatusCode());

        deleteResponseByWrongUser = service.deleteFile("wrong_token", fileName);

        Assertions.assertNotEquals(HttpStatus.OK, deleteResponseByWrongUser.getStatusCode());

        Assertions.assertTrue(successfulFileDownloadResponseBody.getFile().exists());

        var deleteResponse = service.deleteFile("token", fileName);

        Assertions.assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());

        Assertions.assertFalse(successfulFileDownloadResponseBody.getFile().exists());

        Mockito.verify(repositoryMock, Mockito.times(1))
                .deleteFileData(1, fileName);
    }

    @Test
    public void checkAuthentification() throws Exception {
        var repositoryMock = Mockito.mock(CloudServiceRepository.class);

        var service = new CloudServiceService(repositoryMock, env, encoder);

        Mockito.when(repositoryMock.findUserByLogin(Mockito.anyString()))
                .thenReturn(Optional.empty());

        Mockito.when(repositoryMock.findUserByLogin("login"))
                .thenReturn(
                        Optional.of(
                                new User(1, "login", encoder.encode("passphrase"))
                        )
                );

        var successfulLoginResponse = service.login(
                new UserCredentials("login", "passphrase")
        );

        Assertions.assertEquals(HttpStatus.OK, successfulLoginResponse.getStatusCode());

        Assertions.assertInstanceOf(SuccessfulLoginResponse.class, successfulLoginResponse.getBody());

        var failedLoginResponse = service.login(
                new UserCredentials("login", "wrong_passphrase")
        );

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, failedLoginResponse.getStatusCode());

        var authToken = ((SuccessfulLoginResponse) successfulLoginResponse.getBody()).getAuthToken();

        var successfulLogoutResponse = service.logout(authToken);

        Assertions.assertEquals(HttpStatus.OK, successfulLogoutResponse.getStatusCode());

        Mockito.verify(repositoryMock, Mockito.times(1))
                .saveUserSession(Mockito.eq(authToken), Mockito.eq((long) 1));

        Mockito.verify(repositoryMock, Mockito.times(2))
                .findUserByLogin(Mockito.eq("login"));

        Mockito.verify(repositoryMock, Mockito.times(1))
                .deleteUserSession(Mockito.eq(authToken));

        Mockito.verify(repositoryMock, Mockito.times(1))
                .deleteUserSession(Mockito.eq(authToken));
    }
}
