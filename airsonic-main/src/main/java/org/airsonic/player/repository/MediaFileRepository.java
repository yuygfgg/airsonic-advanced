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

    List<MediaFile> findByPath(String path);

    List<MediaFile> findByFolderIdAndPath(Integer folderId, String path);

    List<MediaFile> findByFolderIdInAndMediaTypeAndPresentTrue(List<Integer> folderIds, MediaType mediaType, Pageable page);

    List<MediaFile> findByFolderIdAndParentPath(Integer folderId, String parentPath, Sort sort);

    List<MediaFile> findByFolderIdAndParentPathAndPresentTrue(Integer folderId, String parentPath, Sort sort);

    List<MediaFile> findByAlbumArtistAndAlbumNameAndMediaTypeInAndPresentTrue(String albumArtist, String albumName,
            List<MediaType> mediaTypes, Sort sort);

    Optional<MediaFile> findByPathAndFolderIdAndStartPosition(String path, Integer folderId, Double startPosition);
}
