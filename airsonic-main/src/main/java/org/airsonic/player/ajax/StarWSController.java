package org.airsonic.player.ajax;

import org.airsonic.player.service.MediaFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;

@Controller
@MessageMapping("/rate/mediafile")
public class StarWSController {

    @Autowired
    private MediaFileService mediaFileService;

    @MessageMapping("/star")
    public void star(Principal user, List<Integer> ids) {
        mediaFileService.starMediaFiles(ids, user.getName());
    }

    @MessageMapping("/unstar")
    public void unstar(Principal user, List<Integer> ids) {
        mediaFileService.unstarMediaFiles(ids, user.getName());
    }

}
