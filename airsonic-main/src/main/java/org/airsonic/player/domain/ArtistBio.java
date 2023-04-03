/*
 * This file is part of Airsonic.
 *
 *  Airsonic is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Airsonic is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Copyright 2023 (C) Y.Tory
 *  Copyright 2014 (C) Sindre Mehus
 */

package org.airsonic.player.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
@RequiredArgsConstructor
@Getter
public class ArtistBio {

    private final String biography;
    private final String musicBrainzId;
    private final String lastFmUrl;
    private final String smallImageUrl;
    private final String mediumImageUrl;
    private final String largeImageUrl;

}
