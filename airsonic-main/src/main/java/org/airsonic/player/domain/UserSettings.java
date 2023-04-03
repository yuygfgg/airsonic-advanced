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
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Locale;

/**
 * Represent user-specific settings.
 *
 * @author Sindre Mehus
 */
@Getter
@Setter
@RequiredArgsConstructor
public class UserSettings {

    private final String username;
    private Locale locale;
    private String themeId;
    private boolean showNowPlayingEnabled;
    private boolean showArtistInfoEnabled;
    private boolean finalVersionNotificationEnabled;
    private boolean betaVersionNotificationEnabled;
    private boolean songNotificationEnabled;
    private boolean keyboardShortcutsEnabled;
    private boolean autoHidePlayQueue;
    private boolean showSideBar;
    private boolean viewAsList;
    private boolean queueFollowingSongs;
    private AlbumListType defaultAlbumList = AlbumListType.RANDOM;
    private Visibility mainVisibility = new Visibility();
    private Visibility playlistVisibility = new Visibility();
    private Visibility playqueueVisibility = new Visibility();
    private boolean lastFmEnabled;
    private boolean listenBrainzEnabled;
    private String listenBrainzUrl;
    private boolean podcastIndexEnabled;
    private String podcastIndexUrl;
    private TranscodeScheme transcodeScheme = TranscodeScheme.OFF;
    private int selectedMusicFolderId = -1;
    private boolean partyModeEnabled;
    private boolean nowPlayingAllowed;
    private AvatarScheme avatarScheme = AvatarScheme.NONE;
    private Integer systemAvatarId;
    private Instant changed = Instant.now();
    private int paginationSizeFiles = 10;
    private int paginationSizeFolders = 7;
    private int paginationSizePlaylist = 10;
    private int paginationSizePlayqueue = 10;
    private int paginationSizeBookmarks = 20;
    private boolean autoBookmark = true;
    private int videoBookmarkFrequency = 40;
    private int audioBookmarkFrequency = 10;
    private int searchCount = 25;


    /**
     * Configuration of what information to display about a song.
     */
    public static class Visibility {
        private boolean trackNumberVisible;
        private boolean discNumberVisible;
        private boolean artistVisible = true;
        private boolean albumArtistVisible;
        private boolean albumVisible = true;
        private boolean genreVisible;
        private boolean yearVisible;
        private boolean bitRateVisible;
        private boolean durationVisible = true;
        private boolean formatVisible;
        private boolean fileSizeVisible;
        private boolean headerVisible;
        private boolean playCountVisible;
        private boolean lastPlayedVisible;
        private boolean createdVisible;
        private boolean changedVisible;
        private boolean lastScannedVisible;
        private boolean entryTypeVisible;

        public Visibility() {}

        public boolean getTrackNumberVisible() {
            return trackNumberVisible;
        }

        public void setTrackNumberVisible(boolean trackNumberVisible) {
            this.trackNumberVisible = trackNumberVisible;
        }

        public boolean getDiscNumberVisible() {
            return discNumberVisible;
        }

        public void setDiscNumberVisible(boolean discNumberVisible) {
            this.discNumberVisible = discNumberVisible;
        }

        public boolean getArtistVisible() {
            return artistVisible;
        }

        public void setArtistVisible(boolean artistVisible) {
            this.artistVisible = artistVisible;
        }

        public boolean getAlbumArtistVisible() {
            return albumArtistVisible;
        }

        public void setAlbumArtistVisible(boolean albumArtistVisible) {
            this.albumArtistVisible = albumArtistVisible;
        }

        public boolean getAlbumVisible() {
            return albumVisible;
        }

        public void setAlbumVisible(boolean albumVisible) {
            this.albumVisible = albumVisible;
        }

        public boolean getGenreVisible() {
            return genreVisible;
        }

        public void setGenreVisible(boolean genreVisible) {
            this.genreVisible = genreVisible;
        }

        public boolean getYearVisible() {
            return yearVisible;
        }

        public void setYearVisible(boolean yearVisible) {
            this.yearVisible = yearVisible;
        }

        public boolean getBitRateVisible() {
            return bitRateVisible;
        }

        public void setBitRateVisible(boolean bitRateVisible) {
            this.bitRateVisible = bitRateVisible;
        }

        public boolean getDurationVisible() {
            return durationVisible;
        }

        public void setDurationVisible(boolean durationVisible) {
            this.durationVisible = durationVisible;
        }

        public boolean getFormatVisible() {
            return formatVisible;
        }

        public void setFormatVisible(boolean formatVisible) {
            this.formatVisible = formatVisible;
        }

        public boolean getFileSizeVisible() {
            return fileSizeVisible;
        }

        public void setFileSizeVisible(boolean fileSizeVisible) {
            this.fileSizeVisible = fileSizeVisible;
        }

        public boolean getHeaderVisible() {
            return headerVisible;
        }

        public void setHeaderVisible(boolean headerVisible) {
            this.headerVisible = headerVisible;
        }

        public boolean getPlayCountVisible() {
            return playCountVisible;
        }

        public void setPlayCountVisible(boolean playCountVisible) {
            this.playCountVisible = playCountVisible;
        }

        public boolean getLastPlayedVisible() {
            return lastPlayedVisible;
        }

        public void setLastPlayedVisible(boolean lastPlayedVisible) {
            this.lastPlayedVisible = lastPlayedVisible;
        }

        public boolean getCreatedVisible() {
            return createdVisible;
        }

        public void setCreatedVisible(boolean createdVisible) {
            this.createdVisible = createdVisible;
        }

        public boolean getChangedVisible() {
            return changedVisible;
        }

        public void setChangedVisible(boolean changedVisible) {
            this.changedVisible = changedVisible;
        }

        public boolean getLastScannedVisible() {
            return lastScannedVisible;
        }

        public void setLastScannedVisible(boolean lastScannedVisible) {
            this.lastScannedVisible = lastScannedVisible;
        }

        public boolean getEntryTypeVisible() {
            return entryTypeVisible;
        }

        public void setEntryTypeVisible(boolean entryTypeVisible) {
            this.entryTypeVisible = entryTypeVisible;
        }

    }
}
