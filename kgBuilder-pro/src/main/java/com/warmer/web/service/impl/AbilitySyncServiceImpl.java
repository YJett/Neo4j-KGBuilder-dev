package com.warmer.web.service.impl;

import com.warmer.web.entity.JobAbility;
import com.warmer.web.service.AbilitySyncService;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AbilitySyncServiceImpl implements AbilitySyncService {

    @Autowired
    private Driver driver;

    @Override
    public void createAbility(JobAbility ab) {
        try (Session session = driver.session()) {
            String cypherQuery = "MERGE (n:Skill {abilityId: $abilityId, abilityNo: $abilityNo, abilityNm: $abilityNm, level: $level, upabilityId: $upabilityId, createTime: $createTime, updateTime: $updateTime, jobId: $jobId}) " +
                    "WITH n " +
                    "MATCH (m:Skill {abilityId: $upabilityId}) " +
                    "MERGE (n)-[:HAS_PARENT]->(m)";
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("abilityId", ab.getAbilityId());
            parameters.put("abilityNo", ab.getAbilityNo());
            parameters.put("abilityNm", ab.getAbilityNm());
            parameters.put("level", ab.getLevel());
            parameters.put("upabilityId", ab.getUpabilityId());
            parameters.put("createTime", ab.getCreateTime());
            parameters.put("updateTime", ab.getUpdateTime());
            parameters.put("jobId", ab.getJobId());
            session.run(cypherQuery, parameters);
            log.info("insert ability {}", ab);
        }
    }

    @Override
    public void updateAbility(JobAbility ab) {
        try (Session session = driver.session()) {
            String cypherQuery = "MATCH (n:Skill {abilityId: $abilityId}) " +
                    "SET n.abilityNo = $abilityNo, n.abilityNm = $abilityNm, n.level = $level, n.upabilityId = $upabilityId, n.createTime = $createTime, n.updateTime = $updateTime, n.jobId = $jobId";
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("abilityId", ab.getAbilityId());
            parameters.put("abilityNo", ab.getAbilityNo());
            parameters.put("abilityNm", ab.getAbilityNm());
            parameters.put("level", ab.getLevel());
            parameters.put("upabilityId", ab.getUpabilityId());
            parameters.put("createTime", ab.getCreateTime());
            parameters.put("updateTime", ab.getUpdateTime());
            parameters.put("jobId", ab.getJobId());
            session.run(cypherQuery, parameters);
            log.info("Updated ability: {}", ab);
        }
    }

    @Override
    public void deleteAbility(JobAbility ab) {
        try (Session session = driver.session()) {
            String cypherQuery = "MATCH (n:Skill {abilityId: $abilityId}) " +
                    "DETACH DELETE n";
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("abilityId", ab.getAbilityId());
            session.run(cypherQuery, parameters);
            log.info("Deleted ability with abilityId: {}", ab.getAbilityId());
        }
    }
}

