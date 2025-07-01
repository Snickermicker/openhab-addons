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

/**
 * @author Volker Daube - Initial contribution
 */
public class TimetableSun {
    private int zone_id;
    private int day;

    public int getZoneID() {
        return zone_id;
    }

    public void setZoneID(int zoneID) {
        this.zone_id = zoneID;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }
}
