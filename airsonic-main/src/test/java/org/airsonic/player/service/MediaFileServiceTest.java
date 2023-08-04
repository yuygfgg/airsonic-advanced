package org.airsonic.player.service;

import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MediaFile.MediaType;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.service.metadata.MetaDataParserFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MediaFileServiceTest {

    @Mock
    private MetaDataParserFactory metaDataParserFactory;
    @Mock
    private MediaFileDao mediaFileDao;
    @Mock
    private CoverArtService coverArtService;

    @InjectMocks
    private MediaFileService mediaFileService;

    @Mock
    private MusicFolder mockedFolder;

    private final Path CLASS_PATH = Paths.get("src", "test", "resources");

    @BeforeEach
    public void setUp() {
        when(mockedFolder.getPath()).thenReturn(CLASS_PATH.resolve("MEDIAS"));
    }

    @Test
    public void createIndexedTracksFailedByNoIndexTracksReturnEmptyList() {
        // prepare test data
        MediaFile base = new MediaFile();
        base.setIndexPath("invalidCue/airsonic-test.cue");
        base.setPath("valid/airsonic-test.wav");
        base.setMediaType(MediaType.MUSIC);
        base.setFormat("wav");

        // execute
        List<MediaFile> actual = ReflectionTestUtils.invokeMethod(mediaFileService, "createIndexedTracks", base, mockedFolder);

        // check empty list is returned
        assertTrue(actual.isEmpty());
        // verify updateMedia does not called
        verify(mediaFileDao).createOrUpdateMediaFile(any(), any());
        verify(mediaFileDao).deleteMediaFiles(any(), anyInt());
        verify(coverArtService).persistIfNeeded(eq(base));
    }
}
