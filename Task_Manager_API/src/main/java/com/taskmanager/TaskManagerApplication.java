package com.taskmanager;

import com.taskmanager.config.AiSuggestDodProperties;
import com.taskmanager.config.RealtimeProperties;
import com.taskmanager.config.WebUiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties({WebUiProperties.class, RealtimeProperties.class, AiSuggestDodProperties.class})
@EnableScheduling
public class TaskManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskManagerApplication.class, args);
    }
}
