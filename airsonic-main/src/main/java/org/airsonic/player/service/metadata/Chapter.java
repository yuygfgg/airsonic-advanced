package org.airsonic.player.service.metadata;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Chapter {

    private final Integer id;

    private final String timeBase;

    private final Long start;

    private final String startTime;

    private final Long end;

    private final String endTime;

    private final String title;

    private Map<String, String> tags = new HashMap<>();

    public Chapter(Integer id, String timeBase, Long start, String startTime, Long end, String endTime, String title, Map<String, String> tags) {
        this.id = id;
        this.timeBase = StringUtils.trimToNull(timeBase);
        this.start = start;
        this.startTime = StringUtils.trimToNull(startTime);
        this.end = end;
        this.endTime = StringUtils.trimToNull(endTime);
        this.title = StringUtils.trimToNull(title);
        this.tags = tags == null ? new HashMap<>() : tags;
    }

    public Integer getId() {
        return id;
    }

    public String getTimeBase() {
        return timeBase;
    }

    public Long getStart() {
        return start;
    }

    public String getStartTime() {
        return startTime;
    }

    public Double getStartTimeSeconds() {
        return Optional.ofNullable(startTime).map(Double::parseDouble).orElse(null);
    }

    public Long getEnd() {
        return end;
    }

    public String getEndTime() {
        return endTime;
    }

    public Double getEndTimeSeconds() {
        return Optional.ofNullable(endTime).map(Double::parseDouble).orElse(null);
    }

    public String getTitle() {
        if (title == null) {
            return this.tags.get("title");
        }
        return title;
    }

    public Map<String, String> getTags() {
        return tags;
    }

}
