package fr.nations.war;

import java.util.UUID;

public class War {

    private final UUID id;
    private final UUID attackerNationId;
    private final UUID defenderNationId;
    private final WarType type;
    private WarStatus status;
    private final long declaredAt;
    private long endsAt;
    private String reason;
    private int attackerKills;
    private int defenderKills;
    private String staffNote;
    private UUID validatedBy;

    public War(UUID id, UUID attackerNationId, UUID defenderNationId, WarType type,
               long declaredAt, long endsAt, String reason) {
        this.id = id;
        this.attackerNationId = attackerNationId;
        this.defenderNationId = defenderNationId;
        this.type = type;
        this.declaredAt = declaredAt;
        this.endsAt = endsAt;
        this.reason = reason;
        this.status = WarStatus.PENDING_VALIDATION;
        this.attackerKills = 0;
        this.defenderKills = 0;
    }

    public boolean isNationInvolved(UUID nationId) {
        return attackerNationId.equals(nationId) || defenderNationId.equals(nationId);
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > endsAt && status.isActive();
    }

    public UUID getId() { return id; }
    public UUID getAttackerNationId() { return attackerNationId; }
    public UUID getDefenderNationId() { return defenderNationId; }
    public WarType getType() { return type; }
    public WarStatus getStatus() { return status; }
    public void setStatus(WarStatus status) { this.status = status; }
    public long getDeclaredAt() { return declaredAt; }
    public long getEndsAt() { return endsAt; }
    public void setEndsAt(long endsAt) { this.endsAt = endsAt; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public int getAttackerKills() { return attackerKills; }
    public void setAttackerKills(int attackerKills) { this.attackerKills = attackerKills; }
    public void incrementAttackerKills() { this.attackerKills++; }
    public int getDefenderKills() { return defenderKills; }
    public void setDefenderKills(int defenderKills) { this.defenderKills = defenderKills; }
    public void incrementDefenderKills() { this.defenderKills++; }
    public String getStaffNote() { return staffNote; }
    public void setStaffNote(String staffNote) { this.staffNote = staffNote; }
    public UUID getValidatedBy() { return validatedBy; }
    public void setValidatedBy(UUID validatedBy) { this.validatedBy = validatedBy; }

    public long getTimeRemainingMillis() {
        return Math.max(0, endsAt - System.currentTimeMillis());
    }

    public String getFormattedTimeRemaining() {
        long remaining = getTimeRemainingMillis();
        long hours = remaining / 3600000;
        long minutes = (remaining % 3600000) / 60000;
        return hours + "h " + minutes + "min";
    }
}
