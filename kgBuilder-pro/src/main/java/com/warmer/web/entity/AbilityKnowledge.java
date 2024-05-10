package com.warmer.web.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AbilityKnowledge {
    private int schId;
    private int abilityId;
    private int knowledgeId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // Getters and setters...
}
