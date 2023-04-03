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
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.io.FilenameUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A media file (audio, video or directory) with an assortment of its meta data.
 *
 * @author Sindre Mehus
 * @version $Id$
 */
@NoArgsConstructor
@Getter
@Setter
public class MediaFile {

    private Integer id;
    private String path;
    private Integer folderId;
    private MediaType mediaType;
    private Double startPosition = NOT_INDEXED; // i.e. not an indexed track
    private String format;
    private String title;
    private String albumName;
    private String artist;
    private String albumArtist;
    private Integer discNumber;
    private Integer trackNumber;
    private Integer year;
    private String genre;
    private Integer bitRate;
    private boolean variableBitRate;
    private Double duration;
    private Long fileSize;
    private Integer width;
    private Integer height;
    private String parentPath;
    private String indexPath;
    private int playCount;
    private Instant lastPlayed;
    private String comment;
    private Instant created;
    private Instant changed;
    private Instant lastScanned;
    private Instant starredDate;
    private Instant childrenLastUpdated;
    private boolean present;
    private int version;
    private String musicBrainzReleaseId;
    private String musicBrainzRecordingId;

    public MediaFile(Integer id, String path, Integer folderId, MediaType mediaType, Double startPosition, String format, String title,
                     String albumName, String artist, String albumArtist, Integer discNumber, Integer trackNumber, Integer year, String genre, Integer bitRate,
                     boolean variableBitRate, Double duration, Long fileSize, Integer width, Integer height, String parentPath, String indexPath, int playCount,
                     Instant lastPlayed, String comment, Instant created, Instant changed, Instant lastScanned, Instant childrenLastUpdated, boolean present,
                     int version, String musicBrainzReleaseId, String musicBrainzRecordingId) {
        this.id = id;
        this.path = path;
        this.folderId = folderId;
        this.mediaType = mediaType;
        this.startPosition = startPosition;
        this.format = format;
        this.title = title;
        this.albumName = albumName;
        this.artist = artist;
        this.albumArtist = albumArtist;
        this.discNumber = discNumber;
        this.trackNumber = trackNumber;
        this.year = year;
        this.genre = genre;
        this.bitRate = bitRate;
        this.variableBitRate = variableBitRate;
        this.duration = duration;
        this.fileSize = fileSize;
        this.width = width;
        this.height = height;
        this.parentPath = parentPath;
        this.indexPath = indexPath;
        this.playCount = playCount;
        this.lastPlayed = lastPlayed;
        this.comment = comment;
        this.created = created;
        this.changed = changed;
        this.lastScanned = lastScanned;
        this.childrenLastUpdated = childrenLastUpdated;
        this.present = present;
        this.version = version;
        this.musicBrainzReleaseId = musicBrainzReleaseId;
        this.musicBrainzRecordingId = musicBrainzRecordingId;
    }


    public Path getRelativePath() {
        return Paths.get(this.path);
    }

    public Path getFullPath(Path relativeMediaFolderPath) {
        return relativeMediaFolderPath.resolve(this.path);
    }

    public Path getRelativeIndexPath() {
        return this.indexPath == null ? null : Paths.get(this.indexPath);
    }

    public Path getFullIndexPath(Path folderPath) {
        return folderPath.resolve(this.indexPath);
    }

    public boolean hasIndex() {
        return this.indexPath != null;
    }

    public boolean isVideo() {
        return this.mediaType == MediaType.VIDEO;
    }

    public boolean isAudio() {
        return MediaType.audioTypes().contains(this.mediaType.toString());
    }

    public boolean isIndexedTrack() {
        return this.startPosition >= 0.0;
    }

    public boolean isDirectory() {
        return !this.isFile();
    }

    public boolean isFile() {
        return this.mediaType != MediaType.DIRECTORY && this.mediaType != MediaType.ALBUM;
    }

    public boolean isAlbum() {
        return this.mediaType == MediaType.ALBUM;
    }

    /**
     * Returns the name of this media file. If the title is set, it is returned. Otherwise, if this is a directory,
     * the name of the directory is returned. Otherwise, the base name of the file is returned.
     * @return The name of this media file.
     */
    public String getName() {
        return this.title != null ? this.title : this.isDirectory() ? FilenameUtils.getName(this.path) : FilenameUtils.getBaseName(this.path);
    }

    // placeholder to use prior to persistence
    private CoverArt art;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MediaFile other = (MediaFile) obj;
        if (path == null) {
            if (other.path != null) {
                return false;
            }
        } else if (!path.equals(other.path)) {
            return false;
        }
        if (!folderId.equals(other.folderId)) {
            return false;
        }
        if (!startPosition.equals(other.startPosition)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, folderId, startPosition);
    }

    @Override
    public String toString() {
        return getName();
    }

    public static List<Integer> toIdList(List<MediaFile> from) {
        return from.stream().map(toId()).collect(Collectors.toList());
    }

    public static Function<MediaFile, Integer> toId() {
        return from -> from.getId();
    }

    public static final double NOT_INDEXED = -1.0;

    public static enum MediaType {
        MUSIC,
        PODCAST,
        AUDIOBOOK,
        VIDEO,
        DIRECTORY,
        ALBUM;

        private static final List<String> AUDIO_TYPES = Arrays.asList(MUSIC.toString(),AUDIOBOOK.toString(),PODCAST.toString());
        private static final List<String> PLAYABLE_TYPES = Arrays.asList(MUSIC.toString(),AUDIOBOOK.toString(),PODCAST.toString(),VIDEO.toString());

        public static List<String> audioTypes() {
            return AUDIO_TYPES;
        }

        public static List<String> playableTypes() {
            return PLAYABLE_TYPES;
        }
    }
}
