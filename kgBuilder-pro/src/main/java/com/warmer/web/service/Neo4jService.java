package com.warmer.web.service;

import com.warmer.web.entity.AbilityKnowledge;
import com.warmer.web.entity.KnowledgePoint;

import java.util.List;


public interface Neo4jService {
    void createNodeAndRelationship(KnowledgePoint kp);

    void updateNode(KnowledgePoint kp);

    void updateKnowledgeRelationship(KnowledgePoint kp);

    void deleteNode(KnowledgePoint kp);

    void clearKnowledgePoints();

    void rebuildKnowledgeRelationships(Integer schId);

    void createAbilityKnowledgeNodesAndRelationships(List<AbilityKnowledge> akList);

    void updateAbilityKnowledgeNodes(List<AbilityKnowledge> akList);

    void deleteAbilityKnowledgeNodes(List<AbilityKnowledge> akList);

    void clearAbilityKnowledgeRelationships();
}
