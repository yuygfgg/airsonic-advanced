package org.airsonic.player.service;

import org.airsonic.player.config.AirsonicScanConfig;
import org.airsonic.player.dao.AlbumDao;
import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.domain.MediaLibraryStatistics;
import org.airsonic.player.repository.ArtistRepository;
import org.airsonic.player.service.search.IndexManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.Assert.*;
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
    private ArtistRepository artistRepository;
    @Mock
    private AlbumDao albumDao;
    @Mock
    private TaskSchedulingService taskService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    IndexManager indexManager;
    @Mock
    AirsonicScanConfig scanConfig;


    @Test
    public void neverScanned() {
        when(settingsService.getIndexCreationInterval()).thenReturn(-1);
        when(settingsService.getIndexCreationHour()).thenReturn(-1);
        when(indexManager.getStatistics()).thenReturn(null);
        MediaScannerService mediaScannerService = new MediaScannerService(settingsService, indexManager, playlistService, mediaFileService, mediaFolderService, coverArtService, mediaFileDao, artistRepository, albumDao, taskService, messagingTemplate, scanConfig);
        assertTrue(mediaScannerService.neverScanned());

        when(indexManager.getStatistics()).thenReturn(new MediaLibraryStatistics());
        assertFalse(mediaScannerService.neverScanned());
    }
}
