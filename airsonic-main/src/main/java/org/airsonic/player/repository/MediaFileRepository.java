package org.airsonic.player.repository;

import org.airsonic.player.domain.MediaFile;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, Integer> {

    List<MediaFile> findByPath(String path);

    List<MediaFile> findByFolderIdAndPath(Integer folderId, String path);

    List<MediaFile> findByFolderIdAndParentPath(Integer folderId, String parentPath, Sort sort);

    List<MediaFile> findByFolderIdAndParentPathAndPresentTrue(Integer folderId, String parentPath, Sort sort);

    Optional<MediaFile> findByPathAndFolderIdAndStartPosition(String path, Integer folderId, Double startPosition);
}
