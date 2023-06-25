package org.airsonic.player.service;



import org.airsonic.player.TestCaseUtils;
import org.airsonic.player.api.ScanningTestUtils;
import org.airsonic.player.config.AirsonicScanConfig;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.MusicFolder.Type;
import org.airsonic.player.util.MusicFolderTestData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@TestPropertySource(properties = {
    "airsonic.cue.enabled=true",
    "airsonic.scan.parallelism=1"
})
@SpringBootTest
public class MediaScannerServiceTest {

    @Autowired
    private MediaScannerService mediaScannerService;

    @SpyBean
    private MediaFileService mediaFileService;

    @SpyBean
    private SettingsService settingsService;

    @SpyBean
    private AirsonicScanConfig scanConfig;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MediaFolderService mediaFolderService;

    @TempDir
    private static Path tempDir;

    @BeforeAll
    public static void setUp() {
        System.setProperty("airsonic.home", tempDir.toString());
    }

    @BeforeEach
    public void setup() {
        jdbcTemplate.execute("DELETE FROM media_file");
        jdbcTemplate.execute("DELETE FROM album");
        jdbcTemplate.execute("DELETE FROM artist");
        TestCaseUtils.waitForScanFinish(mediaScannerService);
    }

    @AfterEach
    public void cleanup() {
        if (cleanupId != null) {
            ScanningTestUtils.after(cleanupId, mediaFolderService);
            cleanupId = null;
        }
    }

    private UUID cleanupId = null;

    @Test
    public void testMusicFullScanTimeOut() {
        when(settingsService.getFullScan()).thenReturn(true);
        when(settingsService.getIgnoreSymLinks()).thenReturn(false);
        when(scanConfig.getFullTimeout()).thenReturn(1);
        doAnswer(invocation -> {
            Thread.sleep(10000);
            return null;
        }).when(mediaFileService).setMemoryCacheEnabled(anyBoolean());

        // Add the "loop" folder to the database
        Path musicFolderFile = MusicFolderTestData.resolveMusicLoopFolderPath();
        MusicFolder musicFolder = new MusicFolder(2, musicFolderFile, "loop", Type.MEDIA, true, Instant.now().truncatedTo(ChronoUnit.MICROS));
        long start = System.currentTimeMillis();
        cleanupId = ScanningTestUtils.before(Arrays.asList(musicFolder), mediaFolderService, mediaScannerService);
        long end = System.currentTimeMillis();
        // Test that the scan time out is respected
        assertTrue(end - start < 10000);
    }

    @Test
    public void testMusicScanTimeOut() {
        when(settingsService.getFullScan()).thenReturn(false);
        when(settingsService.getIgnoreSymLinks()).thenReturn(false);
        when(scanConfig.getTimeout()).thenReturn(1);
        doAnswer(invocation -> {
            Thread.sleep(10000);
            return null;
        }).when(mediaFileService).setMemoryCacheEnabled(anyBoolean());

        // Add the "loop" folder to the database
        Path musicFolderFile = MusicFolderTestData.resolveMusicLoopFolderPath();
        MusicFolder musicFolder = new MusicFolder(2, musicFolderFile, "loop", Type.MEDIA, true, Instant.now().truncatedTo(ChronoUnit.MICROS));
        long start = System.currentTimeMillis();
        cleanupId = ScanningTestUtils.before(Arrays.asList(musicFolder), mediaFolderService, mediaScannerService);
        long end = System.currentTimeMillis();
        // Test that the scan time out is respected
        assertTrue(end - start < 10000);
    }
}
