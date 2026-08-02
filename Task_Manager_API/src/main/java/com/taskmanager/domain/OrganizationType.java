package com.taskmanager.domain;

public enum OrganizationType {
    LOCAL,
    COMMERCIAL;

    public int maxMembers() {
        return this == LOCAL ? 20 : 200;
    }

    public int maxTeams() {
        return this == LOCAL ? 5 : 50;
    }

    public int maxProjects() {
        return this == LOCAL ? 10 : 100;
    }
}
