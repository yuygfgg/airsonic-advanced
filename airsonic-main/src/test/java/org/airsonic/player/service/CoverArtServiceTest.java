/*
 * This file is part of Airsonic
 *
 *  Airsonic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * Airsonic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 * Copyright 2023 (C) Y.Tory
 */

package org.airsonic.player.service;

import org.airsonic.player.dao.CoverArtDao;
import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.CoverArt;
import org.airsonic.player.domain.CoverArt.EntityType;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CoverArtServiceTest {

    @Mock
    private CoverArtDao coverArtDao;

    @Mock
    private MediaFolderService mediaFolderService;

    @InjectMocks
    private CoverArtService coverArtService;

    @Mock
    private MusicFolder mockedFolder;

    @Mock
    private MediaFile mockedMediaFile;

    @Mock
    private Album mockedAlbum;

    @Mock
    private Artist mockedArtist;

    @Captor
    private ArgumentCaptor<CoverArt> coverArtCaptor;


    @Test
    public void testGetReturnsCachedCoverArt() {
        CoverArt coverArt = new CoverArt(1, EntityType.MEDIA_FILE, "path/to/image.jpg", null, false);
        when(coverArtDao.get(EntityType.MEDIA_FILE, 1)).thenReturn(coverArt);
        CoverArt result = coverArtService.get(EntityType.MEDIA_FILE, 1);
        assertEquals(coverArt, result);
        verify(coverArtDao, times(1)).get(EntityType.MEDIA_FILE, 1);
    }

    @Test
    public void testGetReturnsNullForNonexistentCoverArt() {
        when(coverArtDao.get(EntityType.MEDIA_FILE, 1)).thenReturn(null);
        CoverArt result = coverArtService.get(EntityType.MEDIA_FILE, 1);
        assertEquals(CoverArt.NULL_ART, result);
        verify(coverArtDao, times(1)).get(EntityType.MEDIA_FILE, 1);
    }

    @ParameterizedTest
    @MethodSource("coverArtWithNullAndNULLART")
    public void testGetFullPathReturnsNullForNullCoverArt(CoverArt coverArt) {
        Path result = coverArtService.getFullPath(coverArt);
        assertNull(result);
        verify(mediaFolderService, never()).getMusicFolderById(anyInt());
    }

    @Test
    public void testGetFullPathReturnsRelativePathIfNoMusicFolderId() {
        CoverArt coverArt = new CoverArt(1, EntityType.MEDIA_FILE, "path/to/image.jpg", null, false);
        Path result = coverArtService.getFullPath(coverArt);
        assertEquals(Paths.get("path/to/image.jpg"), result);
        verify(mediaFolderService, never()).getMusicFolderById(anyInt());
    }

    @Test
    public void testGetFullPathReturnsNullForNullFolder() {
        CoverArt coverArt = new CoverArt(1, EntityType.MEDIA_FILE, "path/to/image.jpg", 1, false);
        when(mediaFolderService.getMusicFolderById(1)).thenReturn(null);
        Path result = coverArtService.getFullPath(coverArt);
        assertNull(result);
        verify(mediaFolderService, times(1)).getMusicFolderById(1);
    }

    @Test
    public void testGetFullPathReturnsFullPath() {
        CoverArt coverArt = new CoverArt(1, EntityType.MEDIA_FILE, "path/to/image.jpg", 1, false);
        when(mediaFolderService.getMusicFolderById(1)).thenReturn(mockedFolder);
        when(mockedFolder.getPath()).thenReturn(Paths.get("music"));
        Path result = coverArtService.getFullPath(coverArt);
        assertEquals(Paths.get("music/path/to/image.jpg"), result);
        verify(mediaFolderService, times(1)).getMusicFolderById(1);
    }

    @Test
    public void testGetFullPathWithTypeAndId() {
        CoverArt coverArt = new CoverArt(1, EntityType.MEDIA_FILE, "path/to/image.jpg", 1, false);
        when(mediaFolderService.getMusicFolderById(1)).thenReturn(mockedFolder);
        when(mockedFolder.getPath()).thenReturn(Paths.get("music"));
        when(coverArtDao.get(EntityType.MEDIA_FILE, 1)).thenReturn(coverArt);
        Path result = coverArtService.getFullPath(EntityType.MEDIA_FILE, 1);
        assertEquals(Paths.get("music/path/to/image.jpg"), result);
        verify(mediaFolderService, times(1)).getMusicFolderById(1);
    }

    private static Stream<CoverArt> coverArtWithNullAndNULLART() {
        return Stream.of(null, CoverArt.NULL_ART);
    }

    @Test
    public void testUpsertWithCoverArt() {
        CoverArt coverArt = new CoverArt(1, EntityType.MEDIA_FILE, "path/to/image.jpg", 1, false);
        coverArtService.upsert(coverArt);
        verify(coverArtDao, times(1)).upsert(coverArt);
    }

    @Test
    public void testUpsertWithCoverArtProperties() {
        coverArtService.upsert(EntityType.MEDIA_FILE, 1, "path/to/image.jpg", 1, false);
        verify(coverArtDao, times(1)).upsert(coverArtCaptor.capture());
        CoverArt coverArt = coverArtCaptor.getValue();
        assertEquals(1, coverArt.getEntityId());
        assertEquals(EntityType.MEDIA_FILE, coverArt.getEntityType());
        assertEquals("path/to/image.jpg", coverArt.getPath());
        assertEquals(1, coverArt.getFolderId());
        assertEquals(false, coverArt.getOverridden());
    }

    @Test
    void testExpunge() {
        coverArtService.expunge();
        verify(coverArtDao, times(1)).expunge();
    }

    @Test
    void testDelete() {
        coverArtService.delete(EntityType.MEDIA_FILE, 1);
        verify(coverArtDao, times(1)).delete(EntityType.MEDIA_FILE, 1);
    }


    @ParameterizedTest
    @MethodSource("coverArtWithNullAndNULLART")
    void testPersistIfNeededWithMediaFileWithoutCoverArtShouldDoNothing(CoverArt coverArt) {
        when(mockedMediaFile.getArt()).thenReturn(coverArt);
        coverArtService.persistIfNeeded(mockedMediaFile);
        verify(coverArtDao, never()).upsert(any());
    }

    @Test
    void testPersistIfNeededWithMediaFileShouldNotOverrideOverriddenCoverArt() {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setId(1);

        CoverArt existingCoverArt = new CoverArt(1, EntityType.MEDIA_FILE, "path/to/art.jpg", null, true);
        when(coverArtDao.get(EntityType.MEDIA_FILE, 1)).thenReturn(existingCoverArt);

        CoverArt artToPersist = new CoverArt(1, EntityType.MEDIA_FILE, "path/to/updated-art.jpg", null, false);
        mediaFile.setArt(artToPersist);

        coverArtService.persistIfNeeded(mediaFile);

        verify(coverArtDao).get(EntityType.MEDIA_FILE, 1);
        verify(coverArtDao, never()).upsert(any(CoverArt.class));
        assertNull(mediaFile.getArt());
    }

    @ParameterizedTest
    @MethodSource("coverArtWillBeOverriddenForMediaFile")
    void testPersistIfNeededWithMediaFileShouldOverrideExistingCoverArt(CoverArt existingCoverArt) {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setId(1);

        when(coverArtDao.get(EntityType.MEDIA_FILE, 1)).thenReturn(existingCoverArt);

        CoverArt artToPersist = new CoverArt(-1, EntityType.MEDIA_FILE, "path/to/updated-art.jpg", null, false);
        mediaFile.setArt(artToPersist);

        coverArtService.persistIfNeeded(mediaFile);

        verify(coverArtDao).get(EntityType.MEDIA_FILE, 1);
        verify(coverArtDao).upsert(coverArtCaptor.capture());
        assertNull(mediaFile.getArt());
        CoverArt persistedCoverArt = coverArtCaptor.getValue();
        assertEquals(EntityType.MEDIA_FILE, persistedCoverArt.getEntityType());
        assertEquals(mediaFile.getId(), persistedCoverArt.getEntityId());
        assertEquals("path/to/updated-art.jpg", persistedCoverArt.getPath());
        assertFalse(persistedCoverArt.getOverridden());
    }

    private static Stream<CoverArt> coverArtWillBeOverriddenForMediaFile() {
        return Stream.of(new CoverArt(1, EntityType.MEDIA_FILE, "path/to/art.jpg", null, false), CoverArt.NULL_ART);
    }

    @ParameterizedTest
    @MethodSource("coverArtWithNullAndNULLART")
    void testPersistIfNeededWithAlbumWithoutCoverArtShouldDoNothing(CoverArt coverArt) {
        when(mockedAlbum.getArt()).thenReturn(coverArt);
        coverArtService.persistIfNeeded(mockedAlbum);
        verify(coverArtDao, never()).upsert(any());
    }

    @Test
    void testPersistIfNeededWithAlbumShouldNotOverrideOverriddenCoverArt() {
        Album album = new Album();
        album.setId(1);

        CoverArt existingCoverArt = new CoverArt(1, EntityType.ALBUM, "path/to/art.jpg", null, true);
        when(coverArtDao.get(EntityType.ALBUM, 1)).thenReturn(existingCoverArt);

        CoverArt artToPersist = new CoverArt(1, EntityType.ALBUM, "path/to/updated-art.jpg", null, false);
        album.setArt(artToPersist);

        coverArtService.persistIfNeeded(album);

        verify(coverArtDao).get(EntityType.ALBUM, 1);
        verify(coverArtDao, never()).upsert(any(CoverArt.class));
        assertNull(album.getArt());
    }

    @ParameterizedTest
    @MethodSource("coverArtWillBeOverriddenForAlbum")
    void testPersistIfNeededWithAlbumShouldOverrideExistingCoverArt(CoverArt existingCoverArt) {
        Album album = new Album();
        album.setId(1);

        when(coverArtDao.get(EntityType.ALBUM, 1)).thenReturn(existingCoverArt);

        CoverArt artToPersist = new CoverArt(-1, EntityType.ALBUM, "path/to/updated-art.jpg", null, false);
        album.setArt(artToPersist);

        coverArtService.persistIfNeeded(album);

        verify(coverArtDao).get(EntityType.ALBUM, 1);
        verify(coverArtDao).upsert(coverArtCaptor.capture());
        assertNull(album.getArt());
        CoverArt persistedCoverArt = coverArtCaptor.getValue();
        assertEquals(EntityType.ALBUM, persistedCoverArt.getEntityType());
        assertEquals(album.getId(), persistedCoverArt.getEntityId());
        assertEquals("path/to/updated-art.jpg", persistedCoverArt.getPath());
        assertFalse(persistedCoverArt.getOverridden());
    }

    private static Stream<CoverArt> coverArtWillBeOverriddenForAlbum() {
        return Stream.of(new CoverArt(1, EntityType.ALBUM, "path/to/art.jpg", null, false), CoverArt.NULL_ART);
    }

    @ParameterizedTest
    @MethodSource("coverArtWithNullAndNULLART")
    void testPersistIfNeededWithArtistWithoutCoverArtShouldDoNothing(CoverArt coverArt) {
        when(mockedArtist.getArt()).thenReturn(coverArt);
        coverArtService.persistIfNeeded(mockedArtist);
        verify(coverArtDao, never()).upsert(any());
    }

    @Test
    void testPersistIfNeededWithArtistShouldNotOverrideOverriddenCoverArt() {
        Artist artist = new Artist();
        artist.setId(1);

        CoverArt existingCoverArt = new CoverArt(1, EntityType.ARTIST, "path/to/art.jpg", null, true);
        when(coverArtDao.get(EntityType.ARTIST, 1)).thenReturn(existingCoverArt);

        CoverArt artToPersist = new CoverArt(1, EntityType.ARTIST, "path/to/updated-art.jpg", null, false);
        artist.setArt(artToPersist);

        coverArtService.persistIfNeeded(artist);

        verify(coverArtDao).get(EntityType.ARTIST, 1);
        verify(coverArtDao, never()).upsert(any(CoverArt.class));
        assertNull(artist.getArt());
    }

    @ParameterizedTest
    @MethodSource("coverArtWillBeOverriddenForArtist")
    void testPersistIfNeededWithArtistShouldOverrideExistingCoverArt(CoverArt existingCoverArt) {
        Artist artist = new Artist();
        artist.setId(1);

        when(coverArtDao.get(EntityType.ARTIST, 1)).thenReturn(existingCoverArt);

        CoverArt artToPersist = new CoverArt(-1, EntityType.ARTIST, "path/to/updated-art.jpg", null, false);
        artist.setArt(artToPersist);

        coverArtService.persistIfNeeded(artist);

        verify(coverArtDao).get(EntityType.ARTIST, 1);
        verify(coverArtDao).upsert(coverArtCaptor.capture());
        assertNull(artist.getArt());
        CoverArt persistedCoverArt = coverArtCaptor.getValue();
        assertEquals(EntityType.ARTIST, persistedCoverArt.getEntityType());
        assertEquals(artist.getId(), persistedCoverArt.getEntityId());
        assertEquals("path/to/updated-art.jpg", persistedCoverArt.getPath());
        assertFalse(persistedCoverArt.getOverridden());
    }

    private static Stream<CoverArt> coverArtWillBeOverriddenForArtist() {
        return Stream.of(new CoverArt(1, EntityType.ARTIST, "path/to/art.jpg", null, false), CoverArt.NULL_ART);
    }


    @Test
    void testPersistIfNeeded2() {

    }

    @Test
    void testPersistIfNeeded3() {

    }

}
