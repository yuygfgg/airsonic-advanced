package org.airsonic.player.service;

import org.airsonic.player.config.AirsonicScanConfig;
import org.airsonic.player.domain.MediaLibraryStatistics;
import org.airsonic.player.repository.AlbumRepository;
import org.airsonic.player.repository.ArtistRepository;
import org.airsonic.player.service.search.IndexManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class MediaScannerServiceUnitTest {

    @Mock
    private SettingsService settingsService;
    @Mock
    private PlaylistFileService playlistFileService;
    @Mock
    private MediaFileService mediaFileService;
    @Mock
    private MediaFolderService mediaFolderService;
    @Mock
    private CoverArtService coverArtService;
    @Mock
    private ArtistRepository artistRepository;
    @Mock
    private AlbumRepository albumRepository;
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
        MediaScannerService mediaScannerService = new MediaScannerService(settingsService, indexManager, playlistFileService, mediaFileService, mediaFolderService, coverArtService, artistRepository, albumRepository, taskService, messagingTemplate, scanConfig);
        assertTrue(mediaScannerService.neverScanned());

        when(indexManager.getStatistics()).thenReturn(new MediaLibraryStatistics());
        assertFalse(mediaScannerService.neverScanned());
    }
}
