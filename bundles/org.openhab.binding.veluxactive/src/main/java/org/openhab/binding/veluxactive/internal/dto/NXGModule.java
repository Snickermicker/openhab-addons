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

import org.eclipse.jdt.annotation.Nullable;

/**
 * Device type NXG - Bridge
 *
 * @author Volker Daube - Initial contribution
 */
public class NXGModule extends BasicDeviceModule {
    private String subtype;
    private long setup_date;
    private boolean reachable;
    private String[] modules_bridged;
    private ScheduleLimit[] schedule_limits;
    private Capability[] capabilities;
    private boolean pincode_enabled;
    private boolean busy;
    private boolean calibrating;
    private int firmware_revision_netatmo;
    private String firmware_revision_thirdparty;
    private int hardware_version;
    private boolean is_raining;
    private boolean locked;
    private boolean locking;
    private String pairing;
    private boolean secure;
    private int wifi_strength;
    private String wifi_state;

    /**
     * Overrides the getID method to replace colons with dashes.
     * The NXGModule ID is format is similar to a MAC address .
     */
    @Override
    public @Nullable String getID() {
        String id = super.getID();
        if (id != null) {
            id = id.replace(':', '-');
        }
        return id;
    }

    public String getSubtype() {
        return subtype;
    }

    public void setSubtype(String subtype) {
        this.subtype = subtype;
    }

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

    public boolean isReachable() {
        return reachable;
    }

    public void setReachable(boolean reachable) {
        this.reachable = reachable;
    }

    public String[] getModulesBridged() {
        return modules_bridged;
    }

    public void setModulesBridged(String[] modulesBridged) {
        this.modules_bridged = modulesBridged;
    }

    public ScheduleLimit[] getScheduleLimits() {
        return schedule_limits;
    }

    public void setScheduleLimits(ScheduleLimit[] scheduleLimits) {
        this.schedule_limits = scheduleLimits;
    }

    public Capability[] getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(Capability[] capabilities) {
        this.capabilities = capabilities;
    }

    public boolean isPincodeEnabled() {
        return pincode_enabled;
    }

    public void setPincodeEnabled(boolean pincodeEnabled) {
        this.pincode_enabled = pincodeEnabled;
    }

    public boolean isBusy() {
        return busy;
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
    }

    public boolean isCalibrating() {
        return calibrating;
    }

    public void setCalibrating(boolean calibrating) {
        this.calibrating = calibrating;
    }

    public int getFirmwareRevisionNetatmo() {
        return firmware_revision_netatmo;
    }

    public void setFirmwareRevisionNetatmo(int firmwareRevisionNetatmo) {
        this.firmware_revision_netatmo = firmwareRevisionNetatmo;
    }

    public String getFirmwareRevisionThirdparty() {
        return firmware_revision_thirdparty;
    }

    public void setFirmwareRevisionThirdparty(String firmwareRevisionThirdparty) {
        this.firmware_revision_thirdparty = firmwareRevisionThirdparty;
    }

    public int getHardwareVersion() {
        return hardware_version;
    }

    public void setHardwareVersion(int hardwareVersion) {
        this.hardware_version = hardwareVersion;
    }

    public boolean isRaining() {
        return is_raining;
    }

    public void setRaining(boolean isRaining) {
        this.is_raining = isRaining;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public boolean isLocking() {
        return locking;
    }

    public void setLocking(boolean locking) {
        this.locking = locking;
    }

    public String getPairing() {
        return pairing;
    }

    public void setPairing(String pairing) {
        this.pairing = pairing;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public int getWifiStrength() {
        return wifi_strength;
    }

    public void setWifiStrength(int wifiStrength) {
        this.wifi_strength = wifiStrength;
    }

    public String getWifiState() {
        return wifi_state;
    }

    public void setWifiState(String wifiState) {
        this.wifi_state = wifiState;
    }
}
