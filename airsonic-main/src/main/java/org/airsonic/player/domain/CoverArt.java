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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Objects;

@Getter
@Setter
public class CoverArt {
    private int entityId;
    private EntityType entityType;
    private String path;
    private Integer folderId;
    private boolean overridden;
    private Instant created = Instant.now();
    private Instant updated = created;

    public enum EntityType {
        MEDIA_FILE, ALBUM, ARTIST, NONE
    }

    public final static CoverArt NULL_ART = new CoverArt(-2, EntityType.NONE, null, null, false);

    public CoverArt(int entityId, EntityType entityType, String path, Integer folderId, boolean overridden) {
        super();
        this.entityId = entityId;
        this.entityType = entityType;
        this.path = path;
        this.folderId = folderId;
        this.overridden = overridden;
    }

    public CoverArt(int entityId, EntityType entityType, String path, Integer folderId, boolean overridden, Instant created, Instant updated) {
        this(entityId, entityType, path, folderId, overridden);
        this.created = created;
        this.updated = updated;
    }

    public Path getRelativePath() {
        return Paths.get(path);
    }

    public Path getFullPath(Path relativeMediaFolderPath) {
        return relativeMediaFolderPath.resolve(path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityId, entityType);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CoverArt other = (CoverArt) obj;
        if (entityId != other.entityId)
            return false;
        if (entityType != other.entityType)
            return false;
        return true;
    }
}
