/**
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
package org.airsonic.player.controller;

import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.CoverArt;
import org.airsonic.player.domain.CoverArt.EntityType;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MediaFile.MediaType;
import org.airsonic.player.domain.Playlist;
import org.airsonic.player.domain.PodcastChannel;
import org.airsonic.player.service.AlbumService;
import org.airsonic.player.service.ArtistService;
import org.airsonic.player.service.CoverArtService;
import org.airsonic.player.service.MediaFileService;
import org.airsonic.player.service.PlaylistService;
import org.airsonic.player.service.PodcastService;
import org.airsonic.player.util.ImageUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Y.Tory
 * @version $Id$
 */
@SpringBootTest
@AutoConfigureMockMvc
public class CoverArtControllerTest {

    @Autowired
    private MockMvc mvc;

    @SpyBean
    private MediaFileService mediaFileService;

    @MockBean
    private AlbumService albumService;

    @MockBean
    private ArtistService artistService;

    @MockBean
    private PlaylistService playlistService;

    @MockBean
    private PodcastService podcastService;

    @SpyBean
    private CoverArtService coverArtService;

    @Mock
    private CoverArt mockedCoverArt;

    @InjectMocks
    private CoverArtController coverArtController;

    @TempDir
    private static Path tempDir;

    /** authentication info */
    private final String AIRSONIC_USER = "admin";
    private final String AIRSONIC_PASSWORD = "admin";

    /** resource */
    private final ClassPathResource IMAGE_RESOURCE = new ClassPathResource("MEDIAS/Music2/_DIR_ chrome hoof - 2004/Folder.jpg");
    private final ClassPathResource PLAYLIST_RESOURCE = new ClassPathResource("COVERARTS/playlist.png");
    private final ClassPathResource PODCAST_RESOURCE = new ClassPathResource("COVERARTS/podcast.png");

    @BeforeAll
    public static void setup() {
        System.setProperty("airsonic.home", tempDir.toString());
    }

