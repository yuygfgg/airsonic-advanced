package org.airsonic.player.service.cache;

import org.airsonic.player.domain.Playlist;
import org.airsonic.player.spring.CacheConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.cache.CacheManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class PlaylistCache {

    @Autowired
    private CacheManager cacheManager;

    public void clear() {
        cacheManager.getCache(CacheConfiguration.PLAYLIST_CACHE).clear();
        cacheManager.getCache(CacheConfiguration.PLAYLIST_USERS_CACHE).clear();
    }

    public Playlist getPlaylistById(@Nullable Integer id) {
        if (id == null) {
            return null;
        }
        return cacheManager.getCache(CacheConfiguration.PLAYLIST_CACHE, Integer.class, Playlist.class).get(id);
    }

    public void putPlaylistById(@Nullable Integer id, @Nullable Playlist playlist) {
        if (id == null || playlist == null) {
            return;
        }
        cacheManager.getCache(CacheConfiguration.PLAYLIST_CACHE, Integer.class, Playlist.class).put(id, playlist);
    }

    public void removePlaylistById(@Nullable Integer id) {
        if (id == null) {
            return;
        }
        cacheManager.getCache(CacheConfiguration.PLAYLIST_CACHE, Integer.class, Playlist.class).remove(id);
    }

    public List<String> getUsersForPlaylist(@Nullable Integer id) {
        if (id == null) {
            return new ArrayList<>();
        }
        PlaylistUserList userList = cacheManager.getCache(CacheConfiguration.PLAYLIST_USERS_CACHE, Integer.class, PlaylistUserList.class).get(id);
        if (userList == null) {
            return new ArrayList<>();
        }
        return userList.getUsers();
    }

    public void putUsersForPlaylist(@Nullable Integer id, @Nullable List<String> users) {
        if (id == null || users == null) {
            return;
        }
        cacheManager.getCache(CacheConfiguration.PLAYLIST_USERS_CACHE, Integer.class, PlaylistUserList.class).put(id, new PlaylistUserList(users));
    }

    public void removeUsersForPlaylist(@Nullable Integer id) {
        if (id == null) {
            return;
        }
        cacheManager.getCache(CacheConfiguration.PLAYLIST_USERS_CACHE, Integer.class, PlaylistUserList.class).remove(id);
    }

    public static class PlaylistUserList {
        private final List<String> users;

        public PlaylistUserList(@Nonnull List<String> users) {
            this.users = Collections.unmodifiableList(users);
        }

        public List<String> getUsers() {
            return users;
        }
    }

}
