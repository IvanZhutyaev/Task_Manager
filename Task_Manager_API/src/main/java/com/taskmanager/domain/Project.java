package com.taskmanager.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "strict_business_rules", nullable = false)
    private boolean strictBusinessRules = false;

    @Column(name = "custom_workflow_enabled", nullable = false)
    private boolean customWorkflowEnabled = false;

    @Column(name = "capacity_limit_hours")
    private BigDecimal capacityLimitHours;

    @Column(name = "sla_warning_days")
    private Integer slaWarningDays;

    @Column(name = "sla_escalate_days")
    private Integer slaEscalateDays;

    @Column(name = "dod_templates_enabled", nullable = false)
    private boolean dodTemplatesEnabled = false;

    @Column(name = "require_approval_for_done", nullable = false)
    private boolean requireApprovalForDone = false;

    @Column(name = "priority_queue_rules", nullable = false)
    private boolean priorityQueueRules = false;

    @Column(name = "max_unfinished_sprint_percent")
    private Integer maxUnfinishedSprintPercent;

    @Column(name = "auto_archive_done_days")
    private Integer autoArchiveDoneDays;

    @Column(name = "restrict_high_priority_to_owner", nullable = false)
    private boolean restrictHighPriorityToOwner = false;

    @Column(name = "time_in_status_alerts_days")
    private Integer timeInStatusAlertsDays;

    @Column(name = "block_done_on_open_risks", nullable = false)
    private boolean blockDoneOnOpenRisks = false;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isStrictBusinessRules() {
        return strictBusinessRules;
    }

    public void setStrictBusinessRules(boolean strictBusinessRules) {
        this.strictBusinessRules = strictBusinessRules;
    }

    public boolean isCustomWorkflowEnabled() {
        return customWorkflowEnabled;
    }

    public void setCustomWorkflowEnabled(boolean customWorkflowEnabled) {
        this.customWorkflowEnabled = customWorkflowEnabled;
    }

    public BigDecimal getCapacityLimitHours() {
        return capacityLimitHours;
    }

    public void setCapacityLimitHours(BigDecimal capacityLimitHours) {
        this.capacityLimitHours = capacityLimitHours;
    }

    public Integer getSlaWarningDays() {
        return slaWarningDays;
    }

    public void setSlaWarningDays(Integer slaWarningDays) {
        this.slaWarningDays = slaWarningDays;
    }

    public Integer getSlaEscalateDays() {
        return slaEscalateDays;
    }

    public void setSlaEscalateDays(Integer slaEscalateDays) {
        this.slaEscalateDays = slaEscalateDays;
    }

    public boolean isDodTemplatesEnabled() {
        return dodTemplatesEnabled;
    }

    public void setDodTemplatesEnabled(boolean dodTemplatesEnabled) {
        this.dodTemplatesEnabled = dodTemplatesEnabled;
    }

    public boolean isRequireApprovalForDone() {
        return requireApprovalForDone;
    }

    public void setRequireApprovalForDone(boolean requireApprovalForDone) {
        this.requireApprovalForDone = requireApprovalForDone;
    }

    public boolean isPriorityQueueRules() {
        return priorityQueueRules;
    }

    public void setPriorityQueueRules(boolean priorityQueueRules) {
        this.priorityQueueRules = priorityQueueRules;
    }

    public Integer getMaxUnfinishedSprintPercent() {
        return maxUnfinishedSprintPercent;
    }

    public void setMaxUnfinishedSprintPercent(Integer maxUnfinishedSprintPercent) {
        this.maxUnfinishedSprintPercent = maxUnfinishedSprintPercent;
    }

    public Integer getAutoArchiveDoneDays() {
        return autoArchiveDoneDays;
    }

    public void setAutoArchiveDoneDays(Integer autoArchiveDoneDays) {
        this.autoArchiveDoneDays = autoArchiveDoneDays;
    }

    public boolean isRestrictHighPriorityToOwner() {
        return restrictHighPriorityToOwner;
    }

    public void setRestrictHighPriorityToOwner(boolean restrictHighPriorityToOwner) {
        this.restrictHighPriorityToOwner = restrictHighPriorityToOwner;
    }

    public Integer getTimeInStatusAlertsDays() {
        return timeInStatusAlertsDays;
    }

    public void setTimeInStatusAlertsDays(Integer timeInStatusAlertsDays) {
        this.timeInStatusAlertsDays = timeInStatusAlertsDays;
    }

    public boolean isBlockDoneOnOpenRisks() {
        return blockDoneOnOpenRisks;
    }

    public void setBlockDoneOnOpenRisks(boolean blockDoneOnOpenRisks) {
        this.blockDoneOnOpenRisks = blockDoneOnOpenRisks;
    }

    public boolean canBeManagedBy(User user) {
        return owner != null && owner.getId() != null && owner.getId().equals(user.getId());
    }

    public boolean canBeViewedBy(User user) {
        return owner != null && owner.getId() != null && owner.getId().equals(user.getId());
    }
}
