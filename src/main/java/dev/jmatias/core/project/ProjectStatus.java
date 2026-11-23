package dev.jmatias.core.project;

/** Only ACTIVE projects resolve as a tenant. Must match the project_status_valid CHECK. */
public enum ProjectStatus {
    PROVISIONING,
    ACTIVE,
    SUSPENDED
}
