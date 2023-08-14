package org.airsonic.player.service;

import org.airsonic.player.domain.User;

public interface UserService {

    /**
     * Get system administrator user, which system operations perform on behalf.
     * @return system admin.
     */
    User getSysAdmin();
}
