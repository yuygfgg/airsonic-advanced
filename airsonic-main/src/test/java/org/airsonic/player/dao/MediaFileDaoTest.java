/*
 This file is part of Airsonic.

 Airsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2023 (C) Y.Tory
 */

package org.airsonic.player.dao;

import org.airsonic.player.config.AirsonicHomeConfig;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MediaFile.MediaType;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.MusicFolder.Type;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Unit test of {@link MediaFileDao}.
 *
 * @author Y.Tory
 */
@SpringBootTest
@EnableConfigurationProperties(AirsonicHomeConfig.class)
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class)
public class MediaFileDaoTest {

    @Autowired
    MediaFileDao mediaFileDao;

    @Autowired
    MusicFolderDao musicFolderDao;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @TempDir
    private static Path tempAirsonicDir;

    private static final String MUSIC_FOLDER_PATH = "/path";

    @BeforeAll
    public static void setUp() {
        System.setProperty("airsonic.home", tempAirsonicDir.toString());
    }

    @AfterAll
    public static void cleanUp() {
        System.clearProperty("airsonic.home");
    }

    @BeforeEach
    public void cleanUpBefore() {
        MusicFolder musicFolder = new MusicFolder(Paths.get(MUSIC_FOLDER_PATH), "name", Type.MEDIA, true, Instant.now().truncatedTo(ChronoUnit.MICROS));
        musicFolderDao.createMusicFolder(musicFolder);
    }

    @AfterEach
    public void cleanUpAfter() {
        MusicFolder folder = musicFolderDao.getMusicFolderForPath(MUSIC_FOLDER_PATH);
        jdbcTemplate.execute("DELETE FROM media_file");
        musicFolderDao.deleteMusicFolder(folder.getId());
    }

    @Test
    public void testGetMediaFilesByRelativePathAndFolderId() {
        //prepare
        MusicFolder folder = musicFolderDao.getAllMusicFolders().get(0);
        MediaFile baseFile = new MediaFile();
        baseFile.setFolderId(folder.getId());
        baseFile.setPath("test.wav");
        baseFile.setMediaType(MediaType.MUSIC);
        baseFile.setIndexPath("test.cue");
        baseFile.setStartPosition(MediaFile.NOT_INDEXED);
        baseFile.setCreated(Instant.now());
        baseFile.setChanged(Instant.now());
        baseFile.setLastScanned(Instant.now());
        baseFile.setChildrenLastUpdated(Instant.now());
        // save
        mediaFileDao.createOrUpdateMediaFile(baseFile, file -> {});

        // assert
        List<MediaFile> registeredTracks = mediaFileDao.getMediaFilesByRelativePathAndFolderId("test.wav", folder.getId());
        assertEquals(1, registeredTracks.size());

        // update
        MediaFile mediaFile = new MediaFile();
        mediaFile.setFolderId(folder.getId());
        mediaFile.setPath("test.wav");
        mediaFile.setMediaType(MediaType.MUSIC);
        mediaFile.setStartPosition(10.0);
        mediaFile.setCreated(Instant.now());
        mediaFile.setChanged(Instant.now());
        mediaFile.setLastScanned(Instant.now());
        mediaFile.setChildrenLastUpdated(Instant.now());
        mediaFileDao.createOrUpdateMediaFile(mediaFile, file -> {});

        // assertion
        registeredTracks = mediaFileDao.getMediaFilesByRelativePathAndFolderId("test.wav", folder.getId());
        assertEquals(2, registeredTracks.size());
        registeredTracks.forEach(t -> assertEquals("test.wav",t.getPath()));

        List<MediaFile> wrongFolderTracks = mediaFileDao.getMediaFilesByRelativePathAndFolderId("test.wav", folder.getId() + 1);
        assertEquals(0, wrongFolderTracks.size());

        List<MediaFile> wrongPathTracks = mediaFileDao.getMediaFilesByRelativePathAndFolderId("wrong.wav", folder.getId());
        assertEquals(0, wrongPathTracks.size());
    }

}