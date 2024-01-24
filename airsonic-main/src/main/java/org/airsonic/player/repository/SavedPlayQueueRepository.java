package org.airsonic.player.repository;

import org.airsonic.player.domain.SavedPlayQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SavedPlayQueueRepository extends JpaRepository<SavedPlayQueue, Integer> {

    Optional<SavedPlayQueue> findByUsername(String username);

}
