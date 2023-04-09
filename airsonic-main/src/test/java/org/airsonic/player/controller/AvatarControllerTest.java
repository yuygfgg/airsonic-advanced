/**
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
 */
package org.airsonic.player.controller;

import org.airsonic.player.config.AirsonicHomeConfig;
import org.airsonic.player.dao.AvatarDao;
import org.airsonic.player.domain.Avatar;
import org.airsonic.player.domain.AvatarScheme;
import org.airsonic.player.domain.UserSettings;
import org.airsonic.player.service.SettingsService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Y.Tory
 * @version $Id$
 */
@ExtendWith(MockitoExtension.class)
@WebMvcTest
@ContextConfiguration(classes = {AvatarController.class}, initializers = ConfigDataApplicationContextInitializer.class)
@EnableConfigurationProperties({AirsonicHomeConfig.class})
public class AvatarControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private UserSettings userSettings;

    @MockBean
    private SettingsService settingsService;

    @Mock
    private UserSettings mockedUserSettings;

    @Mock
    private Avatar mockedAvatar;

    @TempDir
    private static Path tempDir;

    @BeforeAll
    public static void setup() {
        System.setProperty("airsonic.home", tempDir.toString());
    }

    @AfterAll
    public static void teardown() {
        System.clearProperty("airsonic.home");
    }

    /** authentication info */
    private final String AIRSONIC_USER = "admin";
    private final String AIRSONIC_PASSWORD = "admin";

    @Test
    @WithMockUser(username = AIRSONIC_USER, password = AIRSONIC_PASSWORD)
    public void getAvatarWithoutParamFailureByNotFound() throws Exception {

        mvc.perform(get("/avatar"))
                .andExpect(status().isNotFound());
    }


    @Test
    @WithMockUser(username = AIRSONIC_USER, password = AIRSONIC_PASSWORD)
    public void getAvatarByIdFailureByNotFound() throws Exception {

        final Integer AVATAR_ID = 1;

        // setup mock
        when(settingsService.getSystemAvatar(eq(AVATAR_ID))).thenReturn(null);

        mvc.perform(get("/avatar")
                .param("id", AVATAR_ID.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = AIRSONIC_USER, password = AIRSONIC_PASSWORD)
    public void getAvatarByUsernameFailureByNotFound() throws Exception {

        when(settingsService.getUserSettings(eq("user"))).thenReturn(mockedUserSettings);
        when(mockedUserSettings.getAvatarScheme()).thenReturn(AvatarScheme.NONE);

        mvc.perform(get("/avatar")
                .param("username", "user"))
                .andExpect(status().isNotFound());
    }


    @Test
    @WithMockUser(username = AIRSONIC_USER, password = AIRSONIC_PASSWORD)
    public void getAvatarByIdTest() throws Exception {

        final Integer AVATAR_ID = 1;

        // set up mock
        when(settingsService.getSystemAvatar(eq(AVATAR_ID))).thenReturn(mockedAvatar);
        when(mockedAvatar.getMimeType()).thenReturn("img/png");
        when(mockedAvatar.getPath()).thenReturn(Paths.get("icons","avatars","Engineer.png"));

        // id = 1 is engineer avatar
        byte[] actual = mvc.perform(get("/avatar")
                .param("id", AVATAR_ID.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        // prepare expected
        byte [] expected = AvatarDao.class.getResourceAsStream("schema/Engineer.png").readAllBytes();

        // assertion
        assertArrayEquals(expected, actual);
        verify(settingsService).getSystemAvatar(eq(AVATAR_ID));
    }

    @Test
    @WithMockUser(username = AIRSONIC_USER, password = AIRSONIC_PASSWORD)
    public void getAvatarByIdPriorToUsernameTest() throws Exception {
        final Integer AVATAR_ID = 1;

        // set up mock
        when(settingsService.getSystemAvatar(eq(AVATAR_ID))).thenReturn(mockedAvatar);
        when(mockedAvatar.getMimeType()).thenReturn("img/png");
        when(mockedAvatar.getPath()).thenReturn(Paths.get("icons","avatars","Engineer.png"));

        // id = 1 is engineer avatar
        MvcResult result = mvc.perform(get("/avatar")
                .param("id", AVATAR_ID.toString())
                .param("username", "user"))
                .andExpect(status().isOk())
                .andReturn();
        byte[] actual = result.getResponse().getContentAsByteArray();

        // prepare expected
        byte [] expected = AvatarDao.class.getResourceAsStream("schema/Engineer.png").readAllBytes();

        // assertion
        assertArrayEquals(expected, actual);
        verify(settingsService).getSystemAvatar(eq(AVATAR_ID));
        verify(settingsService, never()).getUserSettings(anyString());
    }

    @Test
    @WithMockUser(username = AIRSONIC_USER, password = AIRSONIC_PASSWORD)
    public void getAvatarByUsernameWithSystemAvatarTest() throws Exception {

        final int AVATAR_ID = 1;
        final String USER_NAME = "user";

        // setup mock
        when(settingsService.getUserSettings(anyString())).thenReturn(mockedUserSettings);
        when(mockedUserSettings.getAvatarScheme()).thenReturn(AvatarScheme.SYSTEM);
        when(mockedUserSettings.getSystemAvatarId()).thenReturn(AVATAR_ID);
        when(settingsService.getSystemAvatar(eq(AVATAR_ID))).thenReturn(mockedAvatar);
        when(mockedAvatar.getMimeType()).thenReturn("img/png");
        when(mockedAvatar.getPath()).thenReturn(Paths.get("icons","avatars","Engineer.png"));

        // id = 1 is engineer avatar
        MvcResult result = mvc.perform(get("/avatar")
                .param("username", USER_NAME))
                .andExpect(status().isOk())
                .andReturn();
        byte[] actual = result.getResponse().getContentAsByteArray();

        // prepare expected
        byte [] expected = AvatarDao.class.getResourceAsStream("schema/Engineer.png").readAllBytes();

        // assertion
        assertArrayEquals(expected, actual);
        verify(settingsService).getSystemAvatar(eq(AVATAR_ID));
        verify(settingsService).getUserSettings(eq(USER_NAME));
    }

    @Test
    @WithMockUser(username = AIRSONIC_USER, password = AIRSONIC_PASSWORD)
    public void getAvatarByUsernameWithNoneAvatarTest() throws Exception {

        final String USER_NAME = "user";

        // setup mock
        when(settingsService.getUserSettings(anyString())).thenReturn(mockedUserSettings);
        when(mockedUserSettings.getAvatarScheme()).thenReturn(AvatarScheme.NONE);

        // id = 1 is engineer avatar
        mvc.perform(get("/avatar")
                .param("username", USER_NAME))
                .andExpect(status().isNotFound())
                .andReturn();

        verify(settingsService).getUserSettings(eq(USER_NAME));
    }

    @Test
    @WithMockUser(username = AIRSONIC_USER, password = AIRSONIC_PASSWORD)
    public void getAvatarByUsernameWithCustomAvatarTest() throws Exception {

        final String USER_NAME = "user";

        // setup mock
        when(settingsService.getUserSettings(anyString())).thenReturn(mockedUserSettings);
        when(mockedUserSettings.getAvatarScheme()).thenReturn(AvatarScheme.CUSTOM);
        when(settingsService.getCustomAvatar(anyString())).thenReturn(mockedAvatar);
        when(mockedAvatar.getMimeType()).thenReturn("img/png");
        when(mockedAvatar.getPath()).thenReturn(Paths.get("icons","avatars","All-Caps.png"));

        // id = 1 is engineer avatar
        MvcResult result = mvc.perform(get("/avatar")
                .param("username", USER_NAME))
                .andExpect(status().isOk())
                .andReturn();
        byte[] actual = result.getResponse().getContentAsByteArray();

        // prepare expected
        byte [] expected = AvatarDao.class.getResourceAsStream("schema/All-Caps.png").readAllBytes();

        // assertion
        assertArrayEquals(expected, actual);
        verify(settingsService).getUserSettings(eq(USER_NAME));
        verify(settingsService).getCustomAvatar(eq(USER_NAME));
    }

    @Test
    @WithMockUser(username = AIRSONIC_USER, password = AIRSONIC_PASSWORD)
    public void getAvatarByUsernameAndForceCustomTest() throws Exception {

        final String USER_NAME = "user";

        // setup mock
        when(settingsService.getUserSettings(anyString())).thenReturn(mockedUserSettings);
        when(mockedUserSettings.getAvatarScheme()).thenReturn(AvatarScheme.SYSTEM);
        when(settingsService.getCustomAvatar(anyString())).thenReturn(mockedAvatar);
        when(mockedAvatar.getMimeType()).thenReturn("img/png");
        when(mockedAvatar.getPath()).thenReturn(Paths.get("icons","avatars","All-Caps.png"));

        // id = 1 is engineer avatar
        MvcResult result = mvc.perform(get("/avatar")
                .param("username", USER_NAME)
                .param("forceCustom", "true"))
                .andExpect(status().isOk())
                .andReturn();
        byte[] actual = result.getResponse().getContentAsByteArray();

        // prepare expected
        byte [] expected = AvatarDao.class.getResourceAsStream("schema/All-Caps.png").readAllBytes();

        // assertion
        assertArrayEquals(expected, actual);
        verify(settingsService).getUserSettings(eq(USER_NAME));
        verify(settingsService).getCustomAvatar(eq(USER_NAME));
    }
}
