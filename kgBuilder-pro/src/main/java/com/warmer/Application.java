package com.warmer;

import com.warmer.base.util.Neo4jUtil;
import com.warmer.web.config.Neo4jConfig;
import com.warmer.web.config.SyncProperties;
import com.warmer.web.controller.GraphQueryController;
import com.warmer.web.controller.GraphSyncController;
import com.warmer.web.service.impl.AbilitySyncServiceImpl;
import com.warmer.web.service.impl.GraphSyncScheduler;
import com.warmer.web.service.impl.GraphSyncServiceImpl;
import com.warmer.web.service.impl.Neo4jServiceImpl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@ComponentScan(
        basePackages = {"com.warmer"},
        useDefaultFilters = false,
        includeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = Neo4jUtil.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = Neo4jConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SyncProperties.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = GraphSyncController.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = GraphQueryController.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = GraphSyncScheduler.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = GraphSyncServiceImpl.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = Neo4jServiceImpl.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = AbilitySyncServiceImpl.class)
        }
)
@EnableScheduling
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
