package com.warmer.web.controller;

import com.warmer.base.util.R;
import com.warmer.web.request.GraphSyncRequest;
import com.warmer.web.service.GraphSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/sync")
public class GraphSyncController {

    @Autowired
    private GraphSyncService graphSyncService;

    @PostMapping("/full")
    public R<Map<String, Object>> syncFull() {
        return R.success(graphSyncService.syncFull(), "同步完成");
    }

    @PostMapping("/incremental")
    public R<Map<String, Object>> syncIncremental(@RequestBody(required = false) GraphSyncRequest request) {
        LocalDateTime since = null;
        if (request != null && request.getSince() != null && !request.getSince().trim().isEmpty()) {
            since = LocalDateTime.parse(request.getSince());
        }
        return R.success(graphSyncService.syncIncremental(since), "同步完成");
    }

    @PostMapping("/knowledge-point")
    public R<Map<String, Object>> syncKnowledgePoint(@RequestBody GraphSyncRequest request) {
        return R.success(graphSyncService.syncKnowledgePoint(request.getSchId(), request.getKnowledgeId()), "同步完成");
    }

    @PostMapping("/job-ability")
    public R<Map<String, Object>> syncJobAbility(@RequestBody GraphSyncRequest request) {
        return R.success(graphSyncService.syncJobAbility(request.getAbilityId()), "同步完成");
    }

    @PostMapping("/ability-knowledge")
    public R<Map<String, Object>> syncAbilityKnowledge(@RequestBody GraphSyncRequest request) {
        return R.success(graphSyncService.syncAbilityKnowledge(request.getSchId(), request.getAbilityId(), request.getKnowledgeId()), "同步完成");
    }

    @GetMapping("/status")
    public R<Map<String, Object>> getStatus() {
        return R.success(graphSyncService.getStatus(), "查询成功");
    }
}
