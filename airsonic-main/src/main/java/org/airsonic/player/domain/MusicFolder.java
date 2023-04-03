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

import com.google.common.base.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Represents a top level directory in which music or other media is stored.
 *
 * @author Sindre Mehus
 * @version $Revision: 1.1 $ $Date: 2005/11/27 14:32:05 $
 */
@AllArgsConstructor
@Getter
@Setter
public class MusicFolder implements Serializable {

    // The system-generated ID.
    private Integer id;
    // The path of the music folder.
    private Path path;
    // The user-defined name.
    private String name;
    // The type of the music folder.
    private Type type = Type.MEDIA;
    // Whether the folder is enabled.
    private boolean enabled;
    // When the corresponding database entry was last changed.
    private Instant changed;

    /**
     * Creates a new music folder.
     *
     * @param path    The path of the music folder.
     * @param name    The user-defined name.
     * @param enabled Whether the folder is enabled.
     * @param changed When the corresponding database entry was last changed.
     */
    public MusicFolder(Path path, String name, Type type, boolean enabled, Instant changed) {
        this(null, path, name, type, enabled, changed);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return Objects.equal(id, ((MusicFolder) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public static enum Type {
        MEDIA, PODCAST
    }


    /**
     * Convert a list of MusicFolder to a list of id
     *
     * @param from List of MusicFolder to convert. null is not allowed.
     * @return List of MusicFolder id.
     */
    public static List<Integer> toIdList(List<MusicFolder> from) {
        return from.stream().map(toId()).collect(Collectors.toList());
    }

    /**
     * Convert a list of MusicFolder to a list of path
     *
     * @param from List of MusicFolder to convert. null is not allowed.
     * @return List of MusicFolder path.
     */
    public static List<String> toPathList(List<MusicFolder> from) {
        return from.stream().map(toPath()).collect(Collectors.toList());
    }

    private static Function<MusicFolder, Integer> toId() {
        return from -> from.getId();
    }

    private static Function<MusicFolder, String> toPath() {
        return from -> from.getPath().toString();
    }
}