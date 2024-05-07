package com.warmer.web.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgePoint {
    private int schId;
    private int knowledgeId;
    private String knowledgeNm;
    private int flag;
    private Integer upLevel; // This can be null in your table
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // getters and setters
}
