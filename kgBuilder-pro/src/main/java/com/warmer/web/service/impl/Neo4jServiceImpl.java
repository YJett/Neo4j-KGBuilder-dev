package com.warmer.web.service.impl;

import com.warmer.web.entity.AbilityKnowledge;
import com.warmer.web.entity.KnowledgePoint;
import com.warmer.web.service.Neo4jService;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class Neo4jServiceImpl implements Neo4jService {

    @Autowired
    private Driver driver;

    @Override
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

    @Override
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

    @Override
    public void updateKnowledgeRelationship(KnowledgePoint kp) {
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

    @Override
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

    @Override
    public void createAbilityKnowledgeNodesAndRelationships(List<AbilityKnowledge> akList) {
        try (Session session = driver.session()) {
            String cypherQuery = "UNWIND $params AS param " +
                    "MERGE (kp:KnowledgePoint {schId: param.schId, knowledgeId: param.knowledgeId}) " +
                    "MERGE (s:Skill {abilityId: param.abilityId}) " +
                    "MERGE (kp)-[:HAS_SKILL]->(s)";
            List<Map<String, Object>> parameters = akList.stream()
                    .map(ak -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("schId", ak.getSchId());
                        map.put("abilityId", ak.getAbilityId());
                        map.put("knowledgeId", ak.getKnowledgeId());
                        return map;
                    })
                    .collect(Collectors.toList());
            akList.forEach(ak -> log.info("Created relationship: {}", ak));

            session.run(cypherQuery, Collections.singletonMap("params", parameters));
        }
    }


    @Override
    public void updateAbilityKnowledgeNodes(List<AbilityKnowledge> akList) {
        try (Session session = driver.session()) {
            String cypherQuery = "UNWIND $params AS param " +
                    "MERGE (kp:KnowledgePoint {schId: param.schId, knowledgeId: param.knowledgeId}) " +
                    "ON MATCH SET kp.updateTime = param.updateTime " +
                    "MERGE (s:Skill {abilityId: param.abilityId}) " +
                    "ON MATCH SET s.updateTime = param.updateTime";
            List<Map<String, Object>> parameters = akList.stream()
                    .map(ak -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("schId", ak.getSchId());
                        map.put("abilityId", ak.getAbilityId());
                        map.put("knowledgeId", ak.getKnowledgeId());
                        map.put("updateTime", ak.getUpdateTime());
                        return map;
                    })
                    .collect(Collectors.toList());
            akList.forEach(ak -> log.info("Updated nodes: {}", ak));

            session.run(cypherQuery, Collections.singletonMap("params", parameters));
        }
    }


    @Override
    public void deleteAbilityKnowledgeNodes(List<AbilityKnowledge> akList) {
        try (Session session = driver.session()) {
            String cypherQuery = "UNWIND $params AS param " +
                    "MATCH (kp:KnowledgePoint {schId: param.schId, knowledgeId: param.knowledgeId})-[r:HAS_SKILL]->(s:Skill {abilityId: param.abilityId}) " +
                    "DELETE r";
            List<Map<String, Object>> parameters = akList.stream()
                    .map(ak -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("schId", ak.getSchId());
                        map.put("abilityId", ak.getAbilityId());
                        map.put("knowledgeId", ak.getKnowledgeId());
                        return map;
                    })
                    .collect(Collectors.toList());
            akList.forEach(ak -> log.info("Deleted relationship: {}", ak));

            session.run(cypherQuery, Collections.singletonMap("params", parameters));
        }
    }






}

