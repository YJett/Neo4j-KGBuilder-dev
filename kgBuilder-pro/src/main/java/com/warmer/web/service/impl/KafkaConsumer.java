package com.warmer.web.service.impl;

import com.alibaba.otter.canal.client.kafka.protocol.KafkaMessage;
import com.alibaba.otter.canal.protocol.FlatMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.warmer.web.entity.KnowledgePoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import com.alibaba.otter.canal.client.kafka.KafkaCanalConnector;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class KafkaConsumer {

    @Autowired
    private Neo4jService neo4jService;

    private final ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @KafkaListener(topics = "boat_kp_knowledge_point", groupId = "your_group_id")
    public void consume(String message) {
        System.out.println(message);
        try {
            FlatMessage kafkaMessage = objectMapper.readValue(message, FlatMessage.class);
            // Now you can use kafkaMessage as you wish
            // For example, let's print it out
            System.out.println(kafkaMessage);

            // Extract data from kafkaMessage
            Map<String, String> dataMap = kafkaMessage.getData().get(0); // Assuming there's at least one item in the data list

            KnowledgePoint kp = new KnowledgePoint();
            kp.setSchId(Integer.parseInt(dataMap.get("schId")));
            kp.setKnowledgeId(Integer.parseInt(dataMap.get("knowledgeId")));
            kp.setKnowledgeNm(dataMap.get("knowledgeNm"));
            kp.setFlag(Integer.parseInt(dataMap.get("flag")));
            kp.setUpLevel(Integer.parseInt(dataMap.get("upLevel")));

            Timestamp createTimeStamp = Timestamp.valueOf(dataMap.get("createTime"));
            LocalDateTime createLocalDateTime = createTimeStamp.toLocalDateTime();
            kp.setCreateTime(createLocalDateTime);

            Timestamp updateTimeStamp = Timestamp.valueOf(dataMap.get("updateTime"));
            LocalDateTime updateLocalDateTime = updateTimeStamp.toLocalDateTime();
            kp.setUpdateTime(updateLocalDateTime);



            // Perform operation based on type
            String type = kafkaMessage.getType();
            if ("INSERT".equals(type)) {
                // Create the node and relationship in Neo4j
                neo4jService.createNodeAndRelationship(kp);
            } else if ("UPDATE".equals(type)) {
                // Handle update operation
            } else if ("DELETE".equals(type)) {
                // Handle delete operation
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
    public void consumeBoatKnowledgePoint(String message) {
        // Logic for processing "boat_kp_knowledge_point" topic messages
        System.out.println(message);
    }

    @KafkaListener(topics = "boat_jb_job", groupId = "your_group_id")
    public void consumeBoatJob(String message) {
        // Logic for processing "boat_jb_job" topic messages
        System.out.println(message);
    }
}
