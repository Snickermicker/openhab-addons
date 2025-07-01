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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.annotation.Nullable;

/**
 * @author Volker Daube - Initial contribution
 */
public class Home extends ElementWithID {
    private String name;
    private int altitude;
    private float[] coordinates;
    private String country;
    private String timezone;
    private String city;
    private String currency_code;
    private int nb_users;
    private DataVersions data_versions;
    private boolean place_improved;
    private boolean trust_location;
    private boolean therm_absence_notification;
    private boolean therm_absence_autoaway;
    private Room[] rooms;
    private BasicDeviceModule[] modules;
    private Schedule[] schedules;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAltitude() {
        return altitude;
    }

    public void setAltitude(int altitude) {
        this.altitude = altitude;
    }

    public float[] getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(float[] coordinates) {
        this.coordinates = coordinates;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCurrencyCode() {
        return currency_code;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currency_code = currencyCode;
    }

    public int getNbUsers() {
        return nb_users;
    }

    public void setNbUsers(int nbUsers) {
        this.nb_users = nbUsers;
    }

    public DataVersions getDataVersions() {
        return data_versions;
    }

    public void setDataVersions(DataVersions dataVersions) {
        this.data_versions = dataVersions;
    }

    public boolean isPlaceImproved() {
        return place_improved;
    }

    public void setPlaceImproved(boolean placeImproved) {
        this.place_improved = placeImproved;
    }

    public boolean isTrustLocation() {
        return trust_location;
    }

    public void setTrustLocation(boolean trustLocation) {
        this.trust_location = trustLocation;
    }

    public boolean isThermAbsenceNotification() {
        return therm_absence_notification;
    }

    public void setThermAbsenceNotification(boolean thermAbsenceNotification) {
        this.therm_absence_notification = thermAbsenceNotification;
    }

    public boolean isThermAbsenceAutoaway() {
        return therm_absence_autoaway;
    }

    public void setThermAbsenceAutoaway(boolean thermAbsenceAutoaway) {
        this.therm_absence_autoaway = thermAbsenceAutoaway;
    }

    public Room[] getRooms() {
        return rooms;
    }

    public void setRooms(Room[] rooms) {
        this.rooms = rooms;
    }

    public BasicDeviceModule[] getModules() {
        return modules;
    }

    public void setModules(BasicDeviceModule[] modules) {
        this.modules = modules;
    }

    public Schedule[] getSchedules() {
        return schedules;
    }

    public void setSchedules(Schedule[] schedules) {
        this.schedules = schedules;
    }

    public BasicDeviceModule getModuleByID(@Nullable String id) {
        BasicDeviceModule result = null;
        if (modules != null) {
            for (BasicDeviceModule module : modules) {
                String moduleID = module.getID();
                if (moduleID == null) {
                    continue; // skip null IDs
                }
                if (moduleID.equals(id)) {
                    result = module;
                    break;
                }
            }
        }

        return result;
    }

    public Room getRoomByID(@Nullable String id) {
        Room result = null;
        if (modules != null) {
            for (Room room : rooms) {
                String roomID = room.getID();
                if (roomID == null) {
                    continue; // skip null IDs
                }
                if (roomID.equals(id)) {
                    result = room;
                    break;
                }
            }
        }

        return result;
    }

    public List<String> getDeviceIDs() {
        List<String> deviceIDs = new ArrayList<String>();
        for (BasicDeviceModule module : getModules()) {
            // add to result set
            deviceIDs.add(module.getID());
        }
        return deviceIDs;
    }

    public List<BasicDeviceModule> getDevices() {
        List<BasicDeviceModule> devices = new ArrayList<BasicDeviceModule>(Arrays.asList(getModules()));
        return devices;
    }
}
