package com.taskmanager.domain;

public enum OrgRole {
    OWNER,
    ADMIN,
    MEMBER,
    VIEWER;

    public boolean canAdminister() {
        return this == OWNER || this == ADMIN;
    }

    public boolean canWriteContent() {
        return this == OWNER || this == ADMIN || this == MEMBER;
    }

    public boolean canRead() {
        return true;
    }
}
