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
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.airsonic.player.dao.AbstractDao.Column;

import java.time.Instant;

/**
 * @author Sindre Mehus
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Playlist {

    private int id;
    private String username;
    @Column("is_public")
    private boolean shared;
    private String name;
    private String comment;
    private int fileCount;
    private double duration;
    private Instant created;
    private Instant changed;
    private String importedFrom;

    public Playlist(Playlist p) {
        this(p.getId(), p.getUsername(), p.isShared(), p.getName(), p.getComment(), p.getFileCount(), p.getDuration(),
                p.getCreated(), p.getChanged(), p.getImportedFrom());
    }

}
