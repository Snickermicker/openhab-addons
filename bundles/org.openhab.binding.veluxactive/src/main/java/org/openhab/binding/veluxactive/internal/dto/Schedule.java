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

import org.openhab.core.items.Metadata;

import com.google.gson.annotations.SerializedName;

/**
 * @author Volker Daube - Initial contribution
 */
public class Schedule {
    private Timetable[] timetable;
    private Zone[] zones;
    private String name;
    @SerializedName("default")
    private boolean isDefault;
    private TimetableSun[] timetable_sunrise;
    private TimetableSun[] timetable_sunset;
    private Metadata metadata;
    private String id;
    private String type;
    private boolean selected;

    public Timetable[] getTimetable() {
        return timetable;
    }

    public void setTimetable(Timetable[] timetable) {
        this.timetable = timetable;
    }

    public Zone[] getZones() {
        return zones;
    }

    public void setZones(Zone[] zones) {
        this.zones = zones;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public TimetableSun[] getTimetableSunrise() {
        return timetable_sunrise;
    }

    public void setTimetableSunrise(TimetableSun[] timetableSunrise) {
        this.timetable_sunrise = timetableSunrise;
    }

    public TimetableSun[] getTimetableSunset() {
        return timetable_sunset;
    }

    public void setTimetableSunset(TimetableSun[] timetableSunset) {
        this.timetable_sunset = timetableSunset;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public String getID() {
        return id;
    }

    public void setID(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
