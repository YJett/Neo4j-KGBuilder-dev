package com.warmer.web.service.impl;

import com.warmer.web.entity.KnowledgePoint;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class Neo4jService {

    @Autowired
    private Driver driver;

    public void createNodeAndRelationship(KnowledgePoint kp) {
        try (Session session = driver.session()) {
            String cypherQuery = "MERGE (n:KnowledgePoint {schId: $schId, knowledgeId: $knowledgeId, knowledgeNm: $knowledgeNm, flag: $flag, upLevel: $upLevel, createTime: $createTime, updateTime: $updateTime}) " +
                    "WITH n " +
                    "MATCH (m:KnowledgePoint {knowledgeId: $upLevel}) " +
                    "MERGE (n)-[:HAS_PARENT]->(m)";
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("schId", kp.getSchId());
            parameters.put("knowledgeId", kp.getKnowledgeId());
            parameters.put("knowledgeNm", kp.getKnowledgeNm());
            parameters.put("flag", kp.getFlag());
            parameters.put("upLevel", kp.getUpLevel());
            parameters.put("createTime", kp.getCreateTime());
            parameters.put("updateTime", kp.getUpdateTime());
            session.run(cypherQuery, parameters);
            log.info("insert kp {}",kp);
        }
    }

    public void updateNode(KnowledgePoint kp) {
        try (Session session = driver.session()) {
            String cypherQuery = "MATCH (n:KnowledgePoint {schId: $schId, knowledgeId: $knowledgeId}) " +
                    "SET n.knowledgeNm = $knowledgeNm, n.flag = $flag, n.upLevel = $upLevel, n.createTime = $createTime, n.updateTime = $updateTime";
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("schId", kp.getSchId());
            parameters.put("knowledgeId", kp.getKnowledgeId());
            parameters.put("knowledgeNm", kp.getKnowledgeNm());
            parameters.put("flag", kp.getFlag());
            parameters.put("upLevel", kp.getUpLevel());
            parameters.put("createTime", kp.getCreateTime());
            parameters.put("updateTime", kp.getUpdateTime());
            session.run(cypherQuery, parameters);
            log.info("Updated node: {}", kp);
        }
    }

    public void updateRelationship(KnowledgePoint kp) {
        try (Session session = driver.session()) {
            String cypherQuery = "MATCH (n:KnowledgePoint {schId: $schId, knowledgeId: $knowledgeId})-[r:HAS_PARENT]->() " +
                    "DELETE r " +
                    "WITH n " +
                    "MATCH (m:KnowledgePoint {knowledgeId: $upLevel}) " +
                    "MERGE (n)-[:HAS_PARENT]->(m)";
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("schId", kp.getSchId());
            parameters.put("knowledgeId", kp.getKnowledgeId());
            parameters.put("upLevel", kp.getUpLevel());
            session.run(cypherQuery, parameters);
            log.info("Updated relationship: {}", kp);

        }
    }

    public void deleteNode(KnowledgePoint kp) {
        try (Session session = driver.session()) {
            String cypherQuery = "MATCH (n:KnowledgePoint {schId: $schId, knowledgeId: $knowledgeId}) " +
                    "DETACH DELETE n";
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("schId", kp.getSchId());
            parameters.put("knowledgeId", kp.getKnowledgeId());
            session.run(cypherQuery, parameters);
            log.info("Deleted node with schId: {}, knowledgeId: {}", kp.getSchId(), kp.getKnowledgeId());
        }
    }




}

