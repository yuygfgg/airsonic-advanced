package org.airsonic.player.repository;

import org.airsonic.player.domain.CoverArt;
import org.airsonic.player.domain.CoverArt.EntityType;
import org.airsonic.player.domain.entity.CoverArtKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CoverArtRepository extends JpaRepository<CoverArt, CoverArtKey> {

    public Optional<CoverArt> findByEntityTypeAndEntityId(EntityType entityType, Integer entityId);

    public void deleteByEntityTypeAndEntityId(EntityType entityType, Integer entityId);

}
