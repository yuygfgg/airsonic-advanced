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
package org.airsonic.player.repository;

import org.airsonic.player.config.AirsonicHomeConfig;
import org.airsonic.player.dao.UserDao;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.User;
import org.airsonic.player.domain.User.Role;
import org.airsonic.player.domain.UserCredential;
import org.airsonic.player.domain.UserCredential.App;
import org.airsonic.player.domain.entity.StarredArtist;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@EnableConfigurationProperties({AirsonicHomeConfig.class})
@Transactional
public class ArtistRepositoryTest {

    @Autowired
    ArtistRepository artistRepository;

    @Autowired
    StarredArtistRepository starredArtistRepository;

    @TempDir
    private static Path tempDir;

    @BeforeAll
    public static void setup() {
        System.setProperty("airsonic.home", tempDir.toString());
    }

    @Nested
    public class ArtistTest {

        @Test
        public void testFindByName() {
            assertTrue(artistRepository.findByName("name").isEmpty());

            Artist artist = new Artist("name");
            artist.setLastScanned(Instant.now());

            artistRepository.save(artist);
            assertTrue(artistRepository.findByName("name").isPresent());
            artistRepository.delete(artist);
        }

        @Test
        public void testFindByNameAndFolderIdIn() {
            assertTrue(artistRepository.findByNameAndFolderIdIn("name", new ArrayList<>(List.of(1))).isEmpty());

            Artist artist = new Artist("name");
            artist.setLastScanned(Instant.now());
            artist.setFolderId(1);

            artistRepository.save(artist);
            assertTrue(artistRepository.findByNameAndFolderIdIn("name", new ArrayList<>(List.of(1))).isPresent());

            artistRepository.delete(artist);
        }

        @Test
        public void testFindByFolderIdInAndPresentTrue() {
            assertTrue(artistRepository.findByFolderIdInAndPresentTrue(new ArrayList<>(List.of(1, 2)), Sort.by(Direction.ASC, "name")).isEmpty());

            Artist artist = new Artist("name");
            artist.setLastScanned(Instant.now());
            artist.setFolderId(1);
            artist.setPresent(true);
            artistRepository.save(artist);

            Artist artist2 = new Artist("name2");
            artist2.setLastScanned(Instant.now());
            artist2.setFolderId(2);
            artist2.setPresent(true);
            artistRepository.save(artist2);
            assertTrue(artistRepository.findByFolderIdInAndPresentTrue(new ArrayList<>(List.of(1, 2)), Sort.by(Direction.ASC, "name")).size() == 2);

            artistRepository.delete(artist);
            artistRepository.delete(artist2);
        }

        @Test
        public void testFindByPresentFalse() {
            assertTrue(artistRepository.findByPresentFalse().isEmpty());

            Artist artist = new Artist("name");
            artist.setLastScanned(Instant.now());
            artist.setPresent(false);
            artistRepository.save(artist);
            assertTrue(artistRepository.findByPresentFalse().size() == 1);

            artistRepository.delete(artist);
        }

        @Test
        public void testMarkNonPresent() {
            assertTrue(artistRepository.findByPresentFalse().isEmpty());

            Artist artist = new Artist("name");
            artist.setLastScanned(Instant.now().minusSeconds(86400));
            artist.setPresent(true);
            artistRepository.saveAndFlush(artist);
            assertTrue(artistRepository.findByPresentFalse().isEmpty());
            assertTrue(artistRepository.findByName("name").isPresent());

            artistRepository.markNonPresent(Instant.now().minusSeconds(86400));
            assertEquals(1, artistRepository.findByPresentFalse().size());
            artistRepository.delete(artist);
        }

        @Test
        public void testDeleteAllByPresentFalse() {
            assertTrue(artistRepository.findByPresentFalse().isEmpty());

            Artist artist = new Artist("name");
            artist.setLastScanned(Instant.now());
            artist.setPresent(false);
            artistRepository.save(artist);
            assertTrue(artistRepository.findByPresentFalse().size() == 1);

            artistRepository.deleteAllByPresentFalse();
            assertTrue(artistRepository.findByPresentFalse().isEmpty());
        }
    }

    @Nested
    class StarredArtistTest {

        @Autowired UserDao userDao;

        private final String TEST_USER_NAME = "testUserForArtistRepo";

