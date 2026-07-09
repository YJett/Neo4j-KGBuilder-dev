package com.warmer.web.service.impl;

import com.warmer.web.config.SyncProperties;
import com.warmer.web.entity.AbilityKnowledge;
import com.warmer.web.entity.JobAbility;
import com.warmer.web.entity.KnowledgePoint;
import com.warmer.web.service.AbilitySyncService;
import com.warmer.web.service.GraphSyncService;
import com.warmer.web.service.Neo4jService;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class GraphSyncServiceImpl implements GraphSyncService {

    private static final String STATUS_TABLE = "_neo4j_sync_status";

    private JdbcTemplate boatJdbcTemplate;

    private HikariDataSource boatDataSource;

    @Autowired
    private Neo4jService neo4jService;

    @Autowired
    private AbilitySyncService abilitySyncService;

    @Autowired
    private SyncProperties syncProperties;

    private final Map<String, Object> lastStatus = new ConcurrentHashMap<>();
    private volatile LocalDateTime lastSuccessfulAt;

    @PostConstruct
    public void initStatusTable() {
        initBoatJdbcTemplate();
        try {
            boatJdbcTemplate.execute("CREATE TABLE IF NOT EXISTS " + STATUS_TABLE + " (" +
                    "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                    "sync_type VARCHAR(64) NOT NULL, " +
                    "sync_status VARCHAR(32) NOT NULL, " +
                    "affected_count INT NOT NULL DEFAULT 0, " +
                    "message TEXT, " +
                    "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                    ")");
        } catch (Exception ex) {
            log.warn("Unable to create Neo4j sync status table. Sync will continue with log-only status.", ex);
        }
    }

    @PreDestroy
    public void closeBoatDataSource() {
        if (boatDataSource != null) {
            boatDataSource.close();
        }
    }

    private void initBoatJdbcTemplate() {
        SyncProperties.Mysql mysql = syncProperties.getMysql();
        boatDataSource = new HikariDataSource();
        boatDataSource.setJdbcUrl(mysql.getUrl());
        boatDataSource.setUsername(mysql.getUsername());
        boatDataSource.setPassword(mysql.getPassword());
        boatDataSource.setDriverClassName(mysql.getDriverClassName());
        boatDataSource.setMaximumPoolSize(mysql.getMaximumPoolSize());
        boatDataSource.setMinimumIdle(mysql.getMinimumIdle());
        boatDataSource.setPoolName("boat-sync-pool");
        boatJdbcTemplate = new JdbcTemplate(boatDataSource);
    }

    @Override
    public Map<String, Object> syncFull() {
        return runSync("full", new Callable<Integer>() {
            @Override
            public Integer call() {
                neo4jService.clearAbilityKnowledgeRelationships();
                neo4jService.clearKnowledgePoints();
                abilitySyncService.clearAbilities();

                int count = 0;
                count += syncJobAbilityRows(null, null);
                count += syncKnowledgePointRows(null, null, null);
                abilitySyncService.rebuildAbilityRelationships();
                neo4jService.rebuildKnowledgeRelationships(null);
                count += syncAbilityKnowledgeRows(null, null, null, null);
                return count;
            }
        });
    }

    @Override
    public Map<String, Object> syncIncremental(LocalDateTime since) {
        final LocalDateTime effectiveSince = since != null ? since : defaultIncrementalSince();
        return runSync("incremental", new Callable<Integer>() {
            @Override
            public Integer call() {
                int count = 0;
                count += syncJobAbilityRows(null, effectiveSince);
                count += syncKnowledgePointRows(null, null, effectiveSince);
                count += syncAbilityKnowledgeRows(null, null, null, effectiveSince);
                abilitySyncService.rebuildAbilityRelationships();
                neo4jService.rebuildKnowledgeRelationships(null);
                return count;
            }
        });
    }

    @Override
    public Map<String, Object> syncKnowledgePoint(final Integer schId, final Integer knowledgeId) {
        return runSync("knowledge-point", new Callable<Integer>() {
            @Override
            public Integer call() {
                int count = syncKnowledgePointRows(schId, knowledgeId, null);
                if (count == 0 && schId != null && knowledgeId != null) {
                    KnowledgePoint kp = new KnowledgePoint();
                    kp.setSchId(schId);
                    kp.setKnowledgeId(knowledgeId);
                    neo4jService.deleteNode(kp);
                } else {
                    neo4jService.rebuildKnowledgeRelationships(schId);
                }
                return count;
            }
        });
    }

    @Override
    public Map<String, Object> syncJobAbility(final Integer abilityId) {
        return runSync("job-ability", new Callable<Integer>() {
            @Override
            public Integer call() {
                int count = syncJobAbilityRows(abilityId, null);
                if (count == 0 && abilityId != null) {
                    JobAbility ability = new JobAbility();
                    ability.setAbilityId(abilityId);
                    abilitySyncService.deleteAbility(ability);
                } else {
                    abilitySyncService.rebuildAbilityRelationships();
                }
                return count;
            }
        });
    }

    @Override
    public Map<String, Object> syncAbilityKnowledge(final Integer schId, final Integer abilityId, final Integer knowledgeId) {
        return runSync("ability-knowledge", new Callable<Integer>() {
            @Override
            public Integer call() {
                int count = syncAbilityKnowledgeRows(schId, abilityId, knowledgeId, null);
                if (count == 0 && schId != null && abilityId != null && knowledgeId != null) {
                    AbilityKnowledge ak = new AbilityKnowledge();
                    ak.setSchId(schId);
                    ak.setAbilityId(abilityId);
                    ak.setKnowledgeId(knowledgeId);
                    neo4jService.deleteAbilityKnowledgeNodes(Collections.singletonList(ak));
                }
                return count;
            }
        });
    }

    @Override
    public Map<String, Object> getStatus() {
        Map<String, Object> result = new HashMap<>(lastStatus);
        try {
            List<Map<String, Object>> rows = boatJdbcTemplate.queryForList(
                    "SELECT sync_type, sync_status, affected_count, message, create_time " +
                            "FROM " + STATUS_TABLE + " ORDER BY id DESC LIMIT 10");
            result.put("recent", rows);
        } catch (Exception ex) {
            result.put("recentError", ex.getMessage());
        }
        return result;
    }

    private LocalDateTime defaultIncrementalSince() {
        if (lastSuccessfulAt != null) {
            return lastSuccessfulAt.minusMinutes(5);
        }
        return LocalDateTime.now().minusMinutes(syncProperties.getIncrementalLookbackMinutes());
    }

    private Map<String, Object> runSync(String type, Callable<Integer> action) {
        if (!syncProperties.isEnabled()) {
            return statusMap(type, "SKIPPED", 0, "sync.enabled=false");
        }
        try {
            Integer affected = action.call();
            lastSuccessfulAt = LocalDateTime.now();
            Map<String, Object> result = statusMap(type, "SUCCESS", affected, "");
            recordStatus(type, "SUCCESS", affected, "");
            return result;
        } catch (Exception ex) {
            Map<String, Object> result = statusMap(type, "FAILED", 0, ex.getMessage());
            recordStatus(type, "FAILED", 0, ex.getMessage());
            log.error("Neo4j sync failed, type={}", type, ex);
            throw new RuntimeException("Neo4j sync failed: " + ex.getMessage(), ex);
        }
    }

    private Map<String, Object> statusMap(String type, String status, int affected, String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", type);
        result.put("status", status);
        result.put("affected", affected);
        result.put("message", message);
        result.put("time", LocalDateTime.now().toString());
        lastStatus.clear();
        lastStatus.putAll(result);
        return result;
    }

    private void recordStatus(String type, String status, int affected, String message) {
        try {
            boatJdbcTemplate.update(
                    "INSERT INTO " + STATUS_TABLE + " (sync_type, sync_status, affected_count, message) VALUES (?, ?, ?, ?)",
                    type, status, affected, message);
        } catch (Exception ex) {
            log.warn("Unable to record Neo4j sync status, type={}, status={}", type, status, ex);
        }
    }

    private int syncKnowledgePointRows(Integer schId, Integer knowledgeId, LocalDateTime since) {
        QueryParts query = new QueryParts(
                "SELECT schId AS schId, knowledgeId AS knowledgeId, knowledgeNm AS knowledgeNm, " +
                        "flag AS flag, upLevel AS upLevel, createTime AS createTime, updateTime AS updateTime " +
                        "FROM kp_knowledge_point WHERE 1=1");
        if (schId != null) {
            query.and("schId = ?", schId);
        }
        if (knowledgeId != null) {
            query.and("knowledgeId = ?", knowledgeId);
        }
        if (since != null) {
            query.and("updateTime >= ?", Timestamp.valueOf(since));
        }
        List<KnowledgePoint> rows = boatJdbcTemplate.query(query.sql(), query.args(), new KnowledgePointMapper());
        for (KnowledgePoint row : rows) {
            neo4jService.createNodeAndRelationship(row);
        }
        return rows.size();
    }

    private int syncJobAbilityRows(Integer abilityId, LocalDateTime since) {
        QueryParts query = new QueryParts(
                "SELECT abilityId AS abilityId, abilityNo AS abilityNo, abilityNm AS abilityNm, " +
                        "level AS level, upabilityId AS upabilityId, createTime AS createTime, " +
                        "updateTime AS updateTime, jobId AS jobId FROM job_ability WHERE 1=1");
        if (abilityId != null) {
            query.and("abilityId = ?", abilityId);
        }
        if (since != null) {
            query.and("updateTime >= ?", Timestamp.valueOf(since));
        }
        List<JobAbility> rows = boatJdbcTemplate.query(query.sql(), query.args(), new JobAbilityMapper());
        for (JobAbility row : rows) {
            abilitySyncService.createAbility(row);
        }
        return rows.size();
    }

    private int syncAbilityKnowledgeRows(Integer schId, Integer abilityId, Integer knowledgeId, LocalDateTime since) {
        QueryParts query = new QueryParts(
                "SELECT schId AS schId, abilityId AS abilityId, knowledgeId AS knowledgeId, " +
                        "createTime AS createTime, updateTime AS updateTime FROM jb_ability_knowledge WHERE 1=1");
        if (schId != null) {
            query.and("schId = ?", schId);
        }
        if (abilityId != null) {
            query.and("abilityId = ?", abilityId);
        }
        if (knowledgeId != null) {
            query.and("knowledgeId = ?", knowledgeId);
        }
        if (since != null) {
            query.and("updateTime >= ?", Timestamp.valueOf(since));
        }
        List<AbilityKnowledge> rows = boatJdbcTemplate.query(query.sql(), query.args(), new AbilityKnowledgeMapper());
        if (!rows.isEmpty()) {
            neo4jService.createAbilityKnowledgeNodesAndRelationships(rows);
        }
        return rows.size();
    }

    private static LocalDateTime readLocalDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private static Integer readInteger(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return null;
        }
        return new BigDecimal(text).intValue();
    }

    private static class KnowledgePointMapper implements RowMapper<KnowledgePoint> {
        @Override
        public KnowledgePoint mapRow(ResultSet rs, int rowNum) throws SQLException {
            KnowledgePoint kp = new KnowledgePoint();
            Integer schId = readInteger(rs, "schId");
            Integer knowledgeId = readInteger(rs, "knowledgeId");
            kp.setSchId(schId == null ? 0 : schId);
            kp.setKnowledgeId(knowledgeId == null ? 0 : knowledgeId);
            kp.setKnowledgeNm(rs.getString("knowledgeNm"));
            Integer flag = readInteger(rs, "flag");
            kp.setFlag(flag == null ? 0 : flag);
            kp.setUpLevel(readInteger(rs, "upLevel"));
            kp.setCreateTime(readLocalDateTime(rs, "createTime"));
            kp.setUpdateTime(readLocalDateTime(rs, "updateTime"));
            return kp;
        }
    }

    private static class JobAbilityMapper implements RowMapper<JobAbility> {
        @Override
        public JobAbility mapRow(ResultSet rs, int rowNum) throws SQLException {
            JobAbility ability = new JobAbility();
            ability.setAbilityId(readInteger(rs, "abilityId"));
            ability.setAbilityNo(readInteger(rs, "abilityNo"));
            ability.setAbilityNm(rs.getString("abilityNm"));
            ability.setLevel(readInteger(rs, "level"));
            ability.setUpabilityId(readInteger(rs, "upabilityId"));
            ability.setCreateTime(readLocalDateTime(rs, "createTime"));
            ability.setUpdateTime(readLocalDateTime(rs, "updateTime"));
            ability.setJobId(readInteger(rs, "jobId"));
            return ability;
        }
    }

    private static class AbilityKnowledgeMapper implements RowMapper<AbilityKnowledge> {
        @Override
        public AbilityKnowledge mapRow(ResultSet rs, int rowNum) throws SQLException {
            AbilityKnowledge ak = new AbilityKnowledge();
            Integer schId = readInteger(rs, "schId");
            Integer abilityId = readInteger(rs, "abilityId");
            Integer knowledgeId = readInteger(rs, "knowledgeId");
            ak.setSchId(schId == null ? 0 : schId);
            ak.setAbilityId(abilityId == null ? 0 : abilityId);
            ak.setKnowledgeId(knowledgeId == null ? 0 : knowledgeId);
            ak.setCreateTime(readLocalDateTime(rs, "createTime"));
            ak.setUpdateTime(readLocalDateTime(rs, "updateTime"));
            return ak;
        }
    }

    private static class QueryParts {
        private final StringBuilder sql;
        private final List<Object> args = new ArrayList<>();

        private QueryParts(String baseSql) {
            this.sql = new StringBuilder(baseSql);
        }

        private void and(String condition, Object value) {
            sql.append(" AND ").append(condition);
            args.add(value);
        }

        private String sql() {
            return sql.toString();
        }

        private Object[] args() {
            return args.toArray();
        }
    }
}
