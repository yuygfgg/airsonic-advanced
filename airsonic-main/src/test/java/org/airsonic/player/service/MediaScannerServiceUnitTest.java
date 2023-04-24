package org.airsonic.player.service;

import org.airsonic.player.dao.AlbumDao;
import org.airsonic.player.dao.ArtistDao;
import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.domain.MediaLibraryStatistics;
import org.airsonic.player.service.search.IndexManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.env.Environment;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MediaScannerServiceUnitTest {

    @Mock
    private SettingsService settingsService;
    @Mock
    private PlaylistService playlistService;
    @Mock
    private MediaFileService mediaFileService;
    @Mock
    private MediaFolderService mediaFolderService;
    @Mock
    private CoverArtService coverArtService;
    @Mock
    private MediaFileDao mediaFileDao;
    @Mock
    private ArtistDao artistDao;
    @Mock
    private AlbumDao albumDao;
    @Mock
    private TaskSchedulingService taskService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    IndexManager indexManager;
    @Mock
    Environment environment;


    @Test
    public void neverScanned() {
        when(environment.getProperty(eq("MediaScannerParallelism"), anyString())).thenReturn("1");
        when(settingsService.getIndexCreationInterval()).thenReturn(-1);
        when(settingsService.getIndexCreationHour()).thenReturn(-1);
        when(indexManager.getStatistics()).thenReturn(null);
        MediaScannerService mediaScannerService = new MediaScannerService(settingsService, indexManager, playlistService, mediaFileService, mediaFolderService, coverArtService, mediaFileDao, artistDao, albumDao, taskService, messagingTemplate, environment);
        assertTrue(mediaScannerService.neverScanned());

        when(indexManager.getStatistics()).thenReturn(new MediaLibraryStatistics());
        assertFalse(mediaScannerService.neverScanned());
    }
}
