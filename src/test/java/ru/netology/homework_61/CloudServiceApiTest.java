package ru.netology.homework_61;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockPart;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import ru.netology.homework_61.controller.FileRenameRequestBody;
import ru.netology.homework_61.model.User;
import ru.netology.homework_61.repository.CloudServiceRepository;
import ru.netology.homework_61.service.FilesListResponseElement;
import ru.netology.homework_61.service.SuccessfulLoginResponse;
import ru.netology.homework_61.service.UserCredentials;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

// This test requires running configured database.

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
        properties = {
                "files_directory=src/test/resources/storage"
        }
)
public class CloudServiceApiTest {
    private static final String TEST_USER_LOGIN = "testuser";
    private static final String TEST_USER_PASSWORD = "nopasswd";
    private static final Path EXAMPLE_FILE_PATH = Path.of("src", "test", "resources", "fish.JPG");

    private final MockMvc mockMvc;
    private final CloudServiceRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper mapper;

    private String accessToken;

    @Autowired
    public CloudServiceApiTest(MockMvc mockMvc, CloudServiceRepository repository, PasswordEncoder passwordEncoder, ObjectMapper mapper) {
        this.mockMvc = mockMvc;
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
    }

    @BeforeEach
    public void login() throws Exception {
        var exitingTestUser = repository.findUserByLogin(TEST_USER_LOGIN);

        if (exitingTestUser.isEmpty()) {
            repository.addUser(new User(0, TEST_USER_LOGIN, passwordEncoder.encode(TEST_USER_PASSWORD)));
        }

        var content = mockMvc.perform(
                        post("/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        mapper.writeValueAsString(
                                                new UserCredentials(TEST_USER_LOGIN, TEST_USER_PASSWORD)
                                        )
                                )
                )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString();

        accessToken = mapper.readValue(content, SuccessfulLoginResponse.class).getAuthToken();
    }

    @AfterEach
    public void logout() throws Exception {
        if (accessToken != null) {
            mockMvc.perform(
                            post("/logout")
                                    .header("auth-token", accessToken)
                    )
                    .andExpect(MockMvcResultMatchers.status().isOk());

            accessToken = null;
        }
    }

    @Test
    public void checkAllOperationsUsingOneFile() throws Exception {
        var fileName = EXAMPLE_FILE_PATH.getFileName().toString();
        long fileSize;

        {
            var fileData = Files.readAllBytes(EXAMPLE_FILE_PATH);

            fileSize = fileData.length;

            mockMvc.perform(
                            multipart(HttpMethod.POST, "/file")
                                    .part(
                                            new MockPart("file", fileName, fileData)
                                    )
                                    .header("auth-token", accessToken)
                                    .param("filename", fileName)
                    )
                    .andExpect(MockMvcResultMatchers.status().isOk());
        }

        var newFileName = "picture.JPG";

        mockMvc.perform(
                        put("/file")
                                .header("auth-token", accessToken)
                                .param("filename", fileName)
                                .content(
                                        mapper.writeValueAsString(
                                                new FileRenameRequestBody(newFileName)
                                        )
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(MockMvcResultMatchers.status().isOk());

        var responseBody = mockMvc.perform(
                        get("/list")
                                .header("auth-token", accessToken)
                                .param("filename", newFileName)
                                .param("limit", "999")
                )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var list = mapper.readValue(responseBody, FilesListResponseElement[].class);

        Assertions.assertTrue(list.length > 0);

        var found = false;

        for (var element : list) {
            if (newFileName.equals(element.getName())) {
                found = true;

                Assertions.assertEquals(newFileName, element.getName());

                Assertions.assertEquals(fileSize, element.getSize());

                break;
            }
        }

        Assertions.assertTrue(found);

        mockMvc.perform(
                        get("/file")
                                .header("auth-token", accessToken)
                                .param("filename", newFileName)
                )
                .andExpect(MockMvcResultMatchers.status().isOk());

        mockMvc.perform(
                        delete("/file")
                                .header("auth-token", accessToken)
                                .param("filename", newFileName)
                )
                .andExpect(MockMvcResultMatchers.status().isOk());
    }
}
