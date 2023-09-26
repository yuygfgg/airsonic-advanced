package org.airsonic.player.service;

import org.airsonic.player.command.MusicFolderSettingsCommand.MusicFolderInfo;
import org.airsonic.player.config.AirsonicHomeConfig;
import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.dao.MusicFolderDao;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.MusicFolder.Type;
import org.airsonic.player.domain.User;
import org.airsonic.player.repository.MusicFolderRepository;
import org.airsonic.player.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.transaction.Transactional;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@EnableConfigurationProperties({ AirsonicHomeConfig.class })
@Transactional
public class MediaFolderServiceTest {

    @Autowired
    private MediaFolderService mediaFolderService;

    @SpyBean
    private MusicFolderRepository musicFolderRepository;

    @SpyBean
    private UserRepository userRepository;

    @MockBean
    private MusicFolderDao musicFolderDao;

    @SpyBean
    private MediaFileDao mediaFileDao;

    @TempDir
    private static Path tempAirsonicHome;

    @TempDir
    private static Path tempDefaultMusicFolder;

    @TempDir
    private static Path tempDefaultPodcastFolder;

    @TempDir
    private Path tempMusicFolder;

    @TempDir
    private Path tempMusicFolder2;

    private final String TEST_USER_NAME = "testUserForMediaFolder";

    @BeforeAll
    public static void setup() {
        System.setProperty("airsonic.home", tempAirsonicHome.toString());
        System.setProperty("airsonic.defaultMusicFolder", tempDefaultMusicFolder.toString());
        System.setProperty("airsonic.defaultPodcastFolder", tempDefaultPodcastFolder.toString());
    }

    @BeforeEach
    public void beforeEach() {
        mediaFolderService.clearMusicFolderCache();
        User user = new User(TEST_USER_NAME, "me@exampl.com");
        userRepository.save(user);
    }

    @AfterEach
    public void afterEach() {
        userRepository.deleteById(TEST_USER_NAME);
    }

    @Test
    public void testGetAllMusicFolders() {

        // given
        long count = musicFolderRepository.count();
        Mockito.reset(musicFolderRepository);

        // when
        List<MusicFolder> musicFolders = mediaFolderService.getAllMusicFolders(true, true);

        // then
        assertEquals(count, musicFolders.size());
        verify(musicFolderRepository).findByDeleted(false);

        // caching test
        mediaFolderService.getAllMusicFolders();
        verify(musicFolderRepository).findByDeleted(false);
    }

    @Test
    public void testGetMusicFoldersForUser() {
        // given
        Mockito.reset(musicFolderRepository);
        User user = userRepository.findById(TEST_USER_NAME).get();
        MusicFolder musicFolder = new MusicFolder(tempMusicFolder, "testMusicFolder", Type.MEDIA, true, Instant.now());
        musicFolderRepository.save(musicFolder);
        user.addMusicFolder(musicFolder);
        userRepository.save(user);

        // when
        List<MusicFolder> musicFolders = mediaFolderService.getMusicFoldersForUser(TEST_USER_NAME);

        // then
        assertEquals(1, musicFolders.size());
        assertEquals(musicFolder, musicFolders.get(0));
        verify(userRepository).findByUsername(TEST_USER_NAME);

        // caching test
        mediaFolderService.getMusicFoldersForUser(TEST_USER_NAME);
        verify(userRepository).findByUsername(TEST_USER_NAME);
    }

    @Test
    public void testSetMusicFoldersForUser() {
        // given
        Mockito.reset(musicFolderRepository);
        User user = userRepository.findById(TEST_USER_NAME).get();
        MusicFolder musicFolder = new MusicFolder(tempMusicFolder, "testMusicFolder", Type.MEDIA, true, Instant.now());
        musicFolderRepository.save(musicFolder);
        MusicFolder musicFolder2 = new MusicFolder(tempMusicFolder.resolve("test2"), "testMusicFolder2", Type.MEDIA, true, Instant.now());
        musicFolderRepository.save(musicFolder2);

        // when
        mediaFolderService.setMusicFoldersForUser(TEST_USER_NAME, List.of(musicFolder.getId(), musicFolder2.getId()));

        // then
        user = userRepository.findById(TEST_USER_NAME).get();
        assertEquals(2, user.getMusicFolders().size());
        assertEquals(musicFolder, user.getMusicFolders().get(0));
        assertEquals(musicFolder2, user.getMusicFolders().get(1));
        verify(userRepository).findByUsername(TEST_USER_NAME);

    }

