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

import java.time.Instant;

/**
 * A Podcast episode belonging to a channel.
 *
 * @author Sindre Mehus
 * @see PodcastChannel
 */

@Getter
@Setter
public class PodcastEpisode {

    private final Integer id;
    private Integer mediaFileId;
    private Integer channelId;
    private String episodeGuid;
    private String url;
    private String title;
    private String description;
    private Instant publishDate;
    private String duration;
    private Long bytesTotal;
    private Long bytesDownloaded;
    private PodcastStatus status;
    private String errorMessage;

    public PodcastEpisode(Integer id, Integer channelId, String episodeGuid, String url, Integer mediaFileId, String title,
                          String description, Instant publishDate, String duration, Long length, Long bytesDownloaded,
                          PodcastStatus status, String errorMessage) {
        this.id = id;
        this.channelId = channelId;
        this.episodeGuid = episodeGuid;
        this.url = url;
        this.mediaFileId = mediaFileId;
        this.title = title;
        this.description = description;
        this.publishDate = publishDate;
        this.duration = duration;
        this.bytesTotal = length;
        this.bytesDownloaded = bytesDownloaded;
        this.status = status;
        this.errorMessage = errorMessage;
    }

    /**
     * Returns the completion rate of this episode, or null if the length is unknown.
     *
     * @return The completion rate, or null if the length is unknown.
     */
    public Double getCompletionRate() {
        if (this.bytesTotal == null || bytesTotal == 0) {
            return null;
        }
        if (bytesDownloaded == null) {
            return 0.0;
        }

        double d = bytesDownloaded;
        double t = bytesTotal;
        return d / t;
    }

}
