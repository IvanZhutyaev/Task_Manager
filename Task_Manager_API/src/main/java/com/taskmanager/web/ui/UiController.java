package com.taskmanager.web.ui;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@ConditionalOnProperty(prefix = "app.web-ui", name = "enabled", havingValue = "true", matchIfMissing = true)
public class UiController {

    @GetMapping("/")
    public String home() {
        return "redirect:/projects";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping("/projects")
    public String projects() {
        return "projects";
    }

    @GetMapping("/projects/{projectId}")
    public String projectDetail() {
        return "project-detail";
    }

    @GetMapping("/boards/{boardId}")
    public String board() {
        return "board";
    }
}
