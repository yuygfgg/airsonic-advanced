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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

public class AirsonicHomeConfigTest {

    @TempDir
    private static Path tempAirsonicDir;

    @TempDir
    private static Path tempLibresonicDir;

    @BeforeAll
    public static void setup() {
        System.clearProperty("airsonic.home");
        System.clearProperty("libresonic.home");
        System.setProperty("airsonic.home", tempAirsonicDir.toString());
        System.setProperty("libresonic.home", tempLibresonicDir.toString());
    }


    @AfterAll
    public static void cleanup() {
        System.clearProperty("airsonic.home");
        System.clearProperty("libresonic.home");
    }

    @Nested
    @EnableConfigurationProperties({AirsonicHomeConfig.class})
    @ContextConfiguration(initializers = {ConfigDataApplicationContextInitializer.class})
    @ExtendWith(SpringExtension.class)
    public class AirsonicConfigTestWithProperties {

        @Autowired
        private AirsonicHomeConfig homeConfig;


        /**
         * Test getters of AirsonicHomeConfig
         */
        @Test
        public void testGetters() {
            assertEquals(tempAirsonicDir.toString(), homeConfig.getAirsonicHome().toString());
            assertFalse(tempAirsonicDir.resolve("transcode").toFile().exists());
            assertEquals(tempAirsonicDir.resolve("transcode").toString(), homeConfig.getTranscodeDirectory().toString());
            assertTrue(tempAirsonicDir.resolve("transcode").toFile().exists());
            assertEquals(tempAirsonicDir.resolve("airsonic.properties").toString(), homeConfig.getPropertyFile().toString());
            assertEquals(tempAirsonicDir.resolve("airsonic.log").toString(), homeConfig.getDefaultLogFile().toString());
            assertEquals(
                "jdbc:hsqldb:file:" + tempAirsonicDir.resolve("db").resolve("airsonic").toString() + ";hsqldb.tx=mvcc;sql.enforce_size=false;sql.char_literal=false;sql.nulls_first=false;sql.pad_space=false;hsqldb.defrag_limit=50;shutdown=true",
                homeConfig.getDefaultJDBCUrl());
        }
    }

    @Nested
    public class TestGetAirsonicHome {

        /**
         * provide arguments for testGetAirsonicHome
         *
         * @return Stream of Arguments for testGetAirsonicHome
         */
        private static Stream<Arguments> provideArguments() {
            return Stream.of(
                Arguments.of(null, null, Util.isWindows() ? "c:\\airsonic" : "/var/airsonic"),   // default
                Arguments.of("", "", Util.isWindows() ? "c:\\airsonic" : "/var/airsonic"),   // default
                Arguments.of(" ", " ", Util.isWindows() ? "c:\\airsonic" : "/var/airsonic"),   // default
                Arguments.of(tempAirsonicDir.toString(), null, tempAirsonicDir.toString()),      // airsonic.home
                Arguments.of(tempAirsonicDir.toString(), "", tempAirsonicDir.toString()),      // airsonic.home
                Arguments.of(tempAirsonicDir.toString(), " ", tempAirsonicDir.toString()),      // airsonic.home
                Arguments.of(null, tempLibresonicDir.toString(), tempLibresonicDir.toString()), // libresonic.home
                Arguments.of("", tempLibresonicDir.toString(), tempLibresonicDir.toString()), // libresonic.home
                Arguments.of(" ", tempLibresonicDir.toString(), tempLibresonicDir.toString()), // libresonic.home
                Arguments.of(tempAirsonicDir.toString(), tempLibresonicDir.toString(), tempAirsonicDir.toString()) // airsonic.home > libresonic.home
            );
        }
        /**
         * Test method for {@link org.airsonic.player.config.AirsonicHomeConfig#getAirsonicHome()}.
         */
        @ParameterizedTest
        @MethodSource("provideArguments")
        public void testGetAirsonicHome(String airsonicHome, String libresonicHome, String expectedPath) {
            try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
                mockedFiles.when(() -> Files.exists(any(Path.class), any(LinkOption[].class))).thenReturn(true);
                AirsonicHomeConfig homeConfig = new AirsonicHomeConfig(airsonicHome, libresonicHome);
                assertEquals(expectedPath, homeConfig.getAirsonicHome().toString());
            }
        }
    }
}
