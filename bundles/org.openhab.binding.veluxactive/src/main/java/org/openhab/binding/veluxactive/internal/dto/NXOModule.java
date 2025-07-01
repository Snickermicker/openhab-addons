/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.veluxactive.internal.dto;

import java.time.Instant;
import java.time.ZoneId;

/**
 * Device type NXG - RollerShutter, Blind, Window
 *
 * @author Volker Daube - Initial contribution
 */
public class NXOModule extends BasicDeviceModule {
    private long setup_date;
    private String room_id;
    private String bridge;
    private String velux_type;
    private String group_id;
    private int current_position;
    private int firmware_revision;
    private String manufacturer;
    private String mode;
    private int rain_position;
    private boolean reachable;
    private int secure_position;
    private boolean silent;
    private int target_position;

    public long getSetupDateSeconds() {
        return setup_date;
    }

    public String getSetupDate() {
        Instant instant = Instant.ofEpochSecond(setup_date);
        return instant.atZone(ZoneId.systemDefault()).toString();
    }

    public void setSetupDate(long setupDate) {
        this.setup_date = setupDate;
    }

    public String getRoomID() {
        return room_id;
    }

    public void setRoomID(String roomID) {
        this.room_id = roomID;
    }

    public String getBridge() {
        return bridge;
    }

    public void setBridge(String bridge) {
        this.bridge = bridge;
    }

    public String getVeluxType() {
        return velux_type;
    }

    public void setVeluxType(String veluxType) {
        this.velux_type = veluxType;
    }

    public String getGroupID() {
        return group_id;
    }

    public void setGroupID(String groupID) {
        this.group_id = groupID;
    }

    public int getCurrentPosition() {
        return current_position;
    }

    public void setCurrentPosition(int currentPosition) {
        if (currentPosition < 0 || currentPosition > 100) {
            throw new IllegalArgumentException("Position must be between 0 and 100");
        }
        this.current_position = currentPosition;
    }

    public int getFirmwareRevision() {
        return firmware_revision;
    }

    public void setFirmwareRevision(int firmwareRevision) {
        this.firmware_revision = firmwareRevision;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public int getRainPosition() {
        return rain_position;
    }

    public void setRainPosition(int rainPosition) {
        this.rain_position = rainPosition;
    }

    public boolean isReachable() {
        return reachable;
    }

    public void setReachable(boolean reachable) {
        this.reachable = reachable;
    }

    public int getSecurePosition() {
        return secure_position;
    }

    public void setSecurePosition(int securePosition) {
        this.secure_position = securePosition;
    }

    public boolean isSilent() {
        return silent;
    }

    public void setSilent(boolean silent) {
        this.silent = silent;
    }

    public int getTargetPosition() {
        return target_position;
    }

    public void setTargetPosition(int targetPosition) {
        this.target_position = targetPosition;
    }

    public boolean isBlind() {
        return "blind".equals(velux_type);
    }

    public boolean isWindow() {
        return "window".equals(velux_type);
    }
}
