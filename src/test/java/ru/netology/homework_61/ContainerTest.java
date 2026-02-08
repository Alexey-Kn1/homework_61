package ru.netology.homework_61;

import org.junit.ClassRule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockPart;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
import ru.netology.homework_61.controller.FileRenameRequestBody;
import ru.netology.homework_61.controller.FilesListResponseElement;
import ru.netology.homework_61.controller.SuccessfulLoginResponse;
import ru.netology.homework_61.controller.UserCredentials;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ContextConfiguration(initializers = {ContainerTest.Initializer.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ContainerTest {
    private static final String FILES_STORAGE_DIR_PATH = Path.of(".", "src", "test", "resources", "files").toString();
    private static final Path DATA_DIR_PATH = Path.of(".", "src", "test", "resources");
    private static final String FILE_NAME = "fish.JPG";
    private static final String CHANGED_FILE_NAME = "sea.JPG";

    private static final Path EXAMPLE_FILE_PATH = Path.of(DATA_DIR_PATH.toString(), FILE_NAME);

    @ClassRule
    public static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:latest")
            .withDatabaseName("homework_61")
            .withUsername("app")
            .withPassword("nopasswd");

    private final MockMvc api;
    private final ObjectMapper serializer;

    @Autowired
    public ContainerTest(MockMvc api, ObjectMapper serializer) {
        this.api = api;
        this.serializer = serializer;
    }

    @BeforeAll
    public static void startContainer() {
        postgreSQLContainer.start();
    }

    @Test
    public void apiTest() throws Exception {
        api.perform(
                        post("/registration")
                                .content(
                                        serializer.writeValueAsString(
                                                new UserCredentials("user", "passphrase")
                                        )
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());

        var content = api.perform(
                        post("/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        serializer.writeValueAsString(
                                                new UserCredentials("user", "passphrase")
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var accessToken = serializer.readValue(content, SuccessfulLoginResponse.class).getAuthToken();

        var fileName = EXAMPLE_FILE_PATH.getFileName().toString();
        long fileSize;

        {
            var fileData = Files.readAllBytes(EXAMPLE_FILE_PATH);

            fileSize = fileData.length;

            api.perform(
                            multipart(HttpMethod.POST, "/file")
                                    .part(
                                            new MockPart("file", fileName, fileData)
                                    )
                                    .header("auth-token", accessToken)
                                    .param("filename", fileName)
                    )
                    .andExpect(status().isOk());
        }

        api.perform(
                        put("/file")
                                .header("auth-token", accessToken)
                                .param("filename", fileName)
                                .content(
                                        serializer.writeValueAsString(
                                                new FileRenameRequestBody(CHANGED_FILE_NAME)
                                        )
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());

        var responseBody = api.perform(
                        get("/list")
                                .header("auth-token", accessToken)
                                .param("filename", CHANGED_FILE_NAME)
                                .param("limit", "999")
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var list = serializer.readValue(responseBody, FilesListResponseElement[].class);

        Assertions.assertTrue(list.length > 0);

        var found = false;

        for (var element : list) {
            if (CHANGED_FILE_NAME.equals(element.getName())) {
                found = true;

                Assertions.assertEquals(CHANGED_FILE_NAME, element.getName());

                Assertions.assertEquals(fileSize, element.getSize());

                break;
            }
        }

        Assertions.assertTrue(found);

        api.perform(
                        get("/file")
                                .header("auth-token", accessToken)
                                .param("filename", CHANGED_FILE_NAME)
                )
                .andExpect(status().isOk());

        api.perform(
                        delete("/file")
                                .header("auth-token", accessToken)
                                .param("filename", CHANGED_FILE_NAME)
                )
                .andExpect(status().isOk());

        api.perform(
                        post("/logout")
                                .header("auth-token", accessToken)
                )
                .andExpect(status().isOk());
    }

    @BeforeAll
    @AfterAll
    public static void cleanFiles() throws Exception {
        FileUtils.cleanDirectory(new File(FILES_STORAGE_DIR_PATH));
    }

    static class Initializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues.of(
                    "spring.datasource.url=" + postgreSQLContainer.getJdbcUrl(),
                    "spring.datasource.username=" + postgreSQLContainer.getUsername(),
                    "spring.datasource.password=" + postgreSQLContainer.getPassword(),
                    "files_directory=" + FILES_STORAGE_DIR_PATH
            ).applyTo(configurableApplicationContext.getEnvironment());
        }
    }
}
