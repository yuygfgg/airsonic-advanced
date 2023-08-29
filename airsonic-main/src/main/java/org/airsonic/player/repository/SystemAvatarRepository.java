package org.airsonic.player.repository;

import org.airsonic.player.domain.entity.SystemAvatar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemAvatarRepository extends JpaRepository<SystemAvatar, Integer> {}
