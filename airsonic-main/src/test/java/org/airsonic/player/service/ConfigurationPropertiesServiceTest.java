/*
 * This file is part of Airsonic
 *
 *  Airsonic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * Airsonic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 * Copyright 2023 (C) Y.Tory
 */

package org.airsonic.player.service;

import org.apache.commons.configuration2.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConfigurationPropertiesServiceTest {

    @TempDir
    private static Path tempDir;

    @BeforeAll
    public static void setUp() {
        System.setProperty("airsonic.home", tempDir.toString());
    }

    @AfterEach
    public void resetInstance() throws IOException {
        Files.deleteIfExists(tempDir.resolve("airsonic.properties"));
        ConfigurationPropertiesService.reset();
    }


    @Test
    void testConstructor() throws Exception {
        ConfigurationPropertiesService service = ConfigurationPropertiesService.getInstance();
        // test file creation
        assertTrue(Files.exists(tempDir.resolve("airsonic.properties")));

        // test header comment
        assertEquals("", Files.readString(tempDir.resolve("airsonic.properties")));
        service.save();
        assertEquals("# " + ConfigurationPropertiesService.HEADER_COMMENT, Files.readAllLines(tempDir.resolve("airsonic.properties")).get(0));

        // test setting property
        assertFalse(service.containsKey("test"));
        service.setProperty("test", "value");
        assertTrue(service.containsKey("test"));
        assertEquals("value", service.getProperty("test"));
        service.save();
        assertEquals(3, Files.readAllLines(tempDir.resolve("airsonic.properties")).size());
        assertEquals("test=value", Files.readAllLines(tempDir.resolve("airsonic.properties")).get(2));

        // test clearing property
        service.clearProperty("test");
        assertFalse(service.containsKey("test"));
        assertNull(service.getProperty("test"));
        service.save();
        assertEquals(1, Files.readAllLines(tempDir.resolve("airsonic.properties")).size());
    }

    @Test
    void testGetConfiguration() {

        ConfigurationPropertiesService service = ConfigurationPropertiesService.getInstance();

        service.setProperty("testConfig", "value");
        Configuration config = service.getConfiguration();
        assertEquals("value", config.getString("testConfig"));

    }

    @Test
    void testReset() throws Exception {

        ConfigurationPropertiesService service = ConfigurationPropertiesService.getInstance();
        service.setProperty("testReset", "value");
        service.save();
        ConfigurationPropertiesService.reset();

        // test load from file on reset
        service = ConfigurationPropertiesService.getInstance();
        assertTrue(service.containsKey("testReset"));

    }
}
