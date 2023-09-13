package org.airsonic.player.service;

import org.airsonic.player.command.CredentialsManagementCommand;
import org.airsonic.player.command.CredentialsManagementCommand.CredentialsCommand;
import org.airsonic.player.config.AirsonicHomeConfig;
import org.airsonic.player.domain.User;
import org.airsonic.player.domain.User.Role;
import org.airsonic.player.domain.UserCredential;
import org.airsonic.player.domain.UserCredential.App;
import org.airsonic.player.repository.UserCredentialRepository;
import org.airsonic.player.repository.UserRepository;
import org.airsonic.player.security.GlobalSecurityConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest
@EnableConfigurationProperties({ AirsonicHomeConfig.class })
@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

    @Autowired
    private SecurityService securityService;

    @SpyBean
    private UserRepository userRepository;

    @SpyBean
    private UserCredentialRepository userCredentialRepository;

    @TempDir
    private static Path tempDir;

    private User testUser;

    @Mock
    private CredentialsCommand mockedCredentialsCommand;

    @BeforeAll
    public static void setup() {
        System.setProperty("airsonic.home", tempDir.toString());
    }

    private final String TEST_USER_NAME = "testUserForSecurityServiceTest";

    @BeforeEach
    public void beforeEach() {
        testUser = new User(TEST_USER_NAME, "security@activeobjects.no", false, 1000L, 2000L, 3000L, Set.of(Role.ADMIN, Role.COMMENT, Role.COVERART, Role.PLAYLIST, Role.PODCAST, Role.STREAM, Role.JUKEBOX, Role.SETTINGS));
        userRepository.saveAndFlush(testUser);
    }

    @AfterEach
    public void afterEach() {
        userRepository.deleteById(TEST_USER_NAME);
    }

    @Test
    public void testCreateCredentialWithNonExistingUserShouldReturnFalse() {

        // when
        boolean result = securityService.createCredential("nonExistingUser", null, null);

        // then
        assertFalse(result);
        verify(userRepository).findByUsername("nonExistingUser");
        verifyNoInteractions(userCredentialRepository);
    }

    @Test
    public void testCreateUpdateWithMarkedDeletionTest() {
        // given
        CredentialsManagementCommand command = new CredentialsManagementCommand();
        command.setCredentials(List.of(mockedCredentialsCommand));
        UserCredential userCredential = new UserCredential(testUser, TEST_USER_NAME, "testPassword", "hex", App.LASTFM);
        userCredentialRepository.saveAndFlush(userCredential);
        when(mockedCredentialsCommand.getHash()).thenReturn(Integer.toString(userCredential.hashCode()));
        when(mockedCredentialsCommand.getMarkedForDeletion()).thenReturn(true);

        // when
        boolean result = securityService.updateCredentials(TEST_USER_NAME, command, "test", false);

        // then
        assertTrue(result);
        verify(userCredentialRepository).delete(userCredential);

    }

    @Test
    public void testUpdateCredentialsWithDecodableCredentialShouldUpdateEncoderAndCredential() {

        // given
        CredentialsManagementCommand command = new CredentialsManagementCommand();
        command.setCredentials(List.of(mockedCredentialsCommand));
        UserCredential userCredential = new UserCredential(testUser, TEST_USER_NAME, GlobalSecurityConfig.ENCODERS.get("encrypted-AES-GCM").encode("testPassword"), "encrypted-AES-GCM", App.LASTFM);
        userCredentialRepository.saveAndFlush(userCredential);
        when(mockedCredentialsCommand.getHash()).thenReturn(Integer.toString(userCredential.hashCode()));
        when(mockedCredentialsCommand.getMarkedForDeletion()).thenReturn(false);
        when(mockedCredentialsCommand.getEncoder()).thenReturn("hex");
        when(mockedCredentialsCommand.getExpirationInstant()).thenReturn(null);

        // when
        boolean result = securityService.updateCredentials(TEST_USER_NAME, command, "test", false);

        // then
        assertTrue(result);
        verify(userCredentialRepository).save(any(UserCredential.class));
        UserCredential actual = userCredentialRepository.findByUserUsernameAndApp(TEST_USER_NAME, App.LASTFM).get(0);
        assertEquals("hex", actual.getEncoder());
        assertEquals(GlobalSecurityConfig.ENCODERS.get("hex").encode("testPassword"), actual.getCredential());
        assertEquals("test", actual.getComment());
    }

    @Test
    public void testUpdateCredentialsWithReencodePlainTextShouldUpdateEncoderAndCredential() {

        // given
        CredentialsManagementCommand command = new CredentialsManagementCommand();
        command.setCredentials(List.of(mockedCredentialsCommand));
        UserCredential userCredential = new UserCredential(testUser, TEST_USER_NAME, "testPassword", "hex", App.LASTFM);
        userCredentialRepository.saveAndFlush(userCredential);
        when(mockedCredentialsCommand.getHash()).thenReturn(Integer.toString(userCredential.hashCode()));
        when(mockedCredentialsCommand.getMarkedForDeletion()).thenReturn(false);
        when(mockedCredentialsCommand.getEncoder()).thenReturn("hex");
        when(mockedCredentialsCommand.getExpirationInstant()).thenReturn(null);

        // when
        boolean result = securityService.updateCredentials(TEST_USER_NAME, command, "test", true);

        // then
        assertTrue(result);
        verify(userCredentialRepository).save(any(UserCredential.class));
        UserCredential actual = userCredentialRepository.findByUserUsernameAndApp(TEST_USER_NAME, App.LASTFM).get(0);
        assertEquals("hex", actual.getEncoder());
        assertEquals(GlobalSecurityConfig.ENCODERS.get("hex").encode("testPassword"), actual.getCredential());
        assertEquals("test", actual.getComment());
    }
}
