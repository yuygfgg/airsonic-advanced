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

import org.airsonic.player.util.Util;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;

@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties({AirsonicHomeConfig.class})
public class AirsonicConfigTest {

    @Nested
    @TestPropertySource(properties = {
        "airsonic.home=/tmp/test_airsonic",
        "libresonic.home=/tmp/test_libresonic"
    })
    public class AirsonicConfigTestWithProperties {

        @Autowired
        private AirsonicHomeConfig homeConfig;

        /**
         * Test method for {@link org.airsonic.player.config.AirsonicHomeConfig#getAirsonicHome()}.
         */
        @Test
        public void testGetAirsonicHome() {
            assertEquals("/tmp/test_airsonic", homeConfig.getAirsonicHome().toString());
        }

        /**
         * Test method for {@link org.airsonic.player.config.AirsonicHomeConfig#ensureDirectoryPresent()}.
         */
        @Test
        public void testEnsureDirectoryPresent() throws Exception {
            // check no exception is thrown when the directory is present
            try (MockedStatic<Files> filesMockedStatic = Mockito.mockStatic(Files.class, Mockito.CALLS_REAL_METHODS)) {
                filesMockedStatic.when(() -> Files.exists(homeConfig.getAirsonicHome())).thenReturn(true);
                assertDoesNotThrow(() -> homeConfig.ensureDirectoryPresent());
                filesMockedStatic.verify(() -> Files.createDirectory(homeConfig.getAirsonicHome()), never());
            }

            try (MockedStatic<Files> filesMockedStatic = Mockito.mockStatic(Files.class)) {
                filesMockedStatic.when(() -> Files.exists(homeConfig.getAirsonicHome())).thenReturn(false);
                filesMockedStatic.when(() -> Files.createDirectory(homeConfig.getAirsonicHome())).thenReturn(Paths.get("/tmp/airsonic"));
                assertDoesNotThrow(() -> homeConfig.ensureDirectoryPresent());
                filesMockedStatic.verify(() -> Files.createDirectory(homeConfig.getAirsonicHome()));
            }
        }
    }

    @Nested
    @TestPropertySource(properties = {
        "libresonic.home=/tmp/test_libresonic"
    })
    public class AirsonicConfigTestWithLibreSonicHome {

        @Autowired
        private AirsonicHomeConfig homeConfig;

        /**
         * Test method for {@link org.airsonic.player.config.AirsonicHomeConfig#getAirsonicHome()}.
         */
        @Test
        public void testGetAirsonicHome() {
            assertEquals("/tmp/test_libresonic", homeConfig.getAirsonicHome().toString());
        }
    }

    @Nested
    public class AirsonicConfigTestWitoutProperties {

        @Autowired
        private AirsonicHomeConfig homeConfig;

        /**
         * Test method for {@link org.airsonic.player.config.AirsonicHomeConfig#getAirsonicHome()}.
         */
        @ParameterizedTest
        @CsvSource({
            "true, c:/airsonic",
            "false, /var/airsonic"
        })
        public void testGetAirsonicHome(boolean isWindows, String expectedPath) {
            try (MockedStatic<Util> mockedUtil = Mockito.mockStatic(Util.class, Mockito.CALLS_REAL_METHODS)) {
                mockedUtil.when(Util::isWindows).thenReturn(isWindows);
                assertEquals(expectedPath, homeConfig.getAirsonicHome().toString());
            }
        }
    }
}
