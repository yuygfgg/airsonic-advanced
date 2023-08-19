package org.airsonic.player.service;

import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.CoverArt;
import org.airsonic.player.domain.CoverArt.EntityType;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.repository.CoverArtRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Service
@CacheConfig(cacheNames = "coverArtCache")
@Transactional
public class CoverArtService {
    @Autowired
    MediaFolderService mediaFolderService;
    @Autowired
    private CoverArtRepository coverArtRepository;

    @CacheEvict(key = "#art.entityType.toString().concat('-').concat(#art.entityId)")
    public void upsert(CoverArt art) {
        coverArtRepository.save(art);
    }

    public void persistIfNeeded(MediaFile mediaFile) {
        if (mediaFile.getArt() != null && !CoverArt.NULL_ART.equals(mediaFile.getArt())) {
            CoverArt art = get(EntityType.MEDIA_FILE, mediaFile.getId());
            if (CoverArt.NULL_ART.equals(art) || !art.getOverridden()) {
                mediaFile.getArt().setEntityId(mediaFile.getId());
                upsert(mediaFile.getArt());
            }
            mediaFile.setArt(null);
        }
    }

    public void persistIfNeeded(Album album) {
        if (album.getArt() != null && !CoverArt.NULL_ART.equals(album.getArt())) {
            CoverArt art = get(EntityType.ALBUM, album.getId());
            if (CoverArt.NULL_ART.equals(art) || !art.getOverridden()) {
                album.getArt().setEntityId(album.getId());
                upsert(album.getArt());
            }
            album.setArt(null);
        }
    }

    public void persistIfNeeded(Artist artist) {
        if (artist.getArt() != null && !CoverArt.NULL_ART.equals(artist.getArt())) {
            CoverArt art = get(EntityType.ARTIST, artist.getId());
            if (CoverArt.NULL_ART.equals(art) || !art.getOverridden()) {
                artist.getArt().setEntityId(artist.getId());
                upsert(artist.getArt());
            }
            artist.setArt(null);
        }
    }

    @Cacheable(key = "#type.toString().concat('-').concat(#id)", unless = "#result == null") // 'unless' condition should never happen, because of null-object pattern
    public CoverArt get(EntityType type, int id) {
        return coverArtRepository.findByEntityTypeAndEntityId(type, id).orElse(CoverArt.NULL_ART);
    }

    public Path getFullPath(EntityType type, int id) {
        CoverArt art = get(type, id);
        return getFullPath(art);
    }

    public Path getFullPath(CoverArt art) {
        if (art != null && !CoverArt.NULL_ART.equals(art)) {
            if (art.getFolderId() == null) {
                // null folder ids mean absolute paths
                return art.getRelativePath();
            } else {
                MusicFolder folder = mediaFolderService.getMusicFolderById(art.getFolderId());
                if (folder != null) {
                    return art.getFullPath(folder.getPath());
                }
            }
        }

        return null;
    }

    @CacheEvict(key = "#type.toString().concat('-').concat(#id)")
    public void delete(EntityType type, int id) {
        coverArtRepository.deleteByEntityTypeAndEntityId(type, id);
    }

    @CacheEvict(allEntries = true)
    public void expunge() {
        List<CoverArt> expungeCoverArts = coverArtRepository.findAll().stream()
            .filter(art ->
                (art.getEntityType() == EntityType.ALBUM && art.getAlbum() == null) ||
                (art.getEntityType() == EntityType.ARTIST && art.getArtist() == null) ||
                (art.getEntityType() == EntityType.MEDIA_FILE && art.getMediaFile() == null))
            .collect(Collectors.toList());
        coverArtRepository.deleteAll(expungeCoverArts);
    }
}
