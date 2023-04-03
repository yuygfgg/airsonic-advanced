/*
 This file is part of Airsonic.

 Airsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2023 (C) Y.Tory
 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.airsonic.player.domain;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;

import java.time.Instant;

/**
 * Represents a remote player. A player has a unique ID, a user-defined name, a
 * logged-on user, miscellaneous identifiers, and an associated playlist.
 *
 * @author Sindre Mehus
 */
@Getter
@Setter
public class Player {

    // The system-generated ID.
    private Integer id;

    // The user-defined name.
    private String name;

    // The player "technology", e.g., web, external or jukebox.
    private PlayerTechnology technology = PlayerTechnology.WEB;

    // The third-party client ID (used if this player is managed over the Airsonic REST API).
    private String clientId;

    // player type, e.g., WinAmp, iTunes.
    private String type;

    // The user name of the logged-on user.
    private String username;

    // The IP address of the player.
    private String ipAddress;

    // whether the player has dynamic IP address
    private boolean dynamicIp = true;

    // whether the player is automatically started
    private boolean autoControlEnabled = true;

    // whether apply BOM mark when generating a M3U file
    private boolean m3uBomEnabled = true;

    // the time when the player was last seen
    private Instant lastSeen;

    // the transcode scheme
    private TranscodeScheme transcodeScheme = TranscodeScheme.OFF;

    // the playlist associated with this player
    private PlayQueue playQueue;

    public boolean isJukebox() {
        return technology == PlayerTechnology.JUKEBOX;
    }

    public boolean isExternal() {
        return technology == PlayerTechnology.EXTERNAL;
    }

    public boolean isExternalWithPlaylist() {
        return technology == PlayerTechnology.EXTERNAL_WITH_PLAYLIST;
    }

    public boolean isWeb() {
        return technology == PlayerTechnology.WEB;
    }


    /**
     * Returns a long description of the player, e.g., <code>Player 3 [admin]</code>
     *
     * @return A long description of the player.
     */
    public String getDescription() {
        StringBuilder builder = new StringBuilder();
        if (name != null) {
            builder.append(name);
        } else {
            builder.append("Player ").append(id);
        }

        builder.append(" [").append(username).append(']');
        return builder.toString();
    }

    /**
     * Returns a short description of the player, e.g., <code>Player 3</code>
     *
     * @return A short description of the player.
     */
    public String getShortDescription() {
        if (StringUtils.isNotBlank(name)) {
            return name;
        }
        return "Player " + id;
    }

    /**
     * Returns a string representation of the player.
     *
     * @return A string representation of the player.
     * @see #getDescription()
     */
    @Override
    public String toString() {
        return this.getDescription();
    }

}
