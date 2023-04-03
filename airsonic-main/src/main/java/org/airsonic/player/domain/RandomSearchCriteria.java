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
import org.airsonic.player.service.SearchService;

import java.time.Instant;
import java.util.List;

/**
 * Defines criteria used when generating random playlists.
 *
 * @author Sindre Mehus
 * @see SearchService#getRandomSongs
 */
@AllArgsConstructor
@Getter
public class RandomSearchCriteria {

    // Maximum number of songs to return.
    private final int count;

    // Only return songs of the given genre. May be <code>null</code>.
    private final String genre;

    // Only return songs released after (or in) this year. May be <code>null</code>.
    private final Integer fromYear;

    // Only return songs released before (or in) this year. May be <code>null</code>.
    private final Integer toYear;

    // Only return songs from these music folder. May NOT be <code>null</code>.
    private final List<MusicFolder> musicFolders;

    // Only return songs last played after this date. May be <code>null</code>.
    private final Instant minLastPlayedDate;

    // Only return songs last played before this date. May be <code>null</code>.
    private final Instant maxLastPlayedDate;

    // Only return songs rated more or equal to this value. May be <code>null</code>.
    private final Integer minAlbumRating;

    // Only return songs rated less or equal to this value. May be <code>null</code>.
    private final Integer maxAlbumRating;

    // Only return songs whose play count is more or equal to this value. May be <code>null</code>.
    private final Integer minPlayCount;

    // Only return songs whose play count is less or equal to this value. May be <code>null</code>.
    private final Integer maxPlayCount;

    // Should starred songs be included?
    private final boolean showStarredSongs;

    // Should unstarred songs be included?
    private final boolean showUnstarredSongs;

    // Only return songs with the given format. May be <code>null</code>.
    private final String format;

    /**
     * Creates a new instance.
     *
     * @param count        Maximum number of songs to return.
     * @param genre        Only return songs of the given genre. May be <code>null</code>.
     * @param fromYear     Only return songs released after (or in) this year. May be <code>null</code>.
     * @param toYear       Only return songs released before (or in) this year. May be <code>null</code>.
     * @param musicFolders Only return songs from these music folder. May NOT be <code>null</code>.
     */
    public RandomSearchCriteria(int count, String genre, Integer fromYear, Integer toYear, List<MusicFolder> musicFolders) {
        this(
            count, genre, fromYear, toYear, musicFolders,
            null, null, null, null, null, null, true, true, null
        );
    }

}
