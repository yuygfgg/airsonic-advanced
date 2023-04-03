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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Represents an internet radio station.
 *
 * @author Sindre Mehus
 * @version $Revision: 1.2 $ $Date: 2005/12/25 13:48:46 $
 */
@AllArgsConstructor
@Getter
@Setter
public class InternetRadio {

    // The system-generated ID.
    private final Integer id;

    // The user-defined name.
    private String name;

    // The stream URL for the station.
    private String streamUrl;

    // Th home page URL for the station.
    private String homepageUrl;

    // Whether the station is enabled.
    private boolean isEnabled;

    // When the corresponding database entry was last changed.
    private Instant changed;


    /**
     * Creates a new internet radio station.
     *
     * @param name        The user-defined name.
     * @param streamUrl   The URL for the station.
     * @param homepageUrl The home page URL for the station.
     * @param isEnabled   Whether the station is enabled.
     * @param changed     When the corresponding database entry was last changed.
     */
    public InternetRadio(String name, String streamUrl, String homepageUrl, boolean isEnabled, Instant changed) {
        this(null, name, streamUrl, homepageUrl, isEnabled, changed);
    }
}