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

import com.google.common.util.concurrent.AtomicDouble;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
@Data
@NoArgsConstructor
public class Album {

    private int id;
    private String path;
    private String name;
    private String artist;
    private final AtomicInteger songCount = new AtomicInteger(0);
    private final AtomicDouble duration = new AtomicDouble(0);
    private Integer year;
    private String genre;
    private final AtomicInteger playCount = new AtomicInteger(0);
    private Instant lastPlayed;
    private String comment;
    private Instant created;
    private Instant lastScanned;
    private boolean present;
    private Integer folderId;
    private String musicBrainzReleaseId;
    // placeholder for persistence later
    private CoverArt art;

    public Album(int id, String path, String name, String artist, int songCount, double duration,
            Integer year, String genre, int playCount, Instant lastPlayed, String comment, Instant created, Instant lastScanned,
            boolean present, Integer folderId, String musicBrainzReleaseId) {
        this.id = id;
        this.path = path;
        this.name = name;
        this.artist = artist;
        this.songCount.set(songCount);
        this.duration.set(duration);
        this.year = year;
        this.genre = genre;
        this.playCount.set(playCount);
        this.lastPlayed = lastPlayed;
        this.comment = comment;
        this.created = created;
        this.lastScanned = lastScanned;
        this.folderId = folderId;
        this.present = present;
        this.musicBrainzReleaseId = musicBrainzReleaseId;
    }


    public int getSongCount() {
        return songCount.get();
    }

    public void setSongCount(int songCount) {
        this.songCount.set(songCount);
    }

    public void incrementSongCount() {
        this.songCount.incrementAndGet();
    }

    public double getDuration() {
        return duration.get();
    }

    public void setDuration(double duration) {
        this.duration.set(duration);
    }

    public void incrementDuration(double duration) {
        this.duration.addAndGet(duration);
    }


    public int getPlayCount() {
        return playCount.get();
    }

    public void setPlayCount(int playCount) {
        this.playCount.set(playCount);
    }

    public void incrementPlayCount() {
        this.playCount.incrementAndGet();
    }

}