    @Test
    public void testCreateWithSamePath() {
        // given
        MusicFolder existingFolder = new MusicFolder(tempMusicFolder, "Existing Folder", MusicFolder.Type.MEDIA, true, Instant.now());
        musicFolderRepository.save(existingFolder);

        // when and then
        MusicFolder newFolder = new MusicFolder(tempMusicFolder, "New Folder", MusicFolder.Type.MEDIA, true, Instant.now());
        assertThrows(IllegalArgumentException.class, () -> {
            mediaFolderService.createMusicFolder(newFolder);
        });
    }

    @Test
    public void testCreateWithAncestorPath() {
        // given
        MusicFolder existingFolder = new MusicFolder(tempMusicFolder, "Existing Folder", MusicFolder.Type.MEDIA, true, Instant.now());
        musicFolderRepository.save(existingFolder);

        // when
        MusicFolder newFolder = new MusicFolder(tempMusicFolder.resolve("child"), "New Folder", MusicFolder.Type.MEDIA, true, Instant.now());
        mediaFolderService.createMusicFolder(newFolder);

        // then
        verify(musicFolderRepository).saveAndFlush(newFolder);
        verify(musicFolderDao).reassignChildren(existingFolder, newFolder);
        assertTrue(musicFolderRepository.findAll().contains(newFolder));
        assertTrue(musicFolderRepository.findAll().contains(existingFolder));

    }

    @Test
    public void testCreateWithDeletedDecendantPath() {
        // given
        MusicFolder existingFolder = new MusicFolder(tempMusicFolder.resolve("child"), "Existing Folder", MusicFolder.Type.MEDIA, true, Instant.now());
        existingFolder.setDeleted(true);
        musicFolderRepository.save(existingFolder);

        // when
        MusicFolder newFolder = new MusicFolder(tempMusicFolder, "New Folder", MusicFolder.Type.MEDIA, true, Instant.now());
        mediaFolderService.createMusicFolder(newFolder);

        // then
        verify(musicFolderRepository).saveAndFlush(newFolder);
        verify(musicFolderDao).reassignChildren(existingFolder, newFolder);
        verify(musicFolderRepository).delete(existingFolder);
        assertTrue(musicFolderRepository.findAll().contains(newFolder));
        assertFalse(musicFolderRepository.findAll().contains(existingFolder));
    }

    @Test
    public void testCreateWithNonDeletedDecendantPath() {
        // given
        MusicFolder existingFolder = new MusicFolder(tempMusicFolder.resolve("child"), "Existing Folder", MusicFolder.Type.MEDIA, true, Instant.now());
        musicFolderRepository.save(existingFolder);

        // when
        MusicFolder newFolder = new MusicFolder(tempMusicFolder, "New Folder", MusicFolder.Type.MEDIA, true, Instant.now());
        mediaFolderService.createMusicFolder(newFolder);

        // then
        verify(musicFolderRepository).saveAndFlush(newFolder);
        verify(musicFolderDao, never()).reassignChildren(existingFolder, newFolder);
        verify(musicFolderRepository, never()).delete(existingFolder);
        assertTrue(musicFolderRepository.findAll().contains(newFolder));
        assertTrue(musicFolderRepository.findAll().contains(existingFolder));
    }

    @Test
    public void testUpdateWithoutId() {
        // given
        MusicFolderInfo musicFolderInfo = new MusicFolderInfo();
        Mockito.reset(musicFolderRepository);

        // when and then
        assertThrows(IllegalArgumentException.class, () -> {
            mediaFolderService.updateMusicFolderByInfo(musicFolderInfo);
        });
        verifyNoInteractions(musicFolderRepository);
    }

    @Test
    public void testUpdateWithNonExistentId() {
        // given
        MusicFolderInfo musicFolderInfo = new MusicFolderInfo();
        musicFolderInfo.setId(-1);
        musicFolderInfo.setName("test");
        musicFolderInfo.setPath(tempMusicFolder.toString());
        musicFolderInfo.setType(Type.MEDIA.name());
        musicFolderInfo.setEnabled(true);
        Mockito.reset(musicFolderRepository);

        // when and then
        assertThrows(IllegalArgumentException.class, () -> {
            mediaFolderService.updateMusicFolderByInfo(musicFolderInfo);
        });
        verify(musicFolderRepository).findByIdAndDeletedFalse(-1);
        verify(musicFolderRepository, never()).findByDeleted(anyBoolean());
        verify(musicFolderRepository, never()).save(any(MusicFolder.class));

    }

