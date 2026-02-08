package ru.netology.homework_61;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
import ru.netology.homework_61.model.FileData;
import ru.netology.homework_61.model.User;
import ru.netology.homework_61.repository.CloudServiceRepository;
import ru.netology.homework_61.service.FileNotFoundException;
import ru.netology.homework_61.service.FilesService;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class FilesOperationsTest {
    private static final String FILES_DIR_PATH = Path.of(".", "src", "test", "resources", "files").toString();
    private static final Path DATA_DIR_PATH = Path.of(".", "src", "test", "resources");
    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder();
    private static final String TOKEN = RandomStringUtils.random(100, 0, 0, true, true, null, new Random());
    private static final User USER = new User(1, "user", ENCODER.encode("passphrase"));
    private static final String FILE_NAME = "fish.JPG";
    private static final String CHANGED_FILE_NAME = "sea.JPG";

    @Test
    public void checkFilesOperations() throws Exception {
        var repoMock = Mockito.mock(CloudServiceRepository.class);

        Mockito.when(repoMock.findUserIdByAccessToken(Mockito.anyString()))
                .thenReturn(Optional.empty());

        Mockito.when(repoMock.findUserIdByAccessToken(Mockito.eq(TOKEN)))
                .thenReturn(Optional.of(USER));

        Mockito.when(repoMock.getFileData(Mockito.any(), Mockito.anyString()))
                .thenReturn(Optional.empty());

        var envMock = Mockito.mock(Environment.class);

        Mockito.when(envMock.getProperty(Mockito.eq("files_directory"), Mockito.anyString()))
                .thenReturn(FILES_DIR_PATH);

        Mockito.when(envMock.getProperty(Mockito.eq("files_directory")))
                .thenReturn(FILES_DIR_PATH);

        var service = new FilesService(repoMock, envMock);

        service.uploadFile(
                TOKEN,
                FILE_NAME,
                new MockMultipartFile(
                        FILE_NAME,
                        Files.readAllBytes(
                                Path.of(
                                        DATA_DIR_PATH.toString(),
                                        FILE_NAME
                                )
                        )
                )
        );

        var argc = ArgumentCaptor.forClass(FileData.class);

        Mockito.verify(repoMock)
                .saveFileData(argc.capture());

        var storedFileData = argc.getValue();

        Mockito.when(repoMock.getFileData(Mockito.eq(USER), Mockito.eq(FILE_NAME)))
                .thenReturn(Optional.of(storedFileData));

        Mockito.when(repoMock.listFilesByUser(Mockito.eq(USER), Mockito.anyInt()))
                .thenReturn(List.of(storedFileData));

        Mockito.clearInvocations(repoMock); // Without this further Mockito.verify(repoMock) call fails.

        service.renameFile(TOKEN, FILE_NAME, CHANGED_FILE_NAME);

        argc = ArgumentCaptor.forClass(FileData.class);

        Mockito.verify(repoMock)
                .saveFileData(argc.capture());

        storedFileData = argc.getValue();

        Mockito.when(repoMock.getFileData(Mockito.eq(USER), Mockito.eq(FILE_NAME)))
                .thenReturn(Optional.empty());

        Mockito.when(repoMock.getFileData(Mockito.eq(USER), Mockito.eq(CHANGED_FILE_NAME)))
                .thenReturn(Optional.of(storedFileData));

        Mockito.when(repoMock.listFilesByUser(Mockito.eq(USER), Mockito.anyInt()))
                .thenReturn(List.of(storedFileData));

        Assertions.assertThrows(
                FileNotFoundException.class,
                () -> service.downloadFile(TOKEN, FILE_NAME)
        );

        var fileURL = service.downloadFile(TOKEN, CHANGED_FILE_NAME);

        Assertions.assertEquals(
                -1,
                Files.mismatch(
                        Path.of(
                                DATA_DIR_PATH.toString(),
                                FILE_NAME
                        ),
                        fileURL.getFile().toPath()
                )
        );

        var list = service.getAllFiles(TOKEN, 50);

        Assertions.assertEquals(List.of(storedFileData), list);

        Mockito.when(repoMock.deleteFileData(Mockito.any(), Mockito.anyString()))
                .thenReturn(false);

        Mockito.when(repoMock.deleteFileData(Mockito.eq(USER), Mockito.eq(CHANGED_FILE_NAME)))
                .thenReturn(true);

        service.deleteFile(TOKEN, CHANGED_FILE_NAME);

        Mockito.verify(repoMock, Mockito.times(1))
                .deleteFileData(Mockito.eq(USER), Mockito.eq(CHANGED_FILE_NAME));
    }

    @BeforeAll
    @AfterAll
    public static void cleanFiles() throws Exception {
        FileUtils.cleanDirectory(new File(FILES_DIR_PATH));
    }
}
