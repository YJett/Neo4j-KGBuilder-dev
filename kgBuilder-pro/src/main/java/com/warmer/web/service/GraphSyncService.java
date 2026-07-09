package com.warmer.web.service;

import java.time.LocalDateTime;
import java.util.Map;

public interface GraphSyncService {
    Map<String, Object> syncFull();

    Map<String, Object> syncIncremental(LocalDateTime since);

    Map<String, Object> syncKnowledgePoint(Integer schId, Integer knowledgeId);

    Map<String, Object> syncJobAbility(Integer abilityId);

    Map<String, Object> syncAbilityKnowledge(Integer schId, Integer abilityId, Integer knowledgeId);

    Map<String, Object> getStatus();
}