    @Test
    public void testUpdateWithSamePathShouldSuccess() {
        // given
        MusicFolder existingFolder = new MusicFolder(tempMusicFolder, "Existing Folder", MusicFolder.Type.MEDIA, true, Instant.now());
        musicFolderRepository.save(existingFolder);
        MusicFolderInfo musicFolderInfo = new MusicFolderInfo(existingFolder, false, "");
        musicFolderInfo.setName("New Name");
        musicFolderInfo.setType(Type.PODCAST.name());
        musicFolderInfo.setEnabled(false);

        Mockito.reset(musicFolderRepository);
        // when
        mediaFolderService.updateMusicFolderByInfo(musicFolderInfo);

        // then
        verify(musicFolderRepository).findByIdAndDeletedFalse(existingFolder.getId());
        verify(musicFolderRepository, never()).findByDeleted(anyBoolean());
        verify(musicFolderRepository).save(any(MusicFolder.class));

        MusicFolder updatedFolder = musicFolderRepository.findById(existingFolder.getId()).get();
        assertEquals(musicFolderInfo.getName(), updatedFolder.getName());
        assertEquals(musicFolderInfo.getPath(), updatedFolder.getPath().toString());
        assertEquals(musicFolderInfo.getType(), updatedFolder.getType().name());
        assertEquals(musicFolderInfo.getEnabled(), updatedFolder.isEnabled());

    }

    @Test
    public void testUpdateWithSamePathWithDefferentFolderShouldFail() {
        // given
        MusicFolder existingFolder = new MusicFolder(tempMusicFolder, "Existing Folder", MusicFolder.Type.MEDIA, true, Instant.now());
        musicFolderRepository.save(existingFolder);
        MusicFolderInfo musicFolderInfo = new MusicFolderInfo(existingFolder, false, "");
        musicFolderInfo.setPath(tempDefaultMusicFolder.toString());
        Mockito.reset(musicFolderRepository);
        // when and then
        assertThrows(IllegalArgumentException.class, () -> {
            mediaFolderService.updateMusicFolderByInfo(musicFolderInfo);
        });
        verify(musicFolderRepository).findByIdAndDeletedFalse(existingFolder.getId());
        verify(musicFolderRepository).findByDeleted(false);
        verify(musicFolderRepository).findByDeleted(true);
        verify(musicFolderRepository, never()).save(any(MusicFolder.class));
    }


    @Test
    public void testUpdateWithAncestorPathOfDifferentFolderShouldFail() {
        // given
        MusicFolder existingFolder = new MusicFolder(tempMusicFolder, "Existing Folder", MusicFolder.Type.MEDIA, true, Instant.now());
        musicFolderRepository.save(existingFolder);
        MusicFolder existingFolder2 = new MusicFolder(tempMusicFolder2, "Existing Folder2", MusicFolder.Type.MEDIA, true, Instant.now());
        musicFolderRepository.save(existingFolder2);
        MusicFolderInfo musicFolderInfo = new MusicFolderInfo(existingFolder, false, "");
        musicFolderInfo.setPath(tempMusicFolder2.resolve("child").toString());
        Mockito.reset(musicFolderRepository);
        // when and then
        assertThrows(IllegalArgumentException.class, () -> {
            mediaFolderService.updateMusicFolderByInfo(musicFolderInfo);
        });
        verify(musicFolderRepository).findByIdAndDeletedFalse(existingFolder.getId());
        verify(musicFolderRepository).findByDeleted(false);
        verify(musicFolderRepository).findByDeleted(true);
        verify(musicFolderRepository, never()).save(any(MusicFolder.class));
    }

    @Test
    public void testUpdateWithDecendantPathOfDifferentFolderShouldFail() {
        // given
        MusicFolder existingFolder = new MusicFolder(tempMusicFolder, "Existing Folder", MusicFolder.Type.MEDIA, true, Instant.now());
        musicFolderRepository.save(existingFolder);
        MusicFolder existingFolder2 = new MusicFolder(tempMusicFolder2, "Existing Folder2", MusicFolder.Type.MEDIA, true, Instant.now());
        musicFolderRepository.save(existingFolder2);
        MusicFolderInfo musicFolderInfo = new MusicFolderInfo(existingFolder, false, "");
        musicFolderInfo.setPath(tempMusicFolder2.getParent().toString());
        Mockito.reset(musicFolderRepository);
        // when and then
        assertThrows(IllegalArgumentException.class, () -> {
            mediaFolderService.updateMusicFolderByInfo(musicFolderInfo);
        });
        verify(musicFolderRepository).findByIdAndDeletedFalse(existingFolder.getId());
        verify(musicFolderRepository).findByDeleted(false);
        verify(musicFolderRepository).findByDeleted(true);
        verify(musicFolderRepository, never()).save(any(MusicFolder.class));
    }

