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
package org.airsonic.player.controller;

import org.airsonic.player.config.AirsonicHomeConfig;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.Dimension;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

/**
 * @author Sindre Mehus
 * @version $Id: StreamControllerTestCase.java 3307 2013-01-04 13:48:49Z sindre_mehus $
 */

@ExtendWith(MockitoExtension.class)
public class HLSControllerTestCase {

    @Mock
    private AirsonicHomeConfig airsonicConfig;

    @Test
    public void testParseBitRate() throws Exception {
        when(airsonicConfig.getAirsonicHome()).thenReturn(Files.createTempDirectory("airsonicTest").toAbsolutePath());
        HLSController controller = new HLSController(null, null, null, null, null, null, null, airsonicConfig);

        Pair<Integer, Dimension> pair = controller.parseBitRate("1000", null);
        assertEquals(1000, pair.getLeft().intValue());
        assertNull(pair.getRight());

        pair = controller.parseBitRate("1000@400x300", null);
        assertEquals(1000, pair.getLeft().intValue());
        assertEquals(400, pair.getRight().width);
        assertEquals(300, pair.getRight().height);

        pair = controller.parseBitRate("1000@400x300", "300x400");
        assertEquals(1000, pair.getLeft().intValue());
        assertEquals(400, pair.getRight().width);
        assertEquals(300, pair.getRight().height);

        pair = controller.parseBitRate("1000", "300x400");
        assertEquals(1000, pair.getLeft().intValue());
        assertEquals(300, pair.getRight().width);
        assertEquals(400, pair.getRight().height);

        try {
            controller.parseBitRate("asdfl", null);
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            controller.parseBitRate("1000@300", null);
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            controller.parseBitRate("1000@300x400ZZ", null);
            fail();
        } catch (IllegalArgumentException e) {
        }
    }

}
