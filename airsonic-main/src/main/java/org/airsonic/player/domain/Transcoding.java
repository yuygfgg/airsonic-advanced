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
package org.airsonic.player.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.airsonic.player.util.StringUtil;

import java.util.Objects;

/**
 * Contains the configuration for a transcoding, i.e., a specification of how a given media format
 * should be converted to another.
 * <br/>
 * A transcoding may contain up to three steps. Typically you need to convert in several steps, for
 * instance from OGG to WAV to MP3.
 *
 * @author Sindre Mehus
 */
@AllArgsConstructor
@Getter
@Setter
public class Transcoding {

    // The system-generated ID.
    private Integer id;
    // The user-defined name.
    private String name;
    // The source formats, e.g., "ogg wav aac".
    private String sourceFormats;
    // The target format, e.g., "mp3".
    private String targetFormat;
    // The command to execute in step 1.
    private String step1;
    // The command to execute in step 2.
    private String step2;
    // The command to execute in step 3.
    private String step3;
    // Whether the transcoding should be automatically activated for all players.
    private boolean defaultActive;


    public String[] getSourceFormatsAsArray() {
        return StringUtil.split(sourceFormats);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Transcoding that = (Transcoding) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return (id != null ? id.hashCode() : 0);
    }
}
