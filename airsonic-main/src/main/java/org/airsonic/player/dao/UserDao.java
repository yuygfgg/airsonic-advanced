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

import org.airsonic.player.domain.*;
import org.airsonic.player.util.Util;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Provides user-related database services.
 *
 * @author Sindre Mehus
 */
@Repository
@Transactional
public class UserDao extends AbstractDao {

    private static final String USER_SETTINGS_COLUMNS = "username, settings";
    private UserSettingsRowMapper userSettingsRowMapper = new UserSettingsRowMapper();


    /**
     * Deletes the user with the given username.
     *
     * @param username The username.
     */
    public void deleteUser(String username) {
        update("delete from player where username=?", username);
        update("delete from user_credentials where username=?", username);
        update("delete from users where username=?", username);
    }

    /**
     * Updates the given user.
     *
     * @param user The user to update.
     */
    public void updateUser(User user) {
        String sql = "update users set email=?, ldap_authenticated=?, bytes_streamed=?, bytes_downloaded=?, bytes_uploaded=?, roles=? where username=?";
        update(sql, user.getEmail(), user.isLdapAuthenticated(),
                user.getBytesStreamed(), user.getBytesDownloaded(), user.getBytesUploaded(),
                Util.toJson(user.getRoles()),
                user.getUsername());
    }

    public void updateUserByteCounts(String user, long bytesStreamedDelta, long bytesDownloadedDelta, long bytesUploadedDelta) {
        String sql = "update users set bytes_streamed=bytes_streamed+?, bytes_downloaded=bytes_downloaded+?, bytes_uploaded=bytes_uploaded+? where username=?";
        update(sql, bytesStreamedDelta, bytesDownloadedDelta, bytesUploadedDelta, user);
    }

    /**
     * Returns settings for the given user.
     *
     * @param username The username.
     * @return User-specific settings, or <code>null</code> if no such settings exist.
     */
    public UserSettings getUserSettings(String username) {
        String sql = "select " + USER_SETTINGS_COLUMNS + " from user_settings where username=?";
        return queryOne(sql, userSettingsRowMapper, username);
    }

    /**
     * Updates settings for the given username, creating it if necessary.
     *
     * @param settings The user-specific settings.
     */
    public boolean updateUserSettings(UserSettings settings) {
        update("delete from user_settings where username=?", settings.getUsername());

        String sql = "insert into user_settings (" + USER_SETTINGS_COLUMNS + ") values (" + questionMarks(USER_SETTINGS_COLUMNS) + ")";

        return update(sql, settings.getUsername(), Util.toJson(settings)) == 1;
    }


    private static class UserSettingsRowMapper implements RowMapper<UserSettings> {
        @Override
        public UserSettings mapRow(ResultSet rs, int rowNum) throws SQLException {
            return Util.fromJson(rs.getString("settings"), UserSettings.class);
        }
    }
}
