package org.airsonic.player.service.cache;

import org.airsonic.player.domain.CoverArt;
import org.airsonic.player.spring.CacheConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.cache.CacheManager;

@Component
public class CoverArtCache {

    @Autowired
    private CacheManager cacheManager;

    public void clear() {
        cacheManager.getCache(CacheConfiguration.COVER_ART_CACHE).clear();
    }

    public CoverArt getCoverArt(CoverArt.EntityType entityType, Integer entityId) {
        if (entityType == null || entityId == null) {
            return null;
        }
        return cacheManager.getCache(CacheConfiguration.COVER_ART_CACHE, String.class, CoverArt.class)
                .get(entityType.toString().concat("-").concat(entityId.toString()));
    }

    public void putCoverArt(CoverArt art) {
        if (art == null || art.getEntityType() == null || art.getEntityId() == null) {
            return;
        }
        cacheManager.getCache(CacheConfiguration.COVER_ART_CACHE, String.class, CoverArt.class)
                .put(art.getEntityType().toString().concat("-").concat(art.getEntityId().toString()), art);
    }

    public void removeCoverArt(CoverArt art) {
        if (art == null || art.getEntityType() == null || art.getEntityId() == null) {
            return;
        }
        cacheManager.getCache(CacheConfiguration.COVER_ART_CACHE, String.class, CoverArt.class)
                .remove(art.getEntityType().toString().concat("-").concat(art.getEntityId().toString()));
    }

    public void removeCoverArt(CoverArt.EntityType entityType, Integer entityId) {
        if (entityType == null || entityId == null) {
            return;
        }
        cacheManager.getCache(CacheConfiguration.COVER_ART_CACHE, String.class, CoverArt.class)
                .remove(entityType.toString().concat("-").concat(entityId.toString()));
    }

}
