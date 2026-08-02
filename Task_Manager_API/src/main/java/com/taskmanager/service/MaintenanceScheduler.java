package com.taskmanager.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MaintenanceScheduler {
    private final MaintenanceService maintenanceService;

    public MaintenanceScheduler(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @Scheduled(cron = "${taskmanager.maintenance.cron:0 0 * * * *}")
    public void run() {
        maintenanceService.runOnce();
    }
}
