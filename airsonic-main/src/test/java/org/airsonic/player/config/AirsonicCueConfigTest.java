/*
 * This file is part of Airsonic.
 *
 * Airsonic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Airsonic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2023 (C) Y.Tory
 */
package org.airsonic.player.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AirsonicCueConfigTest {

    @Nested
    @EnableConfigurationProperties({AirsonicCueConfig.class})
    @ContextConfiguration(initializers = {ConfigDataApplicationContextInitializer.class})
    @ExtendWith(SpringExtension.class)
    @TestPropertySource(properties = {
        "airsonic.cue.enabled=true",
        "airsonic.cue.hide-indexed-files=true"
    })
    public class AirsonicCueConfigTestWithProperties {

        @Autowired
        private AirsonicCueConfig airsonicCueConfig;

        @Test
        public void testAirsonicCueConfig() {
            assertTrue(airsonicCueConfig.isEnabled());
            assertTrue(airsonicCueConfig.isHideIndexedFiles());
        }
    }

    @Nested
    @EnableConfigurationProperties({AirsonicCueConfig.class})
    @ContextConfiguration(initializers = {ConfigDataApplicationContextInitializer.class})
    @ExtendWith(SpringExtension.class)
    @TestPropertySource(properties = {
        "airsonic.cue.enabled=false",
        "airsonic.cue.hide-indexed-files=false"
    })
    public class AirsonicCueConfigTestFalseProperties {

        @Autowired
        private AirsonicCueConfig airsonicCueConfig;

        @Test
        public void testAirsonicCueConfig() {
            assertFalse(airsonicCueConfig.isEnabled());
            assertFalse(airsonicCueConfig.isHideIndexedFiles());
        }
    }
}
