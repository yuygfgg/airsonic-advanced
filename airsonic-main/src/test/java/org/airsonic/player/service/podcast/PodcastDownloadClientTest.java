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
 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.airsonic.player.service.podcast;

import org.airsonic.player.config.AirsonicHomeConfig;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.PodcastChannel;
import org.airsonic.player.domain.PodcastEpisode;
import org.airsonic.player.domain.PodcastStatus;
import org.airsonic.player.domain.Version;
import org.airsonic.player.repository.PodcastEpisodeRepository;
import org.airsonic.player.service.MediaFolderService;
import org.airsonic.player.service.PodcastPersistenceService;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.VersionService;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ContextConfiguration;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PodcastTestConfig.class})
@EnableConfigurationProperties({AirsonicHomeConfig.class})
public class PodcastDownloadClientTest {

    @MockBean
    private PodcastEpisodeRepository podcastEpisodeRepository;

    @MockBean
    private PodcastPersistenceService podcastPersistenceService;

    @MockBean
    private MediaFolderService mediaFolderService;

    @MockBean
    private SecurityService securityService;

    @MockBean
    private VersionService versionService;

    @TempDir
    private Path tempFolder;

    @TempDir
    private static Path airsonicFolder;

    @Autowired
    private PodcastDownloadClient podcastDownloadClient;

    @Mock
    private PodcastEpisode mockedEpisode;

    @Mock
    private PodcastChannel mockedChannel;

    @Mock
    private MediaFile mockedMediaFile;

    @Mock
    private MusicFolder mockedMusicFolder;

    @Mock
    private CloseableHttpClient mockedHttpClient;

    @Mock
    private ThreadPoolTaskExecutor podcastDownloadThreadPool;

    @BeforeAll
    private static void init() {
        System.setProperty("airsonic.home", airsonicFolder.toString());
    }

    @Test
    void testDoDownloadEpisodeWithDeletedEpisodeShouldDoNothing() throws Exception {

        // given
        when(podcastPersistenceService.prepareDownloadEpisode(1)).thenReturn(null);

        // when
        podcastDownloadClient.downloadEpisode(1).get();

        // then
        verify(podcastEpisodeRepository, never()).save(any(PodcastEpisode.class));
    }

    @Test
    public void testDoDonwloadEpisodeWithExceptionShouldSetErrorStatus() throws Exception {

        // given
        when(podcastPersistenceService.prepareDownloadEpisode(1)).thenReturn(mockedEpisode);
        when(mockedEpisode.getChannel()).thenReturn(mockedChannel);
        when(mockedEpisode.getUrl()).thenReturn("http://test.com/test.mp3");
        when(mockedEpisode.getChannel()).thenReturn(mockedChannel);
        when(mockedChannel.getMediaFile()).thenReturn(mockedMediaFile);
        when(mockedMediaFile.getFolder()).thenReturn(mockedMusicFolder);
        when(mockedMusicFolder.getPath()).thenReturn(tempFolder);
        when(mockedMediaFile.getFullPath()).thenReturn(tempFolder);
        when(securityService.isWriteAllowed(Paths.get("test.mp3"), mockedMusicFolder)).thenReturn(true);
        when(versionService.getLocalVersion()).thenReturn(new Version("1.0.0"));

        // when
        try (MockedStatic<HttpClients> mockedHttpClients = Mockito.mockStatic(HttpClients.class, Mockito.CALLS_REAL_METHODS)) {
            mockedHttpClients.when(() -> HttpClients.createDefault()).thenReturn(mockedHttpClient);
            when(mockedHttpClient.execute(any())).thenThrow(new IOException("test"));
            podcastDownloadClient.downloadEpisode(1).get();
        }

        // then
        verify(podcastPersistenceService).updateEpisode(mockedEpisode);
        verify(mockedEpisode).setStatus(PodcastStatus.ERROR);
        verify(mockedEpisode).setErrorMessage("test");
    }

}
