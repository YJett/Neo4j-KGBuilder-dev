package com.warmer.web.controller;

import com.warmer.base.util.Neo4jUtil;
import com.warmer.base.util.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

@RestController
public class GraphQueryController {

    @PostMapping("/getCypherResult1")
    public R<HashMap<String, Object>> getCypherResult(@RequestBody CypherRequest request) {
        if (request == null || request.getCypher() == null || request.getCypher().trim().isEmpty()) {
            return R.error("cypher is required");
        }
        return query(request.getCypher());
    }

    @GetMapping("/getCypherResult")
    public R<HashMap<String, Object>> getCypherResult(@RequestParam("cypher") String cypher) {
        return query(cypher);
    }

    @PostMapping("/graph/query")
    public R<HashMap<String, Object>> queryGraph(@RequestBody CypherRequest request) {
        if (request == null || request.getCypher() == null || request.getCypher().trim().isEmpty()) {
            return R.error("cypher is required");
        }
        return query(request.getCypher());
    }

    private R<HashMap<String, Object>> query(String cypher) {
        if (!isReadOnlyQuery(cypher)) {
            return R.error("only read-only cypher is allowed");
        }
        return R.success(Neo4jUtil.getGraphNodeAndShip(cypher));
    }

    private boolean isReadOnlyQuery(String cypher) {
        String normalized = cypher.trim().replaceAll("\\s+", " ").toUpperCase();
        return (normalized.startsWith("MATCH ")
                || normalized.startsWith("OPTIONAL MATCH ")
                || normalized.startsWith("WITH ")
                || normalized.startsWith("CALL DB."))
                && !normalized.matches(".*\\b(CREATE|MERGE|DELETE|SET|REMOVE|DROP|LOAD|CALL\\s+APOC)\\b.*");
    }

    public static class CypherRequest {
        private String cypher;

        public String getCypher() {
            return cypher;
        }

        public void setCypher(String cypher) {
            this.cypher = cypher;
        }
    }
}
