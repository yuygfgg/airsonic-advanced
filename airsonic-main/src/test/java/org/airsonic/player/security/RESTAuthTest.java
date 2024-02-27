package org.airsonic.player.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.airsonic.player.config.AirsonicDefaultFolderConfig;
import org.airsonic.player.config.AirsonicHomeConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.file.Path;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableConfigurationProperties({AirsonicHomeConfig.class, AirsonicDefaultFolderConfig.class})
public class RESTAuthTest {

    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin";
    private static final String API_VERSION = "1.15.0";

    @TempDir
    private static Path tempDir;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeAll
    public static void beforeAll() {
        System.setProperty("airsonic.home", tempDir.toString());
    }

    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testRequestParamAuthSuccess() throws Exception {
        ResponseEntity<String> response = restTemplate.exchange("/rest/getArtists?v={v}&f={f}&c={c}&u={u}&p={p}",
                HttpMethod.GET, null, String.class, API_VERSION, "json", "test", USERNAME, PASSWORD);
        assertEquals(response.getStatusCode(), HttpStatus.OK);
        JsonNode json = objectMapper.readTree(response.getBody());
        assertEquals(json.get("subsonic-response").get("status").asText(), "ok");
    }

    @Test
    public void testRequestParamAuthFailure() throws Exception {
        ResponseEntity<String> response = restTemplate.exchange("/rest/getArtists?v={v}&f={f}&c={c}&u={u}&p={p}",
                HttpMethod.GET, null, String.class, API_VERSION, "json", "test", USERNAME, "incorrectpassword");
        assertEquals(response.getStatusCode(), HttpStatus.OK);
        JsonNode json = objectMapper.readTree(response.getBody());
        assertEquals(json.get("subsonic-response").get("error").get("code").asInt(), 40);
    }

    @Test
    public void testBasicAuthSuccess() throws Exception {
        ResponseEntity<String> response = restTemplate.exchange("/rest/getArtists?v={v}&f={f}&c={c}",
                HttpMethod.GET, new HttpEntity<>(createHeaders(USERNAME, PASSWORD)), String.class, API_VERSION, "json", "test");
        assertEquals(response.getStatusCode(), HttpStatus.OK);
        JsonNode json = objectMapper.readTree(response.getBody());
        assertEquals(json.get("subsonic-response").get("status").asText(), "ok");
    }

    @Test
    public void testBasicAuthFailure() throws Exception {
        ResponseEntity<String> response = restTemplate.exchange("/rest/getArtists?v={v}&f={f}&c={c}",
                HttpMethod.GET, new HttpEntity<>(createHeaders(USERNAME, "incorrectPassword")), String.class, API_VERSION, "json", "test");
        assertEquals(response.getStatusCode(), HttpStatus.UNAUTHORIZED);
    }

    private HttpHeaders createHeaders(String username, String password) {
        return new HttpHeaders() {
            {
                String auth = username + ":" + password;
                String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
                String authHeader = "Basic " + encodedAuth;
                set("Authorization", authHeader);
            }
        };
    }
   // @Test
   // public void testRequestParamAuthSuccess() throws Exception {
   //     mvc.perform(get("/rest/getArtists")
   //             .param("v", API_VERSION)
   //             .param("f", "json")
   //             .param("c", "test")
   //             .param("u", USERNAME)
   //             .param("p", PASSWORD)
   //             .contentType(MediaType.APPLICATION_JSON))
   //             .andExpect(status().isOk())
   //             .andExpect(jsonPath("$.subsonic-response.status").value("ok"));
   // }
   /*

    @Test
    public void testRequestParamAuthFailure() throws Exception {
        mvc.perform(get("/rest/getArtists")
                .param("v", API_VERSION)
                .param("c", "test")
                .param("f", "json")
                .param("u", USERNAME)
                .param("p", "incorrectpassword"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subsonic-response.error.code").value(40));
    }

    @Test
    public void testBasicAuthSuccess() throws Exception {
        mvc.perform(get("/rest/getArtists")
                .param("v", API_VERSION)
                .param("c", "test")
                .param("f", "json")
                .with(httpBasic(USERNAME, PASSWORD))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subsonic-response.status").value("ok"));
    }

    @Test
    public void testBasicAuthFailure() throws Exception {
        mvc.perform(get("/rest/getArtists")
                .param("v", API_VERSION)
                .param("c", "test")
                .param("f", "json")
                .with(httpBasic(USERNAME, "incorrectpassword"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
    */

}
