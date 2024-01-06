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
package org.airsonic.player.util;

import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test of {@link FileUtil}.
 *
 * @author Sindre Mehus
 */
public class FileUtilTestCase {

    @Test
    public void testIsFileInFolder() {
        assertTrue(FileUtil.isFileInFolder(Paths.get("/music/foo.mp3"), Paths.get("/")));

        assertTrue(FileUtil.isFileInFolder(Paths.get("/music/foo.mp3"), Paths.get("/music")));
        assertTrue(FileUtil.isFileInFolder(Paths.get("/music/foo.mp3"), Paths.get("/music/")));
        assertTrue(FileUtil.isFileInFolder(Paths.get("\\music\\foo.mp3"), Paths.get("\\music")));
        assertTrue(FileUtil.isFileInFolder(Paths.get("\\music\\foo.mp3"), Paths.get("\\music\\")));
        assertTrue(FileUtil.isFileInFolder(Paths.get("music/foo.mp3"), Paths.get("music")));
        assertTrue(FileUtil.isFileInFolder(Paths.get("music\\foo.mp3"), Paths.get("music")));
        assertTrue(FileUtil.isFileInFolder(Paths.get("music/foo.mp3"), Paths.get("music/")));
        assertTrue(FileUtil.isFileInFolder(Paths.get("music\\foo.mp3"), Paths.get("music\\")));

        assertFalse(FileUtil.isFileInFolder(Paths.get(""), Paths.get("/tmp")));
        assertFalse(FileUtil.isFileInFolder(Paths.get("foo.mp3"), Paths.get("/tmp")));
        assertFalse(FileUtil.isFileInFolder(Paths.get("/music/foo.mp3"), Paths.get("/tmp")));
        assertFalse(FileUtil.isFileInFolder(Paths.get("/music/foo.mp3"), Paths.get("/tmp/music")));
        assertFalse(FileUtil.isFileInFolder(Paths.get("music/foo.mp3"), Paths.get("/music")));

        // identity tests
        assertTrue(FileUtil.isFileInFolder(Paths.get("/music/a"), Paths.get("/music/a")));
        assertTrue(FileUtil.isFileInFolder(Paths.get("/music/a"), Paths.get("/music/a/")));
        assertTrue(FileUtil.isFileInFolder(Paths.get("/music/a/"), Paths.get("/music/a/")));
        assertTrue(FileUtil.isFileInFolder(Paths.get("/music/a/"), Paths.get("/music/a")));
        assertFalse(FileUtil.isFileInFolder(Paths.get("/music/a2"), Paths.get("/music/a")));
        assertFalse(FileUtil.isFileInFolder(Paths.get("/music/a"), Paths.get("/music/a2")));

        // Test with redundant references
        assertTrue(FileUtil.isFileInFolder(Paths.get("/music/foo..mp3"), Paths.get("/music")));
        assertTrue(FileUtil.isFileInFolder(Paths.get("/music/foo.."), Paths.get("/music")));
        assertTrue(FileUtil.isFileInFolder(Paths.get("/music/foo.../"), Paths.get("/music")));
        assertTrue(FileUtil.isFileInFolder(Paths.get("/music/foo/.."), Paths.get("/music")));
        assertFalse(FileUtil.isFileInFolder(Paths.get("../music/foo"), Paths.get("/music")));
        assertTrue(FileUtil.isFileInFolder(Paths.get("/../music/foo"), Paths.get("/music")));
        assertFalse(FileUtil.isFileInFolder(Paths.get("/music/../foo"), Paths.get("/music")));
        assertFalse(FileUtil.isFileInFolder(Paths.get("/music/../bar/../foo"), Paths.get("/music")));
        assertTrue(FileUtil.isFileInFolder(Paths.get("/music\\foo\\.."), Paths.get("/music")));
        assertFalse(FileUtil.isFileInFolder(Paths.get("..\\music/foo"), Paths.get("/music")));
        assertFalse(FileUtil.isFileInFolder(Paths.get("/music\\../foo"), Paths.get("/music")));
        assertFalse(FileUtil.isFileInFolder(Paths.get("/music/..\\bar/../foo"), Paths.get("/music")));
    }
}
