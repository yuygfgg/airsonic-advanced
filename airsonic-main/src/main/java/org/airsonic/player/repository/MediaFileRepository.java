package org.airsonic.player.repository;

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MediaFile.MediaType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, Integer> {

    public List<MediaFile> findByPath(String path);

    public List<MediaFile> findByMediaTypeAndPresentFalse(MediaType mediaType);

    public List<MediaFile> findByMediaTypeInAndPresentFalse(Iterable<MediaType> mediaTypes);

    public List<MediaFile> findByFolderIdAndPath(Integer folderId, String path);

    public List<MediaFile> findByFolderIdInAndMediaTypeAndPresentTrue(List<Integer> folderIds, MediaType mediaType, Pageable page);

    public List<MediaFile> findByFolderIdAndParentPath(Integer folderId, String parentPath, Sort sort);

    public List<MediaFile> findByFolderIdAndParentPathAndPresentTrue(Integer folderId, String parentPath, Sort sort);

    public List<MediaFile> findByAlbumArtistAndAlbumNameAndMediaTypeInAndPresentTrue(String albumArtist, String albumName,
            List<MediaType> mediaTypes, Sort sort);

    public Optional<MediaFile> findByArtistAndMediaTypeAndFolderIdInAndPresentTrue(String artist, MediaType mediaType,
            List<Integer> folderIds);

    public Optional<MediaFile> findByPathAndFolderIdAndStartPosition(String path, Integer folderId, Double startPosition);

    public void deleteAllByPresentFalse();
}
