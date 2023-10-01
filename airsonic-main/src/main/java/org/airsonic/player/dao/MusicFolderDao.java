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

 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.airsonic.player.dao;

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Optional;

/**
 * Provides database services for music folders.
 *
 * @author Sindre Mehus
 */
@Repository
public class MusicFolderDao extends AbstractDao {

    private static final String INSERT_COLUMNS = "path, name, type, enabled, changed";
    public static final MusicFolderRowMapper MUSICFOLDER_ROW_MAPPER = new MusicFolderRowMapper();

    @PostConstruct
    public void register() throws Exception {
        registerInserts("music_folder", "id", Arrays.asList(INSERT_COLUMNS.split(", ")), MusicFolder.class);
    }


    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void reassignChildren(MusicFolder from, MusicFolder to) {
        if (to.getPath().getNameCount() > from.getPath().getNameCount()) {
            // assign ancestor -> descendant
            MusicFolder ancestor = from;
            MusicFolder descendant = to;
            String relativePath = ancestor.getPath().relativize(descendant.getPath()).toString();
            // update children
            int len = relativePath.length();
            String sql = "update media_file set "
                    + "folder_id=?, "
                    + "path=SUBSTR(path, " + (len + 2) + "), "
                    + "parent_path=(case "
                    + "  when (length(parent_path) > " + len + ") then SUBSTR(parent_path, " + (len + 2) + ") "
                    + "  else SUBSTR(parent_path, " + (len + 1) + ") end) "
                    + "where folder_id=? and path like ?";
            update(sql, descendant.getId(), ancestor.getId(), relativePath + File.separator + "%");
            sql = "update cover_art set "
                    + "folder_id=?, "
                    + "path=SUBSTR(path, " + (len + 2) + ") "
                    + "where folder_id=? and path like ?";
            update(sql, descendant.getId(), ancestor.getId(), relativePath + File.separator + "%");

            // update root
            sql = "update media_file set "
                    + "folder_id=?, "
                    + "path='', "
                    + "parent_path=null, "
                    + "title=?, "
                    + "type=? "
                    + "where folder_id=? and path=?";
            update(sql, descendant.getId(), descendant.getName(), MediaFile.MediaType.DIRECTORY, ancestor.getId(), relativePath);
        } else {
            // assign descendant -> ancestor
            MusicFolder ancestor = to;
            MusicFolder descendant = from;
            Path relativePath = ancestor.getPath().relativize(descendant.getPath());
            // update root
            String sql = "update media_file set "
                    + "folder_id=?, "
                    + "title=null, "
                    + "path=?, "
                    + "parent_path=? "
                    + "where folder_id=? and path=''";
            update(sql, ancestor.getId(), relativePath, relativePath.getParent() == null ? "" : relativePath.getParent().toString(), descendant.getId());
            // update children
            sql = "update media_file set "
                    + "folder_id=?, "
                    + "path=concat(?, path), "
                    + "parent_path=(case"
                    + "  when (parent_path = '') then ?"
                    + "  else concat(?, parent_path) end) "
                    + "where folder_id=?";
            update(sql, ancestor.getId(), relativePath + File.separator, relativePath, relativePath + File.separator, descendant.getId());
            sql = "update cover_art set folder_id=?, path=concat(?, path) where folder_id=?";
            update(sql, ancestor.getId(), relativePath + File.separator, descendant.getId());
        }
    }

    public static class MusicFolderRowMapper implements RowMapper<MusicFolder> {
        @Override
        public MusicFolder mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new MusicFolder(rs.getInt("id"), Paths.get(rs.getString("path")), rs.getString("name"),
                    MusicFolder.Type.valueOf(rs.getString("type")), rs.getBoolean("enabled"),
                    Optional.ofNullable(rs.getTimestamp("changed")).map(x -> x.toInstant()).orElse(null));
        }
    }
}
