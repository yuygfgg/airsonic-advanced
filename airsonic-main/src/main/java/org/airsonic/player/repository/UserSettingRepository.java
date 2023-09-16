package org.airsonic.player.repository;

import org.airsonic.player.domain.entity.UserSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSettingRepository extends JpaRepository<UserSetting, String> {}
