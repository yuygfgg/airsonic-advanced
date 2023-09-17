package org.airsonic.player.ajax;

import org.airsonic.player.service.PersonalSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@MessageMapping("/settings")
public class UserSettingsWSController {

    @Autowired
    private PersonalSettingsService personalSettingsService;

    @MessageMapping("/sidebar")
    @SendToUser
    public boolean setShowSideBar(Principal p, boolean show) {
        personalSettingsService.updateShowSideBarStatus(p.getName(), show);
        return show;
    }

    @MessageMapping("/viewAsList")
    @SendToUser
    public boolean setViewAsList(Principal p, boolean viewAsList) {
        personalSettingsService.updateViewAsListStatus(p.getName(), viewAsList);
        return viewAsList;
    }

}
