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
package org.airsonic.player.service;

import org.airsonic.player.config.AirsonicHomeConfig;
import org.airsonic.player.dao.UserDao;
import org.airsonic.player.domain.AlbumListType;
import org.airsonic.player.domain.Avatar;
import org.airsonic.player.domain.AvatarScheme;
import org.airsonic.player.domain.UserSettings;
import org.airsonic.player.domain.entity.SystemAvatar;
import org.airsonic.player.repository.CustomAvatarRepository;
import org.airsonic.player.repository.SystemAvatarRepository;
import org.airsonic.player.util.FileUtil;
import org.airsonic.player.util.ImageUtil;
import org.airsonic.player.util.StringUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class PersonalSettingsService {

    private static final Logger LOG = LoggerFactory.getLogger(PersonalSettingsService.class);
    private static final int MAX_AVATAR_SIZE = 64;

    private final SystemAvatarRepository systemAvatarRepository;
    private final CustomAvatarRepository customAvatarRepository;
    private final AirsonicHomeConfig homeConfig;
    private final UserDao userDao;

    public PersonalSettingsService(SystemAvatarRepository systemAvatarRepository,
            CustomAvatarRepository customAvatarRepository, AirsonicHomeConfig homeConfig, UserDao userDao) {
        this.systemAvatarRepository = systemAvatarRepository;
        this.customAvatarRepository = customAvatarRepository;
        this.homeConfig = homeConfig;
        this.userDao = userDao;
    }

    public List<Avatar> getSystemAvatars() {
        return systemAvatarRepository.findAll().stream().map(SystemAvatar::toAvatar).toList();
    }

    public Avatar getCustomAvatar(String username) {
        if (StringUtils.isBlank(username)) {
            return null;
        }
        Path airsonicHome = homeConfig.getAirsonicHome();
        return customAvatarRepository.findByUsername(username).map(ca -> ca.toAvatar(airsonicHome)).orElse(null);
    }

    public Avatar getSystemAvatar(Integer id) {
        if (id == null) {
            return null;
        }
        return systemAvatarRepository.findById(id).map(SystemAvatar::toAvatar).orElse(null);
    }

    /**
     * Returns the avatar for the given user. If id is not null, return the system
     * avatar with the given id. If username is not null, return the custom avatar
     * for the given user. If forceCustom is true, return the custom avatar for the
     * given user.
     *
     * @param id          The system avatar id
     * @param username    The username of the user to get the custom avatar for.
     * @param forceCustom If true, return the custom avatar for the given user.
     * @return Avatar.
     */
    public Avatar getAvatar(Integer id, String username, boolean forceCustom) {

        if (id != null) {
            return getSystemAvatar(id);
        }

        if (username == null) {
            return null;
        }

        UserSettings settings = getUserSettings(username);

        if (forceCustom || settings.getAvatarScheme() == AvatarScheme.CUSTOM) {
            return getCustomAvatar(username);
        }

        if (settings.getAvatarScheme() == AvatarScheme.NONE) {
            return null;
        }
        return getSystemAvatar(settings.getSystemAvatarId());
    }

    /**
     * Creates an avatar image from the given data.
     *
     * @param fileName the file name of the image.
     * @param data     the image data.
     * @param username the username.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Map<String, Object> createCustomAvatar(String fileName, byte[] data, String username) {

        BufferedImage image;
        Map<String, Object> result = new HashMap<>();
        try {
            image = ImageIO.read(new ByteArrayInputStream(data));
            if (image == null) {
                throw new IOException("Failed to decode incoming image: " + fileName + " (" + data.length + " bytes).");
            }
            int width = image.getWidth();
            int height = image.getHeight();
            String mimeType = StringUtil.getMimeType(FilenameUtils.getExtension(fileName));
            Path folder = homeConfig.getAirsonicHome().resolve("avatars").resolve(username);
            Files.createDirectories(folder);
            Path fileOnDisk = folder.resolve(fileName + "." + StringUtils.substringAfter(mimeType, "/"));
            // Scale down image if necessary.
            if (width > MAX_AVATAR_SIZE || height > MAX_AVATAR_SIZE) {
                double scaleFactor = MAX_AVATAR_SIZE / (double) Math.max(width, height);
                height = (int) (height * scaleFactor);
                width = (int) (width * scaleFactor);
                image = ImageUtil.scale(image, width, height);
                mimeType = StringUtil.getMimeType("jpeg");
                fileOnDisk = folder.resolve(fileName + ".jpeg");
                ImageIO.write(image, "jpeg", fileOnDisk.toFile());

                result.put("resized", true);
            } else {
                Files.copy(new ByteArrayInputStream(data), fileOnDisk, StandardCopyOption.REPLACE_EXISTING);
            }
            customAvatarRepository.deleteAllByUsername(username);
            Avatar avatar = new Avatar(fileName, Instant.now(), mimeType, width, height, fileOnDisk);
            customAvatarRepository.save(avatar.toCustomAvatar(username, homeConfig.getAirsonicHome()));
            LOG.info("Created avatar '{}' ({} bytes) for user {}", fileName, FileUtil.size(fileOnDisk), username);
            return result;

        } catch (IOException x) {
            LOG.warn("Failed to upload personal image", x);
            result.put("error", x);
            return result;
        }
    }

    /**
     * Returns settings for the given user.
     *
     * @param username The username.
     * @return User-specific settings. Never <code>null</code>.
     */
    @Cacheable(cacheNames = "userSettingsCache")
    public UserSettings getUserSettings(String username) {
        UserSettings settings = userDao.getUserSettings(username);
        if (settings == null) {
            settings = createDefaultUserSettings(username);
        }
        return settings;
    }

    private UserSettings createDefaultUserSettings(String username) {
        UserSettings settings = new UserSettings(username);
        settings.setFinalVersionNotificationEnabled(true);
        settings.setBetaVersionNotificationEnabled(false);
        settings.setSongNotificationEnabled(true);
        settings.setShowNowPlayingEnabled(true);
        settings.setPartyModeEnabled(false);
        settings.setNowPlayingAllowed(true);
        settings.setAutoHidePlayQueue(true);
        settings.setKeyboardShortcutsEnabled(false);
        settings.setShowSideBar(true);
        settings.setShowArtistInfoEnabled(true);
        settings.setViewAsList(false);
        settings.setQueueFollowingSongs(true);
        settings.setDefaultAlbumList(AlbumListType.RANDOM);
        settings.setLastFmEnabled(false);
        settings.setListenBrainzEnabled(false);
        settings.setChanged(Instant.now());

        UserSettings.Visibility playlist = settings.getPlaylistVisibility();
        playlist.setArtistVisible(true);
        playlist.setAlbumVisible(true);
        playlist.setYearVisible(true);
        playlist.setDurationVisible(true);
        playlist.setBitRateVisible(true);
        playlist.setFormatVisible(true);
        playlist.setFileSizeVisible(true);

        UserSettings.Visibility main = settings.getMainVisibility();
        main.setTrackNumberVisible(true);
        main.setArtistVisible(true);
        main.setDurationVisible(true);

        return settings;
    }

    /**
     * Updates the user settings.
     *
     * @param settings The settings.
     */
    @CacheEvict(cacheNames = "userSettingsCache", key = "#settings.username")
    public void updateUserSettings(UserSettings settings) {
        userDao.updateUserSettings(settings);
    }

}
