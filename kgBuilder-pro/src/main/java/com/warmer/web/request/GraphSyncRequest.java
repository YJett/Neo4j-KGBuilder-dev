package com.warmer.web.request;

import lombok.Data;

@Data
public class GraphSyncRequest {
    private Integer schId;
    private Integer knowledgeId;
    private Integer abilityId;
    private String since;
}
