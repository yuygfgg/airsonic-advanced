package org.airsonic.player.api;

import org.airsonic.player.TestCaseUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.file.Path;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CORSTest {

    private static final String CLIENT_NAME = "airsonic";
    private static final String AIRSONIC_USER = "admin";
    private static final String AIRSONIC_PASSWORD = "admin";
    private static final String EXPECTED_FORMAT = "json";
    private static String AIRSONIC_API_VERSION;

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mvc;

    @TempDir
    private static Path tempAirsonicHome;

    @BeforeAll
    public static void beforeAll() {
        System.setProperty("airsonic.home", tempAirsonicHome.toString());
    }

    @BeforeEach
    public void setup() {
        AIRSONIC_API_VERSION = TestCaseUtils.restApiVersion();
        mvc = MockMvcBuilders
                .webAppContextSetup(wac)
                .apply(springSecurity())
                .dispatchOptions(true)
                .alwaysDo(print())
                .build();
    }

    @Test
    public void corsHeadersShouldBeAddedToSuccessResponses() throws Exception {
        mvc.perform(get("/rest/ping")
                .param("v", AIRSONIC_API_VERSION)
                .param("c", CLIENT_NAME)
                .param("u", AIRSONIC_USER)
                .param("p", AIRSONIC_PASSWORD)
                .param("f", EXPECTED_FORMAT)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subsonic-response.status").value("ok"));
    }

    @Test
    public void corsHeadersShouldBeAddedToErrorResponses() throws Exception {
        mvc.perform(get("/rest/ping")
                .header("Access-Control-Request-Method", "GET")
                .header("Origin", "https://example.com")
                .param("v", AIRSONIC_API_VERSION)
                .param("c", CLIENT_NAME)
                .param("u", AIRSONIC_USER)
                .param("p", "incorrect password")
                .param("f", EXPECTED_FORMAT)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.subsonic-response.status").value("failed"))
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    public void corsShouldNotBeEnabledForOtherPaths() throws Exception {
        mvc.perform(get("/login")
                .header("Access-Control-Request-Method", "GET")
                .header("Origin", "https://example.com"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    public void testOptionRequest() throws Exception {
        mvc.perform(options("/rest/ping")
                .header("Access-Control-Request-Method", "GET")
                .header("Origin", "https://example.com")
                .param("v", AIRSONIC_API_VERSION)
                .param("c", CLIENT_NAME)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

}
