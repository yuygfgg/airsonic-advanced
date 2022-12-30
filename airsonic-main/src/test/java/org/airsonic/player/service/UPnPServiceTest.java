package org.airsonic.player.service;

import org.airsonic.player.util.HomeRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;


@RunWith(SpringRunner.class)
@SpringBootTest
public class UPnPServiceTest {

    @SpyBean
    private SettingsService settingsService;

    @InjectMocks
    @Autowired
    private UPnPService uPnPService;

    @ClassRule
    public static final HomeRule classRule = new HomeRule(); // sets airsonic.home to a temporary dir

    @Test
    @SuppressWarnings("unchecked")
    public void ensureServiceStoppedTest() {
        when(settingsService.isDlnaEnabled()).thenReturn(true);
        when(settingsService.getUPnpPort()).thenReturn(4041);
        uPnPService.init();
        uPnPService.ensureServiceStopped();
        assertFalse(((AtomicReference<Boolean>)(ReflectionTestUtils.getField(uPnPService, "running"))).get());
    }
}
