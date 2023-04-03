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

import javax.persistence.*;

import java.time.Instant;

/**
 * A bookmark within a media file, for a given user.
 *
 * @author Sindre Mehus and Y.Tory
 * @version $Id$
 */
@Entity
@Table(name = "bookmark", uniqueConstraints = @UniqueConstraint(columnNames = {"username","media_file_id"}))
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Bookmark {

    public Bookmark(int mediaFileId, String username) {
        this.mediaFileId = mediaFileId;
        this.username = username;
        Instant now = Instant.now();
        this.created = now;
        this.changed = now;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "media_file_id")
    private int mediaFileId;

    @Column(name = "position_millis")
    private long positionMillis;

    @Column(name = "username")
    private String username;

    @Column(name = "comment")
    private String comment;

    @Column(name = "created")
    private Instant created;

    @Column(name = "changed")
    private Instant changed;

}
