package com.warmer.web.service.impl;

import com.warmer.web.entity.KnowledgePoint;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

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
        }
    }

}

