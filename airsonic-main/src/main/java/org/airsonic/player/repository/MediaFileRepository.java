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
package org.airsonic.player.repository;

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MediaFile.MediaType;
import org.airsonic.player.domain.MusicFolder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, Integer> {

    public List<MediaFile> findByIdInAndFolderIdInAndMediaTypeAndPresentTrue(Iterable<Integer> ids, Iterable<Integer> folderIds, MediaType mediaType);

    public List<MediaFile> findByPath(String path);

    public List<MediaFile> findByLastScannedBeforeAndPresentTrue(Instant lastScanned);

    public List<MediaFile> findByMediaTypeAndPresentFalse(MediaType mediaType);

    public List<MediaFile> findByMediaTypeInAndPresentFalse(Iterable<MediaType> mediaTypes);

    public List<MediaFile> findByMediaTypeInAndArtistAndPresentTrue(List<MediaType> mediaTypes, String artist, Pageable page);

    public List<MediaFile> findByFolderIdAndPresentTrue(Integer folderId);

    public List<MediaFile> findByFolderAndPath(MusicFolder folder, String path);

    public List<MediaFile> findByFolderIdAndPathIn(Integer folderId, Iterable<String> path);

    public List<MediaFile> findByFolderIdInAndMediaTypeAndPresentTrue(List<Integer> folderIds, MediaType mediaType, Pageable page);

    public List<MediaFile> findByFolderIdInAndMediaTypeAndGenreAndPresentTrue(List<Integer> folderIds, MediaType mediaType, String genre, Pageable page);

    public List<MediaFile> findByFolderIdInAndMediaTypeInAndGenreAndPresentTrue(List<Integer> folderIds, Iterable<MediaType> mediaType, String genre, Pageable page);

    public List<MediaFile> findByFolderIdInAndMediaTypeAndYearBetweenAndPresentTrue(List<Integer> folderIds, MediaType mediaType, Integer startYear,
            Integer endYear, Pageable page);

    public List<MediaFile> findByFolderIdInAndMediaTypeAndPlayCountGreaterThanAndPresentTrue(List<Integer> folderIds, MediaType mediaType, Integer playCount, Pageable page);

    public List<MediaFile> findByFolderAndParentPath(MusicFolder folder, String parentPath, Sort sort);

    public List<MediaFile> findByFolderIdAndParentPath(Integer folderId, String parentPath, Sort sort);

    public List<MediaFile> findByFolderAndParentPathAndPresentTrue(MusicFolder folder, String parentPath, Sort sort);

    public Optional<MediaFile> findByFolderIdInAndMediaTypeAndArtistAndPresentTrue(List<Integer> folderIds, MediaType mediaType, String artist);

    public Optional<MediaFile> findByFolderIdInAndMediaTypeAndArtistAndTitleAndPresentTrue(List<Integer> folderIds, MediaType mediaType, String artist, String title);

    public List<MediaFile> findByAlbumArtistAndAlbumNameAndMediaTypeInAndPresentTrue(String albumArtist, String albumName,
            List<MediaType> mediaTypes, Sort sort);


    public Optional<MediaFile> findByPathAndFolderIdAndStartPosition(String path, Integer folderId, Double startPosition);

    public Optional<MediaFile> findByPathAndFolderAndStartPosition(String path, MusicFolder folder, Double startPosition);

    public int countByFolderId(Integer folderId);

    public int countByFolderIdInAndMediaTypeAndPresentTrue(List<Integer> folderIds, MediaType mediaType);

    public int countByFolderIdInAndMediaTypeAndPlayCountGreaterThanAndPresentTrue(List<Integer> folderIds, MediaType mediaType, Integer playCount);

    public void deleteAllByPresentFalse();
}
