package org.airsonic.player.service;

import org.airsonic.player.config.AirsonicHomeConfig;
import org.airsonic.player.dao.DatabaseDao;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatabaseServiceTest {

    @Mock
    private SettingsService settingsService;

    @Mock
    private DatabaseDao databaseDao;

    @Mock
    private TaskSchedulingService taskService;

    @Mock
    private AirsonicHomeConfig homeConfig;

    @Mock
    private SimpMessagingTemplate brokerTemplate;

    @Spy
    private Runnable backupTask = new Runnable() {
        @Override
        public void run() {
            // empty implementation
        }
    };


    @InjectMocks
    private DatabaseService databaseService;

    @TempDir
    private static Path tempDir;

    // Mock LocalDateTime.now() to return a fixed value
    private static final LocalDateTime LOCAL_DATE = LocalDateTime.of(2020, 1, 1, 0, 0, 0);

    private final String BACKUP_DIR = "airsonic.exportDB.20200101000000";


    private static MockedStatic<LocalDateTime> mockedLocalDateTime;

    @BeforeAll
    public static void beforeAll() {
        // Mock LocalDateTime.now() to return a fixed value
        mockedLocalDateTime = mockStatic(LocalDateTime.class, Mockito.CALLS_REAL_METHODS);
        mockedLocalDateTime.when(LocalDateTime::now).thenReturn(LOCAL_DATE);
    }

    @AfterAll
    public static void afterAll() {
        mockedLocalDateTime.close();
    }

    @Test
    void testSchedule() throws Exception {
        // Set up mock behavior
        when(settingsService.getDbBackupInterval()).thenReturn(3);

        // Call the method to be tested
        databaseService.init();

        // Verify expected behavior
        verify(taskService).scheduleAtFixedRate(eq("db-backup"), eq(backupTask), any(Instant.class),
                eq(Duration.ofHours(3)), eq(true));
    }

    @Test
    void testScheduleDisabled() throws Exception {
        // Set up mock behavior
        when(settingsService.getDbBackupInterval()).thenReturn(-1);

        // Call the method to be tested
        databaseService.init();

        // Verify expected behavior
        verify(taskService, never()).scheduleAtFixedRate(anyString(), any(Runnable.class), any(Instant.class),
                any(Duration.class), anyBoolean());
    }

    @Test
    void testUnschedule() throws Exception {
        // Call the method to be tested
        databaseService.unschedule();

        // Verify expected behavior
        verify(taskService).unscheduleTask(eq("db-backup"));
    }

    @Test
    void testBackuppable() throws Exception {
        // Set up mock behavior
        when(settingsService.getDatabaseUrl()).thenReturn("jdbc:hsqldb:file:/path/to/db");

        // Call the method to be tested
        boolean result = databaseService.backuppable();

        // Verify expected behavior
        assertTrue(result);
    }

    @Test
    void testExportDB() throws Exception {
        // Arrange
        Path path = tempDir.resolve("backups").resolve(BACKUP_DIR);
        Path zipPath = path.resolve(BACKUP_DIR + ".zip");

        when(homeConfig.getAirsonicHome()).thenReturn(tempDir);
        when(databaseDao.exportDB(eq(path), any())).thenAnswer(invocation -> {
            Path dbPath = invocation.getArgument(0);
            dbPath.toFile().mkdirs();
            dbPath.resolve("test.xml").toFile().createNewFile();
            return true;
        });

        // Act
        Path actual = databaseService.exportDB();

        // Assert
        assertNotNull(actual);
        assertEquals(zipPath, actual);
        verify(databaseDao).exportDB(eq(path), any());
        verify(brokerTemplate, times(3)).convertAndSend(anyString(), anyString());
    }

    @Test
    void testExportDBException() throws Exception {
        // Arrange
        Path path = tempDir.resolve("backups").resolve(BACKUP_DIR);

        when(homeConfig.getAirsonicHome()).thenReturn(tempDir);
        when(databaseDao.exportDB(eq(path), any())).thenThrow(new Exception());

        // Act
        Path actual = databaseService.exportDB();

        // Assert
        assertNull(actual);
        verify(databaseDao).exportDB(eq(path), any());
        verify(brokerTemplate, times(3)).convertAndSend(anyString(), anyString());
        assertFalse(path.toFile().exists());
    }

    @Test
    void testCleanup() throws Exception {
        // Arrange
        Path directory = tempDir.resolve(BACKUP_DIR);
        Path file = tempDir.resolve("test").resolve(BACKUP_DIR + ".zip");

        directory.toFile().mkdirs();
        file.toFile().getParentFile().mkdirs();
        file.toFile().createNewFile();

        // Act
        databaseService.cleanup(directory);
        databaseService.cleanup(file);

        // Assert
        assertFalse(directory.toFile().exists());
        assertFalse(file.toFile().exists());
    }

    @Test
    void testImportDB() throws Exception {
        // Arrange
        Path filePath = Files.createTempFile(tempDir, "airsonic.exportDB", ".xml");
        when(settingsService.getDatabaseUrl()).thenReturn(String.format("jdbc:hsqldb:file:%s;", filePath));
        doNothing().when(databaseDao).importDB(any());

        // Act
        databaseService.importDB(tempDir);

        // Assert
        assertFalse(filePath.toFile().exists());
        verify(databaseDao).importDB(any());
        verify(brokerTemplate, times(7)).convertAndSend(anyString(), anyString());
    }

    @ParameterizedTest
    @MethodSource("getFiles")
    void testImportDBNotDirectory(File file) throws Exception {
        // Arrange
        Path path = file.toPath();

        // Act
        databaseService.importDB(path);

        // Assert
        verify(databaseDao, never()).importDB(any());
        verify(brokerTemplate).convertAndSend(anyString(), eq("Nothing imported"));
        verify(brokerTemplate, times(3)).convertAndSend(anyString(), anyString());

    }

    private static Stream<File> getFiles() throws Exception {
        File file = Files.createTempFile("test", ".zip").toFile();
        return Stream.of(
            new File("/tmp/nonexistent"),
            file,
            tempDir.toFile()
        );
    }
}
