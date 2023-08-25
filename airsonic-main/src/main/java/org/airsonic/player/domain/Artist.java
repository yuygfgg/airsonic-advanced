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

import org.airsonic.player.repository.AtomicIntegerConverter;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * @author Sindre Mehus
 * @version $Id$
 */
@Entity
@Table(name = "artist")
public class Artist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(unique = true, nullable = false)
    private String name;
    @Column(name = "album_count")
    @Convert(converter = AtomicIntegerConverter.class)
    private final AtomicInteger albumCount = new AtomicInteger();
    @Column(name = "last_scanned", nullable = false)
    private Instant lastScanned;
    @Column(name = "present")
    private boolean present = true;
    @Column(name = "folder_id", nullable = true)
    private Integer folderId;

    public Artist() {
    }

    public Artist(String name) {
        this.name = name;
    }

    public Artist(int id, String name, int albumCount, Instant lastScanned, boolean present, Integer folderId) {
        this.id = id;
        this.name = name;
        this.albumCount.set(albumCount);
        this.lastScanned = lastScanned;
        this.present = present;
        this.folderId = folderId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAlbumCount() {
        return albumCount.get();
    }

    public void setAlbumCount(int albumCount) {
        this.albumCount.set(albumCount);
    }

    public Instant getLastScanned() {
        return lastScanned;
    }

    public void setLastScanned(Instant lastScanned) {
        this.lastScanned = lastScanned;
    }

    public boolean isPresent() {
        return present;
    }

    public void setPresent(boolean present) {
        this.present = present;
    }

    public void setFolderId(Integer folderId) {
        this.folderId = folderId;
    }

    public Integer getFolderId() {
        return folderId;
    }

    // placeholder for persistence later
    @Transient
    private CoverArt art;

    public CoverArt getArt() {
        return art;
    }

    public void setArt(CoverArt art) {
        this.art = art;
    }
}
