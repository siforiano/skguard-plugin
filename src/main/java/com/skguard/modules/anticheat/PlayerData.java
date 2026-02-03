package com.skguard.modules.anticheat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;

public class PlayerData {

    private final UUID uuid;
    private final Map<Check, Integer> violations = new HashMap<>();
    
    // Movement tracking
    private Location lastLocation;
    private double lastDeltaY;
    private boolean lastOnGround;
    private long lastMoveTime;
    
    // Combat tracking
    private long lastAttackTime;
    private int clicksThisSecond;
    private long lastClickTime;
    private float lastYaw, lastPitch;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public int incrementVL(Check check) {
        int vl = violations.getOrDefault(check, 0) + 1;
        violations.put(check, vl);
        return vl;
    }

    public int getVL(Check check) {
        return violations.getOrDefault(check, 0);
    }

    // Getters and Setters for tracking data
    public Location getLastLocation() { return lastLocation; }
    public void setLastLocation(Location lastLocation) { this.lastLocation = lastLocation; }
    public double getLastDeltaY() { return lastDeltaY; }
    public void setLastDeltaY(double lastDeltaY) { this.lastDeltaY = lastDeltaY; }
    public boolean isLastOnGround() { return lastOnGround; }
    public void setLastOnGround(boolean lastOnGround) { this.lastOnGround = lastOnGround; }
    public long getLastMoveTime() { return lastMoveTime; }
    public void setLastMoveTime(long lastMoveTime) { this.lastMoveTime = lastMoveTime; }
    public long getLastAttackTime() { return lastAttackTime; }
    public void setLastAttackTime(long lastAttackTime) { this.lastAttackTime = lastAttackTime; }
    public int getClicksThisSecond() { return clicksThisSecond; }
    public void setClicksThisSecond(int clicksThisSecond) { this.clicksThisSecond = clicksThisSecond; }
    public long getLastClickTime() { return lastClickTime; }
    public void setLastClickTime(long lastClickTime) { this.lastClickTime = lastClickTime; }
    public float getLastYaw() { return lastYaw; }
    public void setLastYaw(float lastYaw) { this.lastYaw = lastYaw; }
    public float getLastPitch() { return lastPitch; }
    public void setLastPitch(float lastPitch) { this.lastPitch = lastPitch; }
}

