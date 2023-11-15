package org.airsonic.player.repository;

import org.airsonic.player.domain.MediaFile.MediaType;
import org.airsonic.player.domain.entity.StarredMediaFile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StarredMediaFileRepository extends JpaRepository<StarredMediaFile, Integer> {

    public List<StarredMediaFile> findByUsernameAndMediaFileMediaTypeAndMediaFileFolderIdInAndMediaFilePresentTrue(
            String username, MediaType mediaType, List<Integer> folderIds, Pageable page);

    public List<StarredMediaFile> findByUsernameAndMediaFileMediaTypeInAndMediaFileFolderIdInAndMediaFilePresentTrue(
            String username, Iterable<MediaType> mediaType, List<Integer> folderIds, Pageable page);

}
