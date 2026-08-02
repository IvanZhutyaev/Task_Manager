package com.taskmanager.service;

import com.taskmanager.domain.Project;
import org.springframework.stereotype.Component;

@Component
public class BusinessRules {

    public boolean isStrict(Project project) {
        return project != null && project.isStrictBusinessRules();
    }
}
