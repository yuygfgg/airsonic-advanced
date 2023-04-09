package org.airsonic.player.dao;

import org.airsonic.player.config.AirsonicHomeConfig;
import org.airsonic.player.domain.Avatar;
import org.airsonic.player.domain.User;
import org.airsonic.player.domain.UserCredential;
import org.airsonic.player.domain.UserCredential.App;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@SpringBootTest
@EnableConfigurationProperties({AirsonicHomeConfig.class})
@ExtendWith(MockitoExtension.class)
public class AvatarDaoTest {

    @Autowired
    private AvatarDao avatarDao;

    @Autowired
    private UserDao userDao;

    @TempDir
    private static Path tempDir;

    @SpyBean
    private AirsonicHomeConfig airsonicConfig;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final String TEST_USER_NAME = "testUser";

    @BeforeAll
    public static void beforeAll() {
        System.setProperty("airsonic.home", tempDir.toString());
    }

    @BeforeEach
    public void beforeEach() {
        // Clear the database
        jdbcTemplate.execute("DELETE FROM users");
        jdbcTemplate.execute("DELETE FROM user_credentials");
        jdbcTemplate.execute("DELETE FROM custom_avatar");
    }

    @AfterAll
    public static void afterAll() {
        System.clearProperty("airsonic.home");
    }

    @Test
    public void testSystemAvatars() {
        // Call the method to be tested
        List<Avatar> result = avatarDao.getAllSystemAvatars();

        // Assertions
        assertEquals(46, result.size());

        // test getSystemAvatar
        Avatar expected = result.get(0);
        Avatar actual = avatarDao.getSystemAvatar(expected.getId());
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getMimeType(), actual.getMimeType());
        assertEquals(expected.getPath(), actual.getPath());
        assertEquals(expected.getHeight(), actual.getHeight());
        assertEquals(expected.getWidth(), actual.getWidth());
    }

    @Test
    public void testCustomAvatar() throws Exception {

        User user = new User(TEST_USER_NAME, "user1@example.com");
        UserCredential uc = new UserCredential(TEST_USER_NAME, TEST_USER_NAME, "secret", "noop", App.AIRSONIC);
        userDao.createUser(user, uc);
        Avatar expected = new Avatar(1, "avatar1", Instant.now(), "image/jpeg", 100, 100, "/avatars/avatar1.jpg");

        // Call the method to be tested
        avatarDao.setCustomAvatar(expected, TEST_USER_NAME);

        // Call the method to be tested
        Avatar actual = avatarDao.getCustomAvatar(TEST_USER_NAME);

        // Assertions
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getMimeType(), actual.getMimeType());
        assertEquals(expected.getPath(), actual.getPath());
        assertEquals(expected.getHeight(), actual.getHeight());
        assertEquals(expected.getWidth(), actual.getWidth());
    }

    @Test
    public void testCustomAvatarWithAirsonicHomePath() throws Exception {

        User user = new User(TEST_USER_NAME, "user1@example.com");
        UserCredential uc = new UserCredential(TEST_USER_NAME, TEST_USER_NAME, "secret", "noop", App.AIRSONIC);
        userDao.createUser(user, uc);
        Avatar expected = new Avatar(1, "avatar1", Instant.now(), "image/jpeg", 100, 100, tempDir.resolve("avatars/avatar1.jpg"));


        // Call the method to be tested
        avatarDao.setCustomAvatar(expected, TEST_USER_NAME);

        // Mock the AirsonicConfig to return a different path
        when(airsonicConfig.getAirsonicHome()).thenReturn(Paths.get("/airsonic-home"));

        // Call the method to be tested
        Avatar actual = avatarDao.getCustomAvatar(TEST_USER_NAME);

        // Assertions
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getMimeType(), actual.getMimeType());
        assertEquals(Paths.get("/airsonic-home/avatars/avatar1.jpg"), actual.getPath());
        assertEquals(expected.getHeight(), actual.getHeight());
        assertEquals(expected.getWidth(), actual.getWidth());
    }

}