        @BeforeEach
        public void setup() {

            User user = new User(TEST_USER_NAME, "artistrepo@activeobjects.no", false, 1000L, 2000L, 3000L, Set.of(Role.ADMIN, Role.COMMENT, Role.COVERART, Role.PLAYLIST, Role.PODCAST, Role.STREAM, Role.JUKEBOX, Role.SETTINGS));
            UserCredential uc = new UserCredential(TEST_USER_NAME, TEST_USER_NAME, "secret", "noop", App.AIRSONIC);
            userDao.createUser(user, uc);
        }

        @AfterEach
        public void clean() {
            userDao.deleteUser(TEST_USER_NAME);
        }

        @Test
        public void testFindByUsername() {

            // given
            Artist artist = new Artist("name");
            artist.setLastScanned(Instant.now());
            artistRepository.save(artist);
            Artist artist2 = new Artist("name2");
            artist2.setLastScanned(Instant.now());
            artistRepository.save(artist2);

            StarredArtist starredArtist = new StarredArtist(artist, TEST_USER_NAME, Instant.now());
            starredArtistRepository.save(starredArtist);
            StarredArtist starredArtist2 = new StarredArtist(artist2, TEST_USER_NAME, Instant.now());
            starredArtistRepository.save(starredArtist2);

            // when / then
            assertEquals(2, starredArtistRepository.findByUsername(TEST_USER_NAME).size());

            // cleanup
            artistRepository.delete(artist);
            artistRepository.delete(artist2);
        }

        @Test
        public void testFindByArtistAndUsername() {

            // given
            Artist artist = new Artist("name");
            artist.setLastScanned(Instant.now());
            artistRepository.save(artist);
            StarredArtist starredArtist = new StarredArtist(artist, TEST_USER_NAME, Instant.now());
            starredArtistRepository.save(starredArtist);

            // when / then
            assertTrue(starredArtistRepository.findByArtistIdAndUsername(artist.getId(), TEST_USER_NAME).isPresent());
            assertFalse(starredArtistRepository.findByArtistIdAndUsername(artist.getId(), "not" + TEST_USER_NAME).isPresent());
            assertFalse(starredArtistRepository.findByArtistIdAndUsername(artist.getId() + 1, TEST_USER_NAME).isPresent());

            // cleanup
            artistRepository.delete(artist);
        }

        @Test
        public void testFindByUsernameAndArtistFolderIdInAndArtistPresentTrue() {

            // given
            Artist artist = new Artist("name");
            artist.setLastScanned(Instant.now());
            artist.setFolderId(1);
            artist.setPresent(true);
            artistRepository.save(artist);
            Artist artist2 = new Artist("name2");
            artist2.setLastScanned(Instant.now());
            artist2.setFolderId(2);
            artist2.setPresent(true);
            artistRepository.save(artist2);
            // not present
            Artist artist3 = new Artist("name3");
            artist3.setLastScanned(Instant.now());
            artist3.setFolderId(2);
            artist3.setPresent(false);
            artistRepository.save(artist3);

            StarredArtist starredArtist = new StarredArtist(artist, TEST_USER_NAME, Instant.now());
            starredArtistRepository.save(starredArtist);
            StarredArtist starredArtist2 = new StarredArtist(artist2, TEST_USER_NAME, Instant.now());
            starredArtistRepository.save(starredArtist2);
            StarredArtist starredArtist3 = new StarredArtist(artist3, TEST_USER_NAME, Instant.now());
            starredArtistRepository.save(starredArtist3);

            // when
            List<StarredArtist> starredArtists = starredArtistRepository
                    .findByUsernameAndArtistFolderIdInAndArtistPresentTrue(TEST_USER_NAME,
                            new ArrayList<>(List.of(1, 2)), Sort.by("created"));

            // then
            assertEquals(2, starredArtists.size());
            assertEquals(artist, starredArtists.get(0).getArtist());
            assertEquals(artist2, starredArtists.get(1).getArtist());

            // cleanup
            artistRepository.delete(artist);
            artistRepository.delete(artist2);
            artistRepository.delete(artist3);
        }

        @Test
        public void testDeleteByArtistIdAndUsername() {

            // given
            Artist artist = new Artist("name");
            artist.setLastScanned(Instant.now());
            artistRepository.save(artist);
            StarredArtist starredArtist = new StarredArtist(artist, TEST_USER_NAME, Instant.now());
            starredArtistRepository.save(starredArtist);

            // when
            starredArtistRepository.deleteByArtistIdAndUsername(artist.getId(), TEST_USER_NAME);

            // then
            assertTrue(starredArtistRepository.findByArtistIdAndUsername(artist.getId(), TEST_USER_NAME).isEmpty());

            // cleanup
            artistRepository.delete(artist);
        }
    }
}
