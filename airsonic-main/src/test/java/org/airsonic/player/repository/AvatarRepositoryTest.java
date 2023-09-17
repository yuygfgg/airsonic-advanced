package org.airsonic.player.repository;

import org.airsonic.player.config.AirsonicHomeConfig;
import org.airsonic.player.domain.User;
import org.airsonic.player.domain.UserCredential;
import org.airsonic.player.domain.UserCredential.App;
import org.airsonic.player.domain.entity.CustomAvatar;
import org.airsonic.player.domain.entity.SystemAvatar;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@EnableConfigurationProperties({AirsonicHomeConfig.class})
@ExtendWith(MockitoExtension.class)
@Transactional
public class AvatarRepositoryTest {

    @Autowired
    private SystemAvatarRepository systemAvatarRepository;

    @Autowired
    private CustomAvatarRepository customAvatarRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserCredentialRepository userCredentialRepository;

    @TempDir
    private static Path tempDir;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final String TEST_USER_NAME = "avatarDaoTestUser";

    @BeforeAll
    public static void beforeAll() {
        System.setProperty("airsonic.home", tempDir.toString());
    }

    @BeforeEach
    public void beforeEach() {
        // Clear the database
        jdbcTemplate.execute("DELETE FROM custom_avatar");
    }

    @AfterAll
    public static void afterAll() {
        System.clearProperty("airsonic.home");
    }

    @Test
    public void testSystemAvatars() {
        // Call the method to be tested
        List<SystemAvatar> result = systemAvatarRepository.findAll();

        // Assertions
        assertEquals(46, result.size());

        // test getSystemAvatar
        SystemAvatar expected = result.get(0);
        SystemAvatar actual = systemAvatarRepository.findById(expected.getId()).get();
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getMimeType(), actual.getMimeType());
        assertEquals(expected.getPath(), actual.getPath());
        assertEquals(expected.getHeight(), actual.getHeight());
        assertEquals(expected.getWidth(), actual.getWidth());
    }

    @Nested
    public class TestCustomAvatar {


        @BeforeEach
        public void beforeEach() {
            User user = new User(TEST_USER_NAME, "avatar_dao_test@example.com");
            UserCredential uc = new UserCredential(user, TEST_USER_NAME, "secret", "noop", App.AIRSONIC);
            userRepository.save(user);
            userCredentialRepository.save(uc);
        }

        @AfterEach
        public void afterEach() {
            userRepository.deleteById(TEST_USER_NAME);
        }

        @Test
        public void testCustomAvatar() throws Exception {

            CustomAvatar expected = new CustomAvatar("avatar1", Instant.now(), "image/jpeg", 100, 100, Paths.get("$[AIRSONIC_HOME]/avatars/avatar1.jpg"), TEST_USER_NAME);

            // Call the method to be tested
            customAvatarRepository.save(expected);

            // Call the method to be tested
            CustomAvatar actual = customAvatarRepository.findByUsername(TEST_USER_NAME).get();

            // Assertions
            assertEquals(expected.getName(), actual.getName());
            assertEquals(expected.getMimeType(), actual.getMimeType());
            assertEquals(expected.getPath(), actual.getPath());
            assertEquals(expected.getHeight(), actual.getHeight());
            assertEquals(expected.getWidth(), actual.getWidth());
        }

        @Test
        public void testDeleteAllByUsername() {

            CustomAvatar expected = new CustomAvatar("avatar1", Instant.now(), "image/jpeg", 100, 100, Paths.get("$[AIRSONIC_HOME]/avatars/avatar1.jpg"), TEST_USER_NAME);

            // Call the method to be tested
            customAvatarRepository.save(expected);

            // Call the method to be tested
            customAvatarRepository.deleteAllByUsername(TEST_USER_NAME);

            // Call the method to be tested
            Optional<CustomAvatar> actual = customAvatarRepository.findByUsername(TEST_USER_NAME);

            // Assertions
            assertTrue(actual.isEmpty());

        }
    }
}
