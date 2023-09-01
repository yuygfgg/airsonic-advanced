package org.airsonic.player.repository;

import org.airsonic.player.domain.Album;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface AlbumRepository extends JpaRepository<Album, Integer> {

    public Optional<Album> findByArtistAndName(String artist, String name);

    public List<Album> findByName(String artist);

    public List<Album> findByArtistAndFolderIdInAndPresentTrue(String artist, Iterable<Integer> musicFolderIds);

    public List<Album> findByArtistAndFolderIdInAndPresentTrue(String artist, Iterable<Integer> musicFolderIds, Pageable pageable);

    public int countByFolderIdInAndPresentTrue(Iterable<Integer> musicFolderIds);

    public List<Album> findByFolderIdInAndPresentTrue(Iterable<Integer> musicFolderIds, Sort sort);

    public List<Album> findByFolderIdInAndPresentTrue(Iterable<Integer> musicFolderIds, Pageable pageable);

    public List<Album> findByGenreAndFolderIdInAndPresentTrue(String genre, Iterable<Integer> musicFolderIds, Pageable pageable);

    public List<Album> findByFolderIdInAndPlayCountGreaterThanAndPresentTrue(Iterable<Integer> musicFolderIds, int playCount, Pageable pageable);

    public List<Album> findByFolderIdInAndLastPlayedNotNullAndPresentTrue(Iterable<Integer> musicFolderIds, Pageable pageable);

    public List<Album> findByFolderIdInAndYearBetweenAndPresentTrue(Iterable<Integer> musicFolderIds, int startYear, int endYear, Pageable pageable);

    public List<Album> findByPresentFalse();

    public void deleteAllByPresentFalse();

    @Transactional
    @Modifying
    @Query("UPDATE Album a SET a.present = false WHERE a.lastScanned < :lastScanned")
    public void markNonPresent(@Param("lastScanned") Instant lastScanned);

}