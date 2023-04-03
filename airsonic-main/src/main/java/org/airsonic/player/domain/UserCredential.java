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
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@AllArgsConstructor
@Getter
@Setter
public class UserCredential {
    private String username;
    private String appUsername;
    private String credential;
    private String encoder;
    private App app;
    private String comment;
    private Instant expiration;
    private Instant created;
    private Instant updated;


    public UserCredential(String username, String appUsername, String credential, String encoder, App app,
            String comment, Instant expiration) {
        this(username, appUsername, credential, encoder, app, comment, expiration, null, null);
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        setCreated(now);
        setUpdated(now);
    }

    public UserCredential(String username, String appUsername, String credential, String encoder, App app,
            String comment) {
        this(username, appUsername, credential, encoder, app, comment, null);
    }

    public UserCredential(String username, String appUsername, String credential, String encoder, App app) {
        this(username, appUsername, credential, encoder, app, null);
    }

    public UserCredential(UserCredential uc) {
        this(uc.getUsername(), uc.getAppUsername(), uc.getCredential(), uc.getEncoder(), uc.getApp(),
                uc.getComment(), uc.getExpiration(), uc.getCreated(), uc.getUpdated());
    }

    @Override
    public int hashCode() {
        return Objects.hash(comment, created, credential, expiration, app, appUsername, encoder, updated, username);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        UserCredential other = (UserCredential) obj;
        if (comment == null) {
            if (other.comment != null)
                return false;
        } else if (!comment.equals(other.comment))
            return false;
        if (created == null) {
            if (other.created != null)
                return false;
        } else if (!created.equals(other.created))
            return false;
        if (credential == null) {
            if (other.credential != null)
                return false;
        } else if (!credential.equals(other.credential))
            return false;
        if (expiration == null) {
            if (other.expiration != null)
                return false;
        } else if (!expiration.equals(other.expiration))
            return false;
        if (app == null) {
            if (other.app != null)
                return false;
        } else if (!app.equals(other.app))
            return false;
        if (appUsername == null) {
            if (other.appUsername != null)
                return false;
        } else if (!appUsername.equals(other.appUsername))
            return false;
        if (encoder == null) {
            if (other.encoder != null)
                return false;
        } else if (!encoder.equals(other.encoder))
            return false;
        if (updated == null) {
            if (other.updated != null)
                return false;
        } else if (!updated.equals(other.updated))
            return false;
        if (username == null) {
            if (other.username != null)
                return false;
        } else if (!username.equals(other.username))
            return false;
        return true;
    }

    @Getter
    @RequiredArgsConstructor
    public enum App {
        AIRSONIC("Airsonic", false, true),
        LASTFM("Last.fm", true, false),
        LISTENBRAINZ("Listenbrainz", false, false),
        PODCASTINDEX("Podcast Index", true, false);

        private final String name;
        private final boolean usernameRequired;
        private final boolean nonDecodableEncodersAllowed;

    }
}
