package org.airsonic.player.dao;

import org.airsonic.player.config.AirsonicHomeConfig;
import org.airsonic.player.domain.AvatarScheme;
import org.airsonic.player.domain.TranscodeScheme;
import org.airsonic.player.domain.User;
import org.airsonic.player.domain.User.Role;
import org.airsonic.player.domain.UserCredential;
import org.airsonic.player.domain.UserCredential.App;
import org.airsonic.player.domain.UserSettings;
import org.airsonic.player.repository.UserCredentialRepository;
import org.airsonic.player.repository.UserRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.comparator.NullSafeComparator;

import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Unit test of {@link UserDao}.
 *
 * @author Sindre Mehus
 */
@SpringBootTest
@EnableConfigurationProperties({ AirsonicHomeConfig.class })
public class UserDaoTestCase {

    @Autowired
    UserDao userDao;

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserCredentialRepository userCredentialRepository;

    @TempDir
    private static Path tempDir;


    @BeforeAll
    public static void setUp() {
        System.setProperty("airsonic.home", tempDir.toString());
    }

    @BeforeEach
    public void clear() {
        userRepository.deleteAll();
        userCredentialRepository.deleteAll();
    }

    @Test
    public void testCreateUser() {
        User user = new User("sindre", "sindre@activeobjects.no", false, 1000L, 2000L, 3000L, Set.of(Role.ADMIN, Role.COMMENT, Role.COVERART, Role.PLAYLIST, Role.PODCAST, Role.STREAM, Role.JUKEBOX, Role.SETTINGS));
        UserCredential uc = new UserCredential(user, "sindre", "secret", "noop", App.AIRSONIC);
        userRepository.save(user);
        userCredentialRepository.save(uc);

        User newUser = userRepository.findAll().get(0);
        assertThat(newUser).usingRecursiveComparison().isEqualTo(user);
        assertThat(userCredentialRepository.findByUserUsernameAndApp("sindre", App.AIRSONIC).get(0)).usingRecursiveComparison().isEqualTo(uc);
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void createTestUser(User user, UserCredential uc) {
        userRepository.save(user);
        userCredentialRepository.save(uc);
    }

    @Test
    public void testUpdateUser() {
        User user = new User("sindre", null);
        user.setRoles(Set.of(Role.ADMIN, Role.COMMENT, Role.COVERART, Role.PLAYLIST, Role.PODCAST, Role.STREAM, Role.JUKEBOX, Role.SETTINGS));
        UserCredential uc = new UserCredential(user, "sindre", "secret", "noop", App.AIRSONIC);
        userRepository.save(user);
        userCredentialRepository.save(uc);

        user.setEmail("sindre@foo.bar");
        user.setLdapAuthenticated(true);
        user.setBytesStreamed(1);
        user.setBytesDownloaded(2);
        user.setBytesUploaded(3);
        user.setRoles(Set.of(Role.DOWNLOAD, Role.UPLOAD));

        userRepository.save(user);

        assertThat(userRepository.findAll().get(0)).usingRecursiveComparison().isEqualTo(user);
        assertThat(userCredentialRepository.findByUserUsernameAndApp("sindre", App.AIRSONIC).get(0)).usingRecursiveComparison().isEqualTo(uc);
    }

    @Test
    public void testUpdateCredential() {
        User user = new User("sindre", null);
        user.setRoles(Set.of(Role.ADMIN, Role.COMMENT, Role.COVERART));
        UserCredential uc = new UserCredential(user, "sindre", "secret", "noop", App.AIRSONIC);
        userRepository.save(user);
        userCredentialRepository.save(uc);

        uc.setCredential("foo");

        userCredentialRepository.save(uc);

        assertThat(userRepository.findAll().get(0)).usingRecursiveComparison().isEqualTo(user);
        assertThat(userCredentialRepository.findByUserUsernameAndApp("sindre", App.AIRSONIC).get(0)).usingRecursiveComparison().isEqualTo(uc);
    }

    @Test
    public void testGetUserByName() {
        User user = new User("sindre", null);
        userRepository.save(user);

        assertThat(userRepository.findByUsername("sindre").get()).usingRecursiveComparison().isEqualTo(user);

        assertTrue(userRepository.findByUsername("sindre2").isEmpty());
        // assertNull("Error in getUserByName().", userRepository.findByUsername("Sindre ", true)); // depends on the collation of the DB
        assertTrue(userRepository.findByUsername("bente").isEmpty());
        assertTrue(userRepository.findByUsername("").isEmpty());
        assertTrue(userRepository.findByUsername(null).isEmpty());
    }

    @Test
    public void testDeleteUser() {
        assertEquals(0, userRepository.findAll().size());

        userRepository.save(new User("sindre", null));
        assertEquals(1, userRepository.count());

        userRepository.save(new User("bente", null));
        assertEquals(2, userRepository.count());

        userRepository.deleteById("sindre");

        assertEquals(1, userRepository.count());

        userRepository.deleteById("bente");
        assertEquals(0, userRepository.count());
    }

    @Test
    public void testGetRolesForUser() {
        User user = new User("sindre", null);
        user.setRoles(Set.of(Role.ADMIN, Role.COMMENT, Role.PODCAST, Role.STREAM, Role.SETTINGS));
        userRepository.save(user);

        Set<Role> roles = userRepository.findByUsername("sindre").get().getRoles();
        assertThat(roles).containsOnly(Role.ADMIN, Role.COMMENT, Role.PODCAST, Role.STREAM, Role.SETTINGS);
    }

    @Test
    public void testUserSettings() {
        assertNull(userDao.getUserSettings("sindre"));

        assertThatExceptionOfType(DataIntegrityViolationException.class)
                .isThrownBy(() -> updateUserSettings(new UserSettings("sindre")));
        User user = new User("sindre", null);

        userRepository.saveAndFlush(user);
        userCredentialRepository.saveAndFlush(new UserCredential(user, "sindre", "secret", "noop", App.AIRSONIC));

        assertNull(userDao.getUserSettings("sindre"));

        UserSettings settings = new UserSettings("sindre");
        userDao.updateUserSettings(settings);
        UserSettings userSettings = userDao.getUserSettings("sindre");
        assertThat(userSettings).usingComparatorForType(new NullSafeComparator<Instant>(new Comparator<Instant>() {
            // use a custom comparator to account for micro second differences
            // (Mysql only stores floats in json to a certain value)
            @Override
            public int compare(Instant o1, Instant o2) {
                if (o1.equals(o2) || Math.abs(ChronoUnit.MICROS.between(o1, o2)) <= 2) {
                    return 0;
                }
                return o1.compareTo(o2);
            }
        }, true), Instant.class)
                .usingRecursiveComparison().isEqualTo(settings);

        settings = new UserSettings("sindre");
        settings.setLocale(Locale.SIMPLIFIED_CHINESE);
        settings.setThemeId("midnight");
        settings.setBetaVersionNotificationEnabled(true);
        settings.setSongNotificationEnabled(false);
        settings.setShowSideBar(true);
        settings.getMainVisibility().setBitRateVisible(true);
        settings.getPlaylistVisibility().setYearVisible(true);
        settings.setLastFmEnabled(true);
        settings.setListenBrainzEnabled(true);
        settings.setTranscodeScheme(TranscodeScheme.MAX_192);
        settings.setShowNowPlayingEnabled(false);
        settings.setSelectedMusicFolderId(3);
        settings.setPartyModeEnabled(true);
        settings.setNowPlayingAllowed(true);
        settings.setAvatarScheme(AvatarScheme.SYSTEM);
        settings.setSystemAvatarId(1);
        settings.setChanged(Instant.ofEpochMilli(9412L));
        settings.setKeyboardShortcutsEnabled(true);
        settings.setPaginationSizeFiles(120);
        settings.setPaginationSizeFolders(9);
        settings.setPaginationSizePlayqueue(121);
        settings.setPaginationSizePlaylist(122);

        userDao.updateUserSettings(settings);
        userSettings = userDao.getUserSettings("sindre");
        assertThat(userSettings).usingRecursiveComparison().isEqualTo(settings);

        userRepository.deleteById("sindre");
        assertNull(userDao.getUserSettings("sindre"));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void updateUserSettings(UserSettings settings) {
        userDao.updateUserSettings(settings);
    }
}
