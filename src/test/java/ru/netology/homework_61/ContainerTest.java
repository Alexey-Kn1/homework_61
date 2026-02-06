package ru.netology.homework_61;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.netology.homework_61.controller.SuccessfulLoginResponse;
import ru.netology.homework_61.controller.UserCredentials;

// Doesn't work yet: application can't connect database.

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = Main.class)
public class ContainerTest {
    private static final String TEST_USER_LOGIN = "testuser";
    private static final String TEST_USER_PASSWORD = "passphrase";

    @Autowired
    private TestRestTemplate restTemplate;

    private static final Network network = Network.newNetwork();

    @Container
    private static GenericContainer<?> postgres = new GenericContainer<>("postgres:latest")
            .withNetwork(network)
            .withNetworkAliases("postgres")
            .withFileSystemBind("./data/postgres", "/var/lib/postgresql")
            .withExposedPorts(5432);

    @Container
    private static GenericContainer<?> appContainer = new GenericContainer<>("app")
            .dependsOn(postgres)
            .withNetwork(network)
            .withExposedPorts(8081);

    @BeforeAll
    public static void startContainer() throws Exception {
        postgres.start();
        appContainer.addEnv("DB_ADDRESS", "jdbc:postgresql://postgres:" + postgres.getMappedPort(5432) + "/homework_61");
        appContainer.start();
    }

    @Test
    public void checkApi() {
        var addr = "http://" + appContainer.getHost() + ":" + appContainer.getMappedPort(8081);

        var loginResponse = restTemplate.postForEntity(
                addr + "/login",
                new UserCredentials(TEST_USER_LOGIN, TEST_USER_PASSWORD),
                SuccessfulLoginResponse.class
        );

        Assertions.assertEquals(HttpStatus.OK, loginResponse.getStatusCode());

        var accessToken = loginResponse.getBody().getAuthToken();

        // TODO: check API.

        var headers = new HttpHeaders();

        headers.add("auth-token", accessToken);

        var logoutResponse = restTemplate.exchange(
                addr + "/logout",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                String.class
        );

        Assertions.assertEquals(HttpStatus.OK, logoutResponse.getStatusCode());
    }
}
