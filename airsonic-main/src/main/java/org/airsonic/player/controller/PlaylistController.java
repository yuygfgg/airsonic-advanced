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

 Copyright 2024 (C) Y.Tory
 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.airsonic.player.controller;

import org.airsonic.player.domain.Player;
import org.airsonic.player.domain.Playlist;
import org.airsonic.player.domain.User;
import org.airsonic.player.domain.UserSettings;
import org.airsonic.player.service.PersonalSettingsService;
import org.airsonic.player.service.PlayerService;
import org.airsonic.player.service.PlaylistService;
import org.airsonic.player.service.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for the playlist page.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping({"/playlist", "/playlist.view"})
public class PlaylistController {

    @Autowired
    private SecurityService securityService;
    @Autowired
    private PlaylistService playlistService;
    @Autowired
    private PersonalSettingsService personalSettingsService;
    @Autowired
    private PlayerService playerService;

    @GetMapping
    public ModelAndView get(@RequestParam(name = "id", required = true) Integer id, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, Object> map = new HashMap<>();

        User user = securityService.getCurrentUser(request);
        String username = user.getUsername();
        UserSettings userSettings = personalSettingsService.getUserSettings(username);
        Player player = playerService.getPlayer(request, response, username);
        Playlist playlist = playlistService.getPlaylist(id);
        if (playlist == null) {
            return new ModelAndView(new RedirectView("notFound"));
        }

        map.put("playlist", playlist);
        map.put("user", user);
        map.put("player", player);
        map.put("visibility", userSettings.getPlaylistVisibility());
        map.put("editAllowed", username.equals(playlist.getUsername()) || securityService.isAdmin(username));
        map.put("partyMode", userSettings.getPartyModeEnabled());
        map.put("initialPaginationSize", userSettings.getPaginationSizePlaylist());

        return new ModelAndView("playlist","model",map);
    }




}
