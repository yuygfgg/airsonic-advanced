package org.airsonic.player.repository;

import org.airsonic.player.domain.PodcastChannelRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PodcastRuleRepository extends JpaRepository<PodcastChannelRule, Integer> {}
