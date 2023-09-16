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

import java.util.Objects;

/**
 * Configuration of what information to display about a song.
 */
public class UserSettingVisibility {
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

    public UserSettingVisibility() {}

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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        UserSettingVisibility that = (UserSettingVisibility) o;
        return trackNumberVisible == that.trackNumberVisible &&
                discNumberVisible == that.discNumberVisible &&
                artistVisible == that.artistVisible &&
                albumArtistVisible == that.albumArtistVisible &&
                albumVisible == that.albumVisible &&
                genreVisible == that.genreVisible &&
                yearVisible == that.yearVisible &&
                bitRateVisible == that.bitRateVisible &&
                durationVisible == that.durationVisible &&
                formatVisible == that.formatVisible &&
                fileSizeVisible == that.fileSizeVisible &&
                headerVisible == that.headerVisible &&
                playCountVisible == that.playCountVisible &&
                lastPlayedVisible == that.lastPlayedVisible &&
                createdVisible == that.createdVisible &&
                changedVisible == that.changedVisible &&
                lastScannedVisible == that.lastScannedVisible &&
                entryTypeVisible == that.entryTypeVisible;
    }

    @Override
    public int hashCode() {
        return Objects.hash(trackNumberVisible, discNumberVisible, artistVisible, albumArtistVisible, albumVisible,
                genreVisible,
                yearVisible, bitRateVisible, durationVisible, formatVisible, fileSizeVisible, headerVisible,
                playCountVisible, lastPlayedVisible, createdVisible, changedVisible, lastScannedVisible,
                entryTypeVisible);
    }
}