package org.airsonic.player.repository;

import org.airsonic.player.domain.PodcastChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PodcastChannelRepository extends JpaRepository<PodcastChannel, Integer> {}
