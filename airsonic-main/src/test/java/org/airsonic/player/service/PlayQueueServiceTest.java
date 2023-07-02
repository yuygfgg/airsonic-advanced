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

import com.google.common.collect.ImmutableMap;
import org.airsonic.player.dao.PlayQueueDao;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.PlayQueue;
import org.airsonic.player.domain.Player;
import org.airsonic.player.domain.SavedPlayQueue;
import org.airsonic.player.service.websocket.AsyncWebSocketClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PlayQueueServiceTest {

    @Mock
    private JukeboxService jukeboxService;
    @Mock
    private AsyncWebSocketClient webSocketClient;
    @Mock
    private MediaFileService mediaFileService;
    @Mock
    private PlayQueueDao playQueueDao;

    @InjectMocks
    private PlayQueueService playQueueService;

    @Mock
    private Player mockedPlayer;
    @Mock
    private PlayQueue mockedPlayQueue;

    private ScheduledThreadPoolExecutor executor;

    @BeforeEach
    public void setup() {
        executor = new ScheduledThreadPoolExecutor(1);
    }

    @AfterEach
    public void teardown() {
        executor.shutdown();
    }



    @Nested
    class NonJukeboxTests {

        @BeforeEach
        public void setup() {
            when(mockedPlayer.isJukebox()).thenReturn(false);
        }

        @Test
        public void testStart() throws Exception {
            // given
            when(mockedPlayer.getPlayQueue()).thenReturn(mockedPlayQueue);
            when(mockedPlayer.getUsername()).thenReturn("testuser");
            when(mockedPlayer.getId()).thenReturn(1);

            // Verify websocketClient is called to send message
            when(webSocketClient.sendToUser(anyString(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));

            // Test
            playQueueService.start(mockedPlayer);

            // then
            verify(mockedPlayQueue).setStatus(PlayQueue.Status.PLAYING);
            verify(webSocketClient).sendToUser("testuser", "/queue/playqueues/1/playstatus", PlayQueue.Status.PLAYING);
        }

        @Test
        public void testStop() throws Exception {
            // given
            when(mockedPlayer.getPlayQueue()).thenReturn(mockedPlayQueue);
            when(mockedPlayer.getUsername()).thenReturn("testuser");
            when(mockedPlayer.getId()).thenReturn(1);

            // Verify websocketClient is called to send message
            when(webSocketClient.sendToUser(anyString(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));

            // Test
            playQueueService.stop(mockedPlayer);

            // then
            verify(mockedPlayQueue).setStatus(PlayQueue.Status.STOPPED);
            verify(webSocketClient).sendToUser("testuser", "/queue/playqueues/1/playstatus", PlayQueue.Status.STOPPED);
        }

        @ParameterizedTest
        @CsvSource({
            "PLAYING, STOPPED",
            "STOPPED, PLAYING"
        })
        public void testToggleStartStop(PlayQueue.Status initialStatus, PlayQueue.Status expectedStatus) throws Exception {
            // given
            when(mockedPlayer.getPlayQueue()).thenReturn(mockedPlayQueue);
            when(mockedPlayer.getUsername()).thenReturn("testuser");
            when(mockedPlayer.getId()).thenReturn(1);
            when(mockedPlayQueue.getStatus()).thenReturn(initialStatus);

            // Verify websocketClient is called to send message
            when(webSocketClient.sendToUser(anyString(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));

            // Test
            playQueueService.toggleStartStop(mockedPlayer);

            // then
            verify(mockedPlayQueue).setStatus(expectedStatus);
            verify(webSocketClient).sendToUser("testuser", "/queue/playqueues/1/playstatus", expectedStatus);
        }

        @Test
        public void testSkip() throws Exception {
            // given
            when(mockedPlayer.getPlayQueue()).thenReturn(mockedPlayQueue);
            when(mockedPlayer.getUsername()).thenReturn("testuser");
            when(mockedPlayer.getId()).thenReturn(1);
            when(mockedPlayQueue.getStatus()).thenReturn(PlayQueue.Status.PLAYING);
            when(webSocketClient.sendToUser(anyString(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));

            // when
            playQueueService.skip(mockedPlayer, 2, 3L);

            // then
            verify(webSocketClient).sendToUser("testuser", "/queue/playqueues/1/skip", ImmutableMap.of("index", 2, "offset", 3L));
            verify(webSocketClient).sendToUser("testuser", "/queue/playqueues/1/playstatus", PlayQueue.Status.PLAYING);
            verify(mockedPlayQueue).setIndex(2);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5})
    public void testSavePlayQueueWithIndex(int index) throws Exception {
        // given
        when(mockedPlayer.getPlayQueue()).thenReturn(mockedPlayQueue);
        List<MediaFile> files = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setId(i);
            files.add(mediaFile);
        }
        when(mockedPlayer.getUsername()).thenReturn("testuser");
        when(mockedPlayQueue.getFiles()).thenReturn(files);
        when(mockedPlayQueue.getFile(index)).thenReturn(files.get(index));
        doAnswer(invocation -> {
            SavedPlayQueue savedPlayQueue = invocation.getArgument(0, SavedPlayQueue.class);
            savedPlayQueue.setId(1);
            return null;
        }).when(playQueueDao).savePlayQueue(any());

        // when
        playQueueService.savePlayQueue(mockedPlayer, index, 10L);

        // then
        ArgumentCaptor<SavedPlayQueue> captor = ArgumentCaptor.forClass(SavedPlayQueue.class);
        verify(playQueueDao).savePlayQueue(captor.capture());
        SavedPlayQueue savedPlayQueue = captor.getValue();
        assertEquals("testuser", savedPlayQueue.getUsername());
        assertEquals(10L, savedPlayQueue.getPositionMillis());
        assertEquals(10, savedPlayQueue.getMediaFileIds().size());
        assertEquals(index, savedPlayQueue.getCurrentMediaFileId());
        assertNotNull(savedPlayQueue.getChanged());
        assertEquals("testuser", savedPlayQueue.getChangedBy());
    }

    @Test
    public void testSavePlayQueueWithMinusOneIndex() throws Exception {
        // given
        when(mockedPlayer.getPlayQueue()).thenReturn(mockedPlayQueue);
        List<MediaFile> files = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setId(i);
            files.add(mediaFile);
        }
        when(mockedPlayer.getUsername()).thenReturn("testuser");
        when(mockedPlayQueue.getFiles()).thenReturn(files);
        doAnswer(invocation -> {
            SavedPlayQueue savedPlayQueue = invocation.getArgument(0, SavedPlayQueue.class);
            savedPlayQueue.setId(1);
            return null;
        }).when(playQueueDao).savePlayQueue(any());

        // when
        playQueueService.savePlayQueue(mockedPlayer, -1, 10L);

        // then
        ArgumentCaptor<SavedPlayQueue> captor = ArgumentCaptor.forClass(SavedPlayQueue.class);
        verify(playQueueDao).savePlayQueue(captor.capture());
        verify(mockedPlayQueue, never()).getFile(anyInt());
        SavedPlayQueue savedPlayQueue = captor.getValue();
        assertEquals("testuser", savedPlayQueue.getUsername());
        assertEquals(10L, savedPlayQueue.getPositionMillis());
        assertEquals(10, savedPlayQueue.getMediaFileIds().size());
        assertNull(savedPlayQueue.getCurrentMediaFileId());
        assertNotNull(savedPlayQueue.getChanged());
        assertEquals("testuser", savedPlayQueue.getChangedBy());
    }

    // TODO: test methods include broadcastPlayQueue


}