    @Test
    @WithMockUser(username = AIRSONIC_USER, password = AIRSONIC_PASSWORD)
    public void getCoverArtFallbackWithNullIdTest() throws Exception {
        // no parameter
        byte[] actual = mvc.perform(get("/coverArt"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // load default_cover.jpg image
            InputStream in = coverArtController.getClass().getResourceAsStream("default_cover.jpg");
            BufferedImage image = ImageIO.read(in);

            // create expected response body
            ImageIO.write(image, "jpeg", out);
            byte[] expected = out.toByteArray();

            // assertion
            assertArrayEquals(expected, actual);
        }
    }

    @Test
    @WithMockUser(username = AIRSONIC_USER, password = AIRSONIC_PASSWORD)
    public void getCoverArtFallbackWithSizeParameterTest() throws Exception {

        // add size parameter to query
        byte[] actual = mvc.perform(get("/coverArt")
                .param("size", "30"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // load default_cover.jpg image
            InputStream in = coverArtController.getClass().getResourceAsStream("default_cover.jpg");
            BufferedImage image = ImageIO.read(in);

            // scale expected image
            BufferedImage thumbImage = ImageUtil.scale(image, 30, 30);

            // create expected response body
            ImageIO.write(thumbImage, "jpeg", out);
            byte[] expected = out.toByteArray();

            // assertion
            assertArrayEquals(expected, actual);
        }
    }

    /** get cover art by id of media which type is album */
    @Test
    @WithMockUser(username = AIRSONIC_USER, password = AIRSONIC_PASSWORD)
    public void getCoverArtWithIntegerIdTest() throws Exception {

        final Integer MEDIA_ID = 100;
        // set up mocked media file
        MediaFile mockedMediaFile = new MediaFile();
        mockedMediaFile.setId(MEDIA_ID);
        mockedMediaFile.setMediaType(MediaType.ALBUM);

        // set up mocked cover art
        CoverArt mockedCoverArt = new CoverArt(MEDIA_ID, EntityType.ALBUM, IMAGE_RESOURCE.getFile().getAbsolutePath(), null, false);

        // set up mock bean
        doReturn(mockedMediaFile).when(mediaFileService).getMediaFile(anyInt());
        doReturn(mockedCoverArt).when(coverArtService).get(any(EntityType.class), anyInt());
        doReturn(IMAGE_RESOURCE.getFile().toPath()).when(coverArtService).getFullPath(any());

        // prepare expected
        byte[] expected = IMAGE_RESOURCE.getInputStream().readAllBytes();

        // execution
        byte[] actual = mvc.perform(get("/coverArt")
                .param("id", MEDIA_ID.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        // assertion
        assertArrayEquals(expected, actual);
        verify(coverArtService).get(eq(EntityType.MEDIA_FILE), eq(MEDIA_ID));

    }

    /** get cover art by id with album prerix "al-" */
    @Test
    @WithMockUser(username = AIRSONIC_USER, password = AIRSONIC_PASSWORD)
    public void getCoverArtWithAlbumIdTest() throws Exception {

        final int ALBUM_ID = 100;

        // set up mocked cover art
        CoverArt mockedCoverArt = new CoverArt(1, EntityType.ALBUM, IMAGE_RESOURCE.getFile().getAbsolutePath(),null, false);

        // set up mocked album
        Album mockedAlbum = new Album();
        mockedAlbum.setId(ALBUM_ID);

        // set up mock
        doReturn(mockedAlbum).when(albumService).getAlbum(anyInt());
        doReturn(mockedCoverArt).when(coverArtService).get(any(EntityType.class), anyInt());
        doReturn(IMAGE_RESOURCE.getFile().toPath()).when(coverArtService).getFullPath(any());

        // prepare expected
        byte[] expected = IMAGE_RESOURCE.getInputStream().readAllBytes();

        // execution
        byte[] actual = mvc.perform(get("/coverArt")
                .param("id", String.format("al-%d", ALBUM_ID)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        // assertion
        assertArrayEquals(expected, actual);
        verify(albumService).getAlbum(eq(ALBUM_ID));
        verify(coverArtService).get(eq(EntityType.ALBUM), eq(ALBUM_ID));
    }

    /** get cover art by id with artist prerix "ar-" */
    @Test
    @WithMockUser(username = AIRSONIC_USER, password = AIRSONIC_PASSWORD)
    public void getCoverArtWithArtistIdTest() throws Exception {

        final int ARTIST_ID = 100;

        // set up mocked album
        Artist mockedArtist = new Artist();
        mockedArtist.setId(ARTIST_ID);

        // set up mock
        doReturn(mockedArtist).when(artistService).getArtist(anyInt());
        doReturn(mockedCoverArt).when(coverArtService).get(any(EntityType.class), anyInt());
        doReturn(IMAGE_RESOURCE.getFile().toPath()).when(coverArtService).getFullPath(any());

        // prepare expected
        byte[] expected = IMAGE_RESOURCE.getInputStream().readAllBytes();

        // execution
        MvcResult result = mvc.perform(get("/coverArt")
                .param("id", String.format("ar-%d", ARTIST_ID)))
                .andExpect(status().isOk())
                .andReturn();
        byte[] actual = result.getResponse().getContentAsByteArray();

        // assertion
        assertArrayEquals(expected, actual);
        verify(coverArtService).get(eq(EntityType.ARTIST), eq(ARTIST_ID));
    }

    /** get cover art by id with playlist prerix "pl-" */
    @Test
    @WithMockUser(username = AIRSONIC_USER, password = AIRSONIC_PASSWORD)
    public void getCoverArtWithPlaylistIdTest() throws Exception {

        // set up mocked playlist
        final int PLAYLIST_ID = 100;
        Playlist mockedPlaylist = new Playlist();
        mockedPlaylist.setId(PLAYLIST_ID);
        mockedPlaylist.setName("playlist");

        // set up mock
        when(playlistService.getPlaylist(anyInt())).thenReturn(mockedPlaylist);

        // prepare expected
        byte[] expected = PLAYLIST_RESOURCE.getInputStream().readAllBytes();

        // execution
        MvcResult result = mvc.perform(get("/coverArt")
                .param("id", String.format("pl-%d", PLAYLIST_ID)))
                .andExpect(status().isOk())
                .andReturn();
        byte[] actual = result.getResponse().getContentAsByteArray();

        // assertion
        assertArrayEquals(expected, actual);
    }

    /** get cover art by id with podcast prerix "pod-" */
    @Test
    @WithMockUser(username = AIRSONIC_USER, password = AIRSONIC_PASSWORD)
    public void getCoverArtWithPodcastIdTest() throws Exception {


        // set up mocked playlist
        final Integer PODCAST_ID = 100;
        PodcastChannel mockedPodcast =
            new PodcastChannel(PODCAST_ID, "http://example.com", "podcast", "description", "http://example.com", null, "errorMessage", null);

        // set up mock
        when(podcastService.getChannel(anyInt())).thenReturn(mockedPodcast);

        // prepare expected
        byte[] expected = PODCAST_RESOURCE.getInputStream().readAllBytes();

        // execution
        MvcResult result = mvc.perform(get("/coverArt")
                .param("id", String.format("pod-%d", PODCAST_ID)))
                .andExpect(status().isOk())
                .andReturn();
        byte[] actual = result.getResponse().getContentAsByteArray();

        // assertion
        assertArrayEquals(expected, actual);
    }


}
