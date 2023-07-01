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

package org.airsonic.player.service;

import org.airsonic.player.dao.PodcastDao;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.PodcastChannel;
import org.airsonic.player.domain.PodcastChannelRule;
import org.airsonic.player.domain.PodcastEpisode;
import org.airsonic.player.domain.PodcastStatus;
import org.airsonic.player.domain.Version;
import org.airsonic.player.service.websocket.AsyncWebSocketClient;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PodcastServiceTestCase {

    @Mock
    private SettingsService settingsService;
    @Mock
    private TaskSchedulingService taskService;
    @Mock
    private PodcastDao podcastDao;
    @Mock
    private SecurityService securityService;
    @Mock
    private MediaFileService mediaFileService;
    @Mock
    private MediaFolderService mediaFolderService;
    @Mock
    private AsyncWebSocketClient asyncSocketClient;
    @Mock
    private VersionService versionService;

    @InjectMocks
    @Spy
    private PodcastService podcastService;

    @Mock
    private PodcastEpisode mockedEpisode;
    @Mock
    private MediaFile mockedMediaFile;
    @Mock
    private MusicFolder mockedMusicFolder;
    @Mock
    private PodcastChannel mockedChannel;
    @Mock
    private CloseableHttpClient mockedHttpClient;
    @Mock
    private CloseableHttpResponse mockedHttpResponse;
    @Mock
    private HttpEntity mockedHttpEntity;

    @TempDir
    private Path tempFolder;


    // test data
    private PodcastChannelRule RULE_SCHEDULE = new PodcastChannelRule(1, 1, null, null);
    private PodcastChannelRule RULE_UNSCHEDULE = new PodcastChannelRule(2, -1, null, null);

    @Test
    void testCreateOrUpdateChannelRule() {
        // given
        // prepare instant
        Instant now = Instant.parse("2020-01-01T00:00:00Z");
        Instant expectedFirstTime = Instant.parse("2020-01-01T00:05:00Z");

        try (MockedStatic<Instant> mockedInstant = Mockito.mockStatic(Instant.class, Mockito.CALLS_REAL_METHODS)) {
            mockedInstant.when(Instant::now).thenReturn(now);
            // when
            podcastService.createOrUpdateChannelRule(RULE_SCHEDULE);

            // then
            verify(podcastDao).createOrUpdateChannelRule(RULE_SCHEDULE);
            verify(taskService).scheduleAtFixedRate(eq("podcast-channel-refresh-1"), any(), eq(expectedFirstTime), eq(Duration.ofHours(1)), eq(true));
        }
    }

    @Test
    void testDeleteChannel() throws Exception {

        // given
        int channelId = 1;
        when(podcastService.getEpisodes(channelId)).thenReturn(Arrays.asList(mockedEpisode));
        when(mockedEpisode.getMediaFileId()).thenReturn(null);
        when(mockedEpisode.getId()).thenReturn(10);
        when(podcastDao.getChannel(channelId)).thenReturn(mockedChannel);
        when(mockedChannel.getMediaFileId()).thenReturn(1);
        when(mediaFileService.getMediaFile(1)).thenReturn(mockedMediaFile);
        when(mockedMediaFile.getFolderId()).thenReturn(1);
        when(mediaFolderService.getMusicFolderById(1)).thenReturn(mockedMusicFolder);
        when(mockedMusicFolder.getPath()).thenReturn(tempFolder);
        when(mockedMediaFile.getFullPath(tempFolder)).thenReturn(tempFolder.resolve("test.mp3"));
        Files.createFile(tempFolder.resolve("test.mp3"));

        // when
        podcastService.deleteChannel(channelId);

        // then
        verify(podcastDao).deleteEpisode(10);
        verify(podcastDao).deleteChannel(channelId);
        verify(podcastService, times(1)).deleteEpisode(mockedEpisode, false);
        verify(mediaFileService).refreshMediaFile(mockedMediaFile, mockedMusicFolder);
        verify(asyncSocketClient).send("/topic/podcasts/deleted", channelId);
        assertFalse(Files.exists(tempFolder.resolve("test.mp3")));
    }

    @Test
    void testDeleteChannelRule() {
        // when
        podcastService.deleteChannelRule(10);

        // then
        verify(podcastDao).deleteChannelRule(10);
        assertUnschedule(10); // assert call unschedule

    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "false"})
    void testDeleteEpisodeWithNullShouldDoNothing(boolean logicalDelete) {
        // given
        when(podcastDao.getEpisode(1)).thenReturn(null);

        // when
        podcastService.deleteEpisode(1, logicalDelete);

        // then
        verify(podcastDao, never()).deleteEpisode(anyInt());
        verify(podcastDao, never()).updateEpisode(any());
    }

    @Test
    public void testDeleteEpisodeForLogicalDeleteShouldUpdateEpisode() throws Exception {
        // given
        when(podcastDao.getEpisode(1)).thenReturn(mockedEpisode);
        when(mockedEpisode.getMediaFileId()).thenReturn(1);
        when(mediaFileService.getMediaFile(1)).thenReturn(mockedMediaFile);
        when(mockedMediaFile.getFolderId()).thenReturn(1);
        when(mediaFolderService.getMusicFolderById(1)).thenReturn(mockedMusicFolder);
        when(mockedMusicFolder.getPath()).thenReturn(tempFolder);
        when(mockedMediaFile.getFullPath(tempFolder)).thenReturn(tempFolder.resolve("test.mp3"));
        Files.createFile(tempFolder.resolve("test.mp3"));
        assertTrue(Files.exists(tempFolder.resolve("test.mp3")));


        // when
        podcastService.deleteEpisode(1, true);

        // then
        verify(podcastDao, never()).deleteEpisode(anyInt());
        verify(mediaFileService).refreshMediaFile(mockedMediaFile, mockedMusicFolder);
        verify(mockedEpisode).setStatus(PodcastStatus.DELETED);
        verify(mockedEpisode).setErrorMessage(null);
        verify(podcastDao).updateEpisode(mockedEpisode);
        assertFalse(Files.exists(tempFolder.resolve("test.mp3")));
    }

    @Test
    public void testDeleteEpisodeForDeleteShouldDeleteEpisode() throws Exception {
        // given
        when(podcastDao.getEpisode(1)).thenReturn(mockedEpisode);
        when(mockedEpisode.getMediaFileId()).thenReturn(1);
        when(mockedEpisode.getId()).thenReturn(1);
        when(mediaFileService.getMediaFile(1)).thenReturn(mockedMediaFile);
        when(mockedMediaFile.getFolderId()).thenReturn(1);
        when(mediaFolderService.getMusicFolderById(1)).thenReturn(mockedMusicFolder);
        when(mockedMusicFolder.getPath()).thenReturn(tempFolder);
        when(mockedMediaFile.getFullPath(tempFolder)).thenReturn(tempFolder.resolve("test.mp3"));
        Files.createFile(tempFolder.resolve("test.mp3"));
        assertTrue(Files.exists(tempFolder.resolve("test.mp3")));

        // when
        podcastService.deleteEpisode(1, false);

        // then
        verify(podcastDao).deleteEpisode(1);
        verify(mediaFileService).refreshMediaFile(mockedMediaFile, mockedMusicFolder);
        assertFalse(Files.exists(tempFolder.resolve("test.mp3")));
        verify(podcastDao, never()).updateEpisode(any());
    }

    @Test
    void testDoDownloadEpisodeWithDeletedEpisodeShouldDoNothing() {

        // given
        when(podcastDao.getEpisode(1)).thenReturn(mockedEpisode);
        when(mockedEpisode.getId()).thenReturn(1);
        when(mockedEpisode.getStatus()).thenReturn(PodcastStatus.DELETED);

        // when
        ReflectionTestUtils.invokeMethod(podcastService, "doDownloadEpisode", mockedEpisode);

        // then
        verify(podcastDao, never()).updateEpisode(any());
    }

    @ParameterizedTest
    @EnumSource(value = PodcastStatus.class, names = {"DOWNLOADING", "COMPLETED"})
    public void testDoDownloadEpisodeWithDownloadingOrCompletedEpisodeShouldDoNothing(PodcastStatus status) {
        // given
        when(podcastDao.getEpisode(1)).thenReturn(mockedEpisode);
        when(mockedEpisode.getId()).thenReturn(1);
        when(mockedEpisode.getStatus()).thenReturn(status);

        // when
        ReflectionTestUtils.invokeMethod(podcastService, "doDownloadEpisode", mockedEpisode);

        // then
        verify(podcastDao, never()).updateEpisode(any());
    }

    @Test
    public void testDoDonwloadEpisodeWithExceptionShouldSetErrorStatus() throws Exception {

        // given
        when(podcastDao.getEpisode(1)).thenReturn(mockedEpisode);
        when(mockedEpisode.getId()).thenReturn(1);
        when(mockedEpisode.getStatus()).thenReturn(PodcastStatus.NEW);
        when(mockedEpisode.getChannelId()).thenReturn(1);
        when(mockedEpisode.getUrl()).thenReturn("http://test.com/test.mp3");
        when(podcastService.getChannel(1)).thenReturn(mockedChannel);
        when(mockedChannel.getMediaFileId()).thenReturn(2);
        when(mediaFileService.getMediaFile(2)).thenReturn(mockedMediaFile);
        when(mockedMediaFile.getFolderId()).thenReturn(1);
        when(mediaFolderService.getMusicFolderById(1)).thenReturn(mockedMusicFolder);
        when(mockedMusicFolder.getPath()).thenReturn(tempFolder);
        when(mockedMediaFile.getFullPath(tempFolder)).thenReturn(tempFolder);
        when(securityService.isWriteAllowed(Paths.get("test.mp3"), mockedMusicFolder)).thenReturn(true);
        when(versionService.getLocalVersion()).thenReturn(new Version("1.0.0"));

        // when
        try (MockedStatic<HttpClients> mockedHttpClients = Mockito.mockStatic(HttpClients.class, Mockito.CALLS_REAL_METHODS)) {
            mockedHttpClients.when(() -> HttpClients.createDefault()).thenReturn(mockedHttpClient);
            when(mockedHttpClient.execute(any())).thenThrow(new IOException("test"));
            ReflectionTestUtils.invokeMethod(podcastService, "doDownloadEpisode", mockedEpisode);
        }

        // then
        verify(podcastDao, times(2)).updateEpisode(mockedEpisode);
        verify(mockedEpisode).setStatus(PodcastStatus.ERROR);
        verify(mockedEpisode).setErrorMessage("test");
    }

    // testGetEpisode

    @ParameterizedTest
    @ValueSource(strings = {"true", "false"})
    public void testGetEpisodeWithNotFoundShouldReturnNull(boolean includeDeleted) {
        // given
        when(podcastDao.getEpisode(1)).thenReturn(null);

        // when
        PodcastEpisode episode = podcastService.getEpisode(1, includeDeleted);

        // then
        assertNull(episode);
        verify(podcastDao).getEpisode(1);
    }

    @Test
    public void testGetEpisodeWithDeletedEpisodeShouldReturnNull() {
        // given
        when(podcastDao.getEpisode(1)).thenReturn(mockedEpisode);
        when(mockedEpisode.getStatus()).thenReturn(PodcastStatus.DELETED);

        // when
        PodcastEpisode episode = podcastService.getEpisode(1, false);

        // then
        assertNull(episode);
        verify(podcastDao).getEpisode(1);
    }

    @ParameterizedTest
    @EnumSource(value = PodcastStatus.class, mode = Mode.EXCLUDE, names = {"DELETED"})
    public void testGetEpisodeWithExcludeDeleteShouldREturnEpisode(PodcastStatus status) {
        // given
        when(podcastDao.getEpisode(1)).thenReturn(mockedEpisode);
        when(mockedEpisode.getStatus()).thenReturn(status);

        // when
        PodcastEpisode episode = podcastService.getEpisode(1, false);

        // then
        assertEquals(mockedEpisode, episode);
        verify(podcastDao).getEpisode(1);
    }

    @ParameterizedTest
    @EnumSource(value = PodcastStatus.class)
    public void testGetEpisodeWithIncludeDeletedShouldReturnEpisode(PodcastStatus status) {
        // given
        when(podcastDao.getEpisode(1)).thenReturn(mockedEpisode);
        when(mockedEpisode.getStatus()).thenReturn(status);

        // when
        PodcastEpisode episode = podcastService.getEpisode(1, true);

        // then
        assertEquals(mockedEpisode, episode);
        verify(podcastDao).getEpisode(1);
    }

    // testGetEpisodes
    @Test
    void testGetEpisodesWithNullIdShouldReturnEmptyList() {

        // when
        List<PodcastEpisode> episodes = podcastService.getEpisodes(null);

        // then
        assertEquals(0, episodes.size());
        verify(podcastDao, never()).getEpisodes(anyInt());
    }

    @Test
    void testGetEpisodesWithIdShouldReturnListWithNullMediaId() {
        // given
        when(podcastDao.getEpisodes(1)).thenReturn(Arrays.asList(mockedEpisode));
        when(mockedEpisode.getMediaFileId()).thenReturn(null);

        // when
        List<PodcastEpisode> episodes = podcastService.getEpisodes(1);

        // then
        assertEquals(1, episodes.size());
        assertEquals(mockedEpisode, episodes.get(0));
        verify(podcastDao).getEpisodes(1);
    }

    @Test
    void testGetEpisodesWithIdShouldReturnListWithAllowedMediaId() {
        // given
        when(podcastDao.getEpisodes(1)).thenReturn(Arrays.asList(mockedEpisode));
        // setup mediafile allowed
        when(mockedEpisode.getMediaFileId()).thenReturn(1);
        when(mediaFileService.getMediaFile(1)).thenReturn(mockedMediaFile);
        when(securityService.isReadAllowed(mockedMediaFile, false)).thenReturn(true);

        // when
        List<PodcastEpisode> episodes = podcastService.getEpisodes(1);

        // then
        assertEquals(1, episodes.size());
        assertEquals(mockedEpisode, episodes.get(0));
        verify(podcastDao).getEpisodes(1);
    }

    @Test
    void testGetEpisodesWithDisallowedMediafileIdShouldReturnEmptyList() {
        // given
        when(podcastDao.getEpisodes(1)).thenReturn(Arrays.asList(mockedEpisode));
        // setup mediafile allowed
        when(mockedEpisode.getMediaFileId()).thenReturn(1);
        when(mediaFileService.getMediaFile(1)).thenReturn(mockedMediaFile);
        when(securityService.isReadAllowed(mockedMediaFile, false)).thenReturn(false);

        // when
        List<PodcastEpisode> episodes = podcastService.getEpisodes(1);

        // then
        assertEquals(0, episodes.size());
        verify(podcastDao).getEpisodes(1);
    }

    // testGetNewestEpisodes
    @Test
    void testGetNewestEpisodes() {

        // given
        int count = 10;
        when(mediaFileService.getMediaFile(anyInt()))
            .thenReturn(null)
            .thenReturn(mockedMediaFile);
        when(mockedMediaFile.isPresent())
            .thenReturn(false)
            .thenReturn(true);
        when(podcastDao.getNewestEpisodes(count)).thenReturn(Arrays.asList(mockedEpisode, mockedEpisode, mockedEpisode));
        when(mockedEpisode.getMediaFileId()).thenReturn(1);

        // when
        List<PodcastEpisode> episodes = podcastService.getNewestEpisodes(count);

        // then
        assertEquals(1, episodes.size());
        verify(mediaFileService, times(3)).getMediaFile(anyInt());
    }

    @Test
    void testSchedule() {
        // given
        when(podcastDao.getAllChannelRules()).thenReturn(Arrays.asList(RULE_SCHEDULE, RULE_UNSCHEDULE));

        // config scheduleDefault to schedule
        when(settingsService.getPodcastUpdateInterval()).thenReturn(1);

        // prepare instant
        Instant now = Instant.parse("2020-01-01T00:00:00Z");
        Instant expectedFirstTime = Instant.parse("2020-01-01T00:05:00Z");


        try (MockedStatic<Instant> mockedInstant = Mockito.mockStatic(Instant.class, Mockito.CALLS_REAL_METHODS)) {
            mockedInstant.when(Instant::now).thenReturn(now);

            // when
            podcastService.schedule();

            // then
            verify(taskService).scheduleAtFixedRate(eq("podcast-channel-refresh--1"), any(), eq(expectedFirstTime), eq(Duration.ofHours(1)), eq(true)); // assert scheduleDefault is called
            assertUnschedule(2); // schedule rule 2
            verify(taskService).scheduleAtFixedRate(eq("podcast-channel-refresh-1"), any(), eq(expectedFirstTime), eq(Duration.ofHours(1)), eq(true));
        }
    }

    @Test
    void testScheduleDefaultWithoutIntervalConfigShouldUnshcedule() {
        // given
        when(settingsService.getPodcastUpdateInterval()).thenReturn(-1);

        // when
        podcastService.scheduleDefault();

        // then
        // unschedule should be called
        assertUnschedule(-1);
    }

    private void assertUnschedule(Integer id) {
        verify(taskService).unscheduleTask(eq("podcast-channel-refresh-" + id));
    }


    @Test
    void testScheduleDefaultWithIntervalConfigShouldSchedule() {
        // given
        when(settingsService.getPodcastUpdateInterval()).thenReturn(1);
        Instant now = Instant.parse("2020-01-01T00:00:00Z");
        Instant expectedFirstTime = Instant.parse("2020-01-01T00:05:00Z");

        // when
        try (MockedStatic<Instant> mockedInstant = Mockito.mockStatic(Instant.class, Mockito.CALLS_REAL_METHODS)) {
            mockedInstant.when(Instant::now).thenReturn(now);
            podcastService.scheduleDefault();

            // then
            verify(taskService).scheduleAtFixedRate(eq("podcast-channel-refresh--1"), any(), eq(expectedFirstTime), eq(Duration.ofHours(1)), eq(true));
            verify(taskService, never()).unscheduleTask(anyString());
        }
    }

}