    @Test
    public void testUpdateByInfo() {
        // given
        MusicFolder existingFolder = new MusicFolder(tempMusicFolder, "Existing Folder", MusicFolder.Type.MEDIA, true, Instant.now());
        musicFolderRepository.save(existingFolder);
        MusicFolderInfo musicFolderInfo = new MusicFolderInfo(existingFolder, false, "");
        musicFolderInfo.setName("New Name");
        musicFolderInfo.setPath(tempMusicFolder.resolve("child").toString());
        musicFolderInfo.setType(Type.PODCAST.name());
        musicFolderInfo.setEnabled(false);
        Mockito.reset(musicFolderRepository);

        // when
        mediaFolderService.updateMusicFolderByInfo(musicFolderInfo);

        // then
        verify(musicFolderRepository).findByIdAndDeletedFalse(existingFolder.getId());
        verify(musicFolderRepository).findByDeleted(false);
        verify(musicFolderRepository).findByDeleted(true);
        verify(musicFolderRepository).save(any(MusicFolder.class));

        MusicFolder updatedFolder = musicFolderRepository.findById(existingFolder.getId()).get();
        assertEquals(musicFolderInfo.getName(), updatedFolder.getName());
        assertEquals(musicFolderInfo.getPath(), updatedFolder.getPath().toString());
        assertEquals(musicFolderInfo.getType(), updatedFolder.getType().name());
        assertEquals(musicFolderInfo.getEnabled(), updatedFolder.isEnabled());
    }

    @Test
    public void testEnableNonexistentPodcastFolder() {
        // when
        boolean result = mediaFolderService.enablePodcastFolder(-1);

        // then
        assertFalse(result);
        verify(musicFolderRepository).findByIdAndTypeAndDeletedFalse(-1, Type.PODCAST);
    }

    @Test
    public void testEnableNonPodcastFolder() {
        // given
        MusicFolder mediaFolder = new MusicFolder(tempMusicFolder, "Media Folder", MusicFolder.Type.MEDIA, true, Instant.now());
        musicFolderRepository.save(mediaFolder);

        // when
        boolean result = mediaFolderService.enablePodcastFolder(mediaFolder.getId());

        // then
        assertFalse(result);
        verify(musicFolderRepository).findByIdAndTypeAndDeletedFalse(mediaFolder.getId(), Type.PODCAST);
    }

    @Test
    public void testEnablePodcastFolderSuccessfully() {
        // given
        MusicFolder podcastFolder1 = new MusicFolder(tempMusicFolder.resolve("podcast1"), "Podcast Folder 1", MusicFolder.Type.PODCAST, false, Instant.now());
        MusicFolder podcastFolder2 = new MusicFolder(tempMusicFolder.resolve("podcast2"), "Podcast Folder 2", MusicFolder.Type.PODCAST, true, Instant.now());
        musicFolderRepository.saveAll(List.of(podcastFolder1, podcastFolder2));

        // when
        boolean result = mediaFolderService.enablePodcastFolder(podcastFolder1.getId());

        // then
        assertTrue(result);
        assertTrue(musicFolderRepository.findById(podcastFolder1.getId()).get().isEnabled());
        assertFalse(musicFolderRepository.findById(podcastFolder2.getId()).get().isEnabled());
        verify(musicFolderRepository).findByIdAndTypeAndDeletedFalse(podcastFolder1.getId(), Type.PODCAST);
        verify(musicFolderRepository).findByIdNotAndTypeAndDeletedFalse(podcastFolder1.getId(), Type.PODCAST);
    }

    @Test
    public void testDeleteMusicFolderWithNonExistentId() {

        // given
        Mockito.reset(musicFolderRepository);
        Mockito.reset(mediaFileDao);

        // when
        mediaFolderService.deleteMusicFolder(-1);

        // then
        verify(musicFolderRepository).findByIdAndDeletedFalse(-1);
        verifyNoMoreInteractions(musicFolderRepository);
        verifyNoInteractions(mediaFileDao);
    }

