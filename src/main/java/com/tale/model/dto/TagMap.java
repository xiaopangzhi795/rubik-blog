package com.tale.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class TagMap {
    String type;
    List<Tag> tags;

    public TagMap(String type, List<Tag> tags) {
        this.type = type;
        this.tags = tags;
    }
}
