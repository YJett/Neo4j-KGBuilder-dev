package com.warmer.web.service;

import com.warmer.web.entity.AbilityKnowledge;
import com.warmer.web.entity.KnowledgePoint;

import java.util.List;


public interface Neo4jService {
    public void createNodeAndRelationship(KnowledgePoint kp);
    public void updateNode(KnowledgePoint kp);

    public void updateKnowledgeRelationship(KnowledgePoint kp);

    public void deleteNode(KnowledgePoint kp);

    void createAbilityKnowledgeNodesAndRelationships(List<AbilityKnowledge> akList);
    void updateAbilityKnowledgeNodes(List<AbilityKnowledge> akList);
    void deleteAbilityKnowledgeNodes(List<AbilityKnowledge> akList);
}