    @Test
    public void testDeleteMusicFolderWithEmptyFolder() {

        // given
        MusicFolder musicFolder = new MusicFolder(tempMusicFolder, "Music Folder", MusicFolder.Type.MEDIA, true, Instant.now());
        musicFolderRepository.save(musicFolder);
        long count = musicFolderRepository.count();
        Mockito.reset(musicFolderRepository);
        Mockito.reset(mediaFileDao);

        when(mediaFileDao.getMediaFileCount(musicFolder.getId())).thenReturn(0);

        // when
        mediaFolderService.deleteMusicFolder(musicFolder.getId());

        // then
        verify(musicFolderRepository).findByIdAndDeletedFalse(musicFolder.getId());
        verify(musicFolderRepository).delete(any(MusicFolder.class));
        assertEquals(count - 1L, musicFolderRepository.count());
    }

    @Test
    public void testDeleteMusicFolderWithAncestors() {

        // given
        MusicFolder musicFolder = new MusicFolder(tempMusicFolder, "Music Folder", MusicFolder.Type.MEDIA, true, Instant.now());
        musicFolderRepository.save(musicFolder);
        MusicFolder childFolder = new MusicFolder(tempMusicFolder.resolve("child"), "Child Folder", MusicFolder.Type.MEDIA, true, Instant.now());
        musicFolderRepository.save(childFolder);
        long count = musicFolderRepository.count();
        Mockito.reset(musicFolderRepository);
        Mockito.reset(mediaFileDao);

        when(mediaFileDao.getMediaFileCount(childFolder.getId())).thenReturn(10);

        // when
        mediaFolderService.deleteMusicFolder(childFolder.getId());

        // then
        verify(musicFolderRepository).findByIdAndDeletedFalse(childFolder.getId());
        verify(musicFolderRepository).delete(any(MusicFolder.class));
        verify(musicFolderDao).reassignChildren(any(MusicFolder.class), any(MusicFolder.class));
        verify(mediaFileDao).deleteMediaFiles(childFolder.getId());
        assertEquals(count - 1L, musicFolderRepository.count());
    }

    @Test
    public void testDeleteMusicFolderWithDecendants() {

        // given
        MusicFolder musicFolder = new MusicFolder(tempMusicFolder, "Music Folder", MusicFolder.Type.MEDIA, true, Instant.now());
        musicFolderRepository.save(musicFolder);
        MusicFolder childFolder = new MusicFolder(tempMusicFolder.resolve("child"), "Child Folder", MusicFolder.Type.MEDIA, true, Instant.now());
        musicFolderRepository.save(childFolder);
        long count = musicFolderRepository.count();
        Mockito.reset(musicFolderRepository);
        Mockito.reset(mediaFileDao);

        when(mediaFileDao.getMediaFileCount(musicFolder.getId())).thenReturn(10);

        // when
        mediaFolderService.deleteMusicFolder(musicFolder.getId());

        // then
        verify(musicFolderRepository).findByIdAndDeletedFalse(musicFolder.getId());
        verify(musicFolderRepository).save(any(MusicFolder.class));
        verifyNoInteractions(musicFolderDao);
        verify(mediaFileDao).deleteMediaFiles(musicFolder.getId());
        assertEquals(count, musicFolderRepository.count());
        MusicFolder deletedFolder = musicFolderRepository.findById(musicFolder.getId()).get();
        assertTrue(deletedFolder.isDeleted());
        assertFalse(deletedFolder.isEnabled());

    }

    @Test
    public void testDeleteMusicFolder() {

        // given
        MusicFolder musicFolder = new MusicFolder(tempMusicFolder, "Music Folder", MusicFolder.Type.MEDIA, true, Instant.now());
        musicFolderRepository.save(musicFolder);
        long count = musicFolderRepository.count();
        Mockito.reset(musicFolderRepository);
        Mockito.reset(mediaFileDao);

        when(mediaFileDao.getMediaFileCount(musicFolder.getId())).thenReturn(10);

        // when
        mediaFolderService.deleteMusicFolder(musicFolder.getId());

        // then
        verify(musicFolderRepository).findByIdAndDeletedFalse(musicFolder.getId());
        verify(musicFolderRepository).save(any(MusicFolder.class));
        verifyNoInteractions(musicFolderDao);
        verify(mediaFileDao).deleteMediaFiles(musicFolder.getId());
        assertEquals(count, musicFolderRepository.count());
        MusicFolder deletedFolder = musicFolderRepository.findById(musicFolder.getId()).get();
        assertTrue(deletedFolder.isDeleted());
        assertFalse(deletedFolder.isEnabled());

    }
}
