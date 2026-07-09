package com.warmer.web.service.impl;

import com.warmer.web.config.SyncProperties;
import com.warmer.web.service.GraphSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class GraphSyncScheduler {

    @Autowired
    private SyncProperties syncProperties;

    @Autowired
    private GraphSyncService graphSyncService;

    @Scheduled(
            fixedDelayString = "${sync.fixed-delay-ms:900000}",
            initialDelayString = "${sync.initial-delay-ms:60000}"
    )
    public void runIncrementalSync() {
        if (!syncProperties.isEnabled() || !syncProperties.isScheduled()) {
            return;
        }
        LocalDateTime since = LocalDateTime.now().minusMinutes(syncProperties.getIncrementalLookbackMinutes());
        try {
            graphSyncService.syncIncremental(since);
        } catch (Exception ex) {
            log.error("Neo4j incremental sync failed", ex);
        }
    }
}
