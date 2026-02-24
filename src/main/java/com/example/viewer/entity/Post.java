package com.example.viewer.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Post {

    private String id;
    private String parentId;
    private String rootId;

    private Long fromId;

    private String content;

    private LocalDateTime createdAt;
}
