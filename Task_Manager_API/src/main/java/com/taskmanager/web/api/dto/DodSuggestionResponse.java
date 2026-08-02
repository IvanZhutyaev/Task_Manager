package com.taskmanager.web.api.dto;

import java.util.List;

public record DodSuggestionResponse(List<SuggestedItem> items) {

    public record SuggestedItem(String title, int position) {
    }
}
