package com.warmer.web.service.impl;

import com.alibaba.otter.canal.protocol.FlatMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.warmer.web.entity.AbilityKnowledge;
import com.warmer.web.entity.JobAbility;
import com.warmer.web.entity.KnowledgePoint;
import com.warmer.web.service.AbilitySyncService;
import com.warmer.web.service.Neo4jService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class KafkaConsumer {

    @Autowired
    private Neo4jService neo4jService;

    @Autowired
    private AbilitySyncService abilitySyncService;

    private final ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @KafkaListener(topics = "boat_kp_knowledge_point", groupId = "your_group_id")
    public void consume(String message) {
        System.out.println(message);
        try {
            FlatMessage kafkaMessage = objectMapper.readValue(message, FlatMessage.class);
            System.out.println(kafkaMessage);

            // Perform operation based on type
            String type = kafkaMessage.getType();

            // Extract data from kafkaMessage
            List<Map<String, String>> dataList = kafkaMessage.getData();

            for (Map<String, String> dataMap : dataList) {
                KnowledgePoint kp = new KnowledgePoint();
                kp.setSchId(Integer.parseInt(dataMap.get("schId")));
                kp.setKnowledgeId(Integer.parseInt(dataMap.get("knowledgeId")));
                kp.setKnowledgeNm(dataMap.get("knowledgeNm"));
                kp.setFlag(Integer.parseInt(dataMap.get("flag")));
                String upLevelStr = Objects.toString(dataMap.get("upLevel"), null);
                kp.setUpLevel(upLevelStr != null ? Integer.parseInt(upLevelStr) : 0);
                Timestamp createTimeStamp = Timestamp.valueOf(dataMap.get("createTime"));
                LocalDateTime createLocalDateTime = createTimeStamp.toLocalDateTime();
                kp.setCreateTime(createLocalDateTime);

                Timestamp updateTimeStamp = Timestamp.valueOf(dataMap.get("updateTime"));
                LocalDateTime updateLocalDateTime = updateTimeStamp.toLocalDateTime();
                kp.setUpdateTime(updateLocalDateTime);

                if ("INSERT".equals(type)) {
                    // Create the node and relationship in Neo4j
                    neo4jService.createNodeAndRelationship(kp);
                } else if ("UPDATE".equals(type)) {
                    // Handle update operation
                    // Extract old data from kafkaMessage
                    List<Map<String, String>> oldDataList = kafkaMessage.getOld();

                    for (Map<String, String> oldDataMap : oldDataList) {
                        boolean nodeUpdateNeeded = false;
                        boolean relationshipUpdateNeeded = false;

                        // Iterate over the keys in oldDataMap
                        for (String key : oldDataMap.keySet()) {
                            // Check if the value of the key has changed
                            if (!dataMap.get(key).equals(oldDataMap.get(key))) {
                                // If the key is 'upLevel', mark that a relationship update is needed
                                if (key.equals("upLevel")) {
                                    relationshipUpdateNeeded = true;
                                } else {
                                    // Otherwise, mark that a node update is needed
                                    nodeUpdateNeeded = true;
                                }
                            }
                        }
                        // After the loop, perform the updates if needed
                        if (nodeUpdateNeeded) {
                            neo4jService.updateNode(kp);
                        }
                        if (relationshipUpdateNeeded) {
                            neo4jService.updateKnowledgeRelationship(kp);
                        }
                    }

                } else if ("DELETE".equals(type)) {
                    // Handle delete operation
                    neo4jService.deleteNode(kp);
                }
            }

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }



    @KafkaListener(topics = "boat_job_ability", groupId = "your_group_id")
    public void consumeJobAbility(String message) {
        System.out.println(message);
        try {
            FlatMessage kafkaMessage = objectMapper.readValue(message, FlatMessage.class);
            System.out.println(kafkaMessage);

            // Perform operation based on type
            String type = kafkaMessage.getType();

            // Extract data from kafkaMessage
            List<Map<String, String>> dataList = kafkaMessage.getData();

            for (Map<String, String> dataMap : dataList) {
                JobAbility ab = new JobAbility();
                ab.setAbilityId(Integer.parseInt(dataMap.get("abilityId")));
                ab.setAbilityNo(dataMap.get("abilityNo"));
                ab.setAbilityNm(dataMap.get("abilityNm"));
                ab.setLevel(Integer.parseInt(dataMap.get("level")));
                String upabilityIdStr = Objects.toString(dataMap.get("upabilityId"), null);
                ab.setUpabilityId(upabilityIdStr != null ? Integer.parseInt(upabilityIdStr) : 0);
                Timestamp createTimeStamp = Timestamp.valueOf(dataMap.get("createTime"));
                LocalDateTime createLocalDateTime = createTimeStamp.toLocalDateTime();
                ab.setCreateTime(createLocalDateTime);

                Timestamp updateTimeStamp = Timestamp.valueOf(dataMap.get("updateTime"));
                LocalDateTime updateLocalDateTime = updateTimeStamp.toLocalDateTime();
                ab.setUpdateTime(updateLocalDateTime);

                String jobIdStr = Objects.toString(dataMap.get("jobId"), null);
                ab.setJobId(jobIdStr != null ? Integer.parseInt(jobIdStr) : 0);

                if ("INSERT".equals(type)) {
                    // Create the node and relationship in Neo4j
                    abilitySyncService.createAbility(ab);
                } else if ("UPDATE".equals(type)) {
                    // Handle update operation
                    // Extract old data from kafkaMessage
                    List<Map<String, String>> oldDataList = kafkaMessage.getOld();

                    for (Map<String, String> oldDataMap : oldDataList) {
                        boolean nodeUpdateNeeded = false;
                        boolean relationshipUpdateNeeded = false;

                        // Iterate over the keys in oldDataMap
                        for (String key : oldDataMap.keySet()) {
                            // Check if the value of the key has changed
                            if (!dataMap.get(key).equals(oldDataMap.get(key))) {
                                // If the key is 'upabilityId', mark that a relationship update is needed
                                if (key.equals("upabilityId")) {
                                    relationshipUpdateNeeded = true;
                                } else {
                                    // Otherwise, mark that a node update is needed
                                    nodeUpdateNeeded = true;
                                }
                            }
                        }
                        // After the loop, perform the updates if needed
                        if (nodeUpdateNeeded) {
                            abilitySyncService.updateAbility(ab);
                        }
                        if (relationshipUpdateNeeded) {
                            // Here you might need to implement a method to update the relationship
                            // abilitySyncService.updateAbilityRelationship(ab);
                        }
                    }

                } else if ("DELETE".equals(type)) {
                    // Handle delete operation
                    abilitySyncService.deleteAbility(ab);
                }
            }

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }




    @KafkaListener(topics = "boat_jb_ability_knowledge", groupId = "your_group_id")
    public void consumeAbilityKnowledge(String message) {
        System.out.println(message);
        try {
            FlatMessage kafkaMessage = objectMapper.readValue(message, FlatMessage.class);
            System.out.println(kafkaMessage);

            List<AbilityKnowledge> akList = new ArrayList<>();

            // Iterate over the data list
            for (Map<String, String> dataMap : kafkaMessage.getData()) {
                AbilityKnowledge ak = new AbilityKnowledge();
                ak.setSchId(Integer.parseInt(dataMap.get("schId")));
                ak.setAbilityId(Integer.parseInt(dataMap.get("abilityId")));
                ak.setKnowledgeId(Integer.parseInt(dataMap.get("knowledgeId")));

                Timestamp createTimeStamp = Timestamp.valueOf(dataMap.get("createTime"));
                LocalDateTime createLocalDateTime = createTimeStamp.toLocalDateTime();
                ak.setCreateTime(createLocalDateTime);

                Timestamp updateTimeStamp = Timestamp.valueOf(dataMap.get("updateTime"));
                LocalDateTime updateLocalDateTime = updateTimeStamp.toLocalDateTime();
                ak.setUpdateTime(updateLocalDateTime);

                akList.add(ak);
            }

            String type = kafkaMessage.getType();
            if ("INSERT".equals(type)) {
                neo4jService.createAbilityKnowledgeNodesAndRelationships(akList);
            } else if ("UPDATE".equals(type)) {
                neo4jService.updateAbilityKnowledgeNodes(akList);
            } else if ("DELETE".equals(type)) {
                neo4jService.deleteAbilityKnowledgeNodes(akList);
            }

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }






    @KafkaListener(topics = "knowledge", groupId = "your_group_id")
    public void consumeKnowledge(String message) {
        // ... (existing logic for processing "knowledge" topic messages)
        System.out.println(message);
    }

    @KafkaListener(topics = "boat_kp_knowledge_point1", groupId = "your_group_id")
    public void consumeMessage(String message, Acknowledgment acknowledgment) {
        try {
            // 处理消息的逻辑
            System.out.println("Received message: " + message);

            // 如果消息处理成功，手动确认消息
            acknowledgment.acknowledge();
        } catch (Exception e) {
            // 如果处理消息时发生异常，可以不确认消息，这样消息会被重新投递
            e.printStackTrace();
        }
    }

    @KafkaListener(topics = "boat_jb_job", groupId = "your_group_id")
    public void consumeBoatJob(String message) {
        // Logic for processing "boat_jb_job" topic messages
        System.out.println(message);
    }
}
