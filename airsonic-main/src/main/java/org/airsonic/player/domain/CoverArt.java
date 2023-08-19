package org.airsonic.player.domain;

import org.airsonic.player.domain.entity.CoverArtKey;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "cover_art")
@IdClass(CoverArtKey.class)
public class CoverArt {

    @Id
    @Column(name = "entity_id", nullable = false)
    private Integer entityId;

    @Id
    @Column(name = "entity_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private EntityType entityType;

    @Column(name = "path", nullable = false)
    private String path;

    @Column(name = "folder_id")
    private Integer folderId;

    @Column(name = "overridden")
    private boolean overridden;

    @Column(name = "created")
    private Instant created = Instant.now();

    @Column(name = "updated")
    private Instant updated = created;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id", insertable = false, updatable = false)
    private Artist artist;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id", insertable = false, updatable = false)
    private Album album;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id", insertable = false, updatable = false)
    private MediaFile mediaFile;

    public enum EntityType {
        MEDIA_FILE, ALBUM, ARTIST, NONE
    }

    public final static CoverArt NULL_ART = new CoverArt(-2, EntityType.NONE, null, null, false);

    public CoverArt() {
    }

    public CoverArt(Integer entityId, EntityType entityType, String path, Integer folderId, boolean overridden) {
        super();
        this.entityId = entityId;
        this.entityType = entityType;
        this.path = path;
        this.folderId = folderId;
        this.overridden = overridden;
    }

    public CoverArt(Integer entityId, EntityType entityType, String path, Integer folderId, boolean overridden, Instant created, Instant updated) {
        this(entityId, entityType, path, folderId, overridden);
        this.created = created;
        this.updated = updated;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Path getRelativePath() {
        return Paths.get(path);
    }

    public Path getFullPath(Path relativeMediaFolderPath) {
        return relativeMediaFolderPath.resolve(path);
    }

    public Integer getFolderId() {
        return folderId;
    }

    public void setFolderId(Integer folderId) {
        this.folderId = folderId;
    }

    public boolean getOverridden() {
        return overridden;
    }

    public void setOverridden(boolean overridden) {
        this.overridden = overridden;
    }

    public Instant getCreated() {
        return created;
    }

    public void setCreated(Instant created) {
        this.created = created;
    }

    public Instant getUpdated() {
        return updated;
    }

    public void setUpdated(Instant updated) {
        this.updated = updated;
    }

    public Artist getArtist() {
        return entityType == EntityType.ARTIST ? artist : null;
    }

    public void setArtist(Artist artist) {
        this.artist = artist;
    }

    public Album getAlbum() {
        return entityType == EntityType.ALBUM ? album : null;
    }

    public void setAlbum(Album album) {
        this.album = album;
    }

    public MediaFile getMediaFile() {
        return entityType == EntityType.MEDIA_FILE ? mediaFile : null;
    }

    public void setMediaFile(MediaFile mediaFile) {
        this.mediaFile = mediaFile;
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
