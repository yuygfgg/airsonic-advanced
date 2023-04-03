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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

/**
 * An icon representing a user.
 *
 * @author Sindre Mehus
 */
@RequiredArgsConstructor
@Getter
public class Avatar {

    private final int id;
    private final String name;
    private final Instant createdDate;
    private final String mimeType;
    private final int width;
    private final int height;
    private final Path path;

    public Avatar(int id, String name, Instant createdDate, String mimeType, int width, int height, String path) {
        this(id, name, createdDate, mimeType, width, height, Paths.get(path));
    }
}
