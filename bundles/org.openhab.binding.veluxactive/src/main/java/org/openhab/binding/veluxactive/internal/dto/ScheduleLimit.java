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
public class ScheduleLimit {
    private int nb_zones;
    private int nb_timeslots;
    private int nb_items;
    private String type;

    public int getNbZones() {
        return nb_zones;
    }

    public void setNbZones(int nbZones) {
        this.nb_zones = nbZones;
    }

    public int getNbTimeslots() {
        return nb_timeslots;
    }

    public void setNbTimeslots(int nbTimeslots) {
        this.nb_timeslots = nbTimeslots;
    }

    public int getNbItems() {
        return nb_items;
    }

    public void setNbItems(int nbItems) {
        this.nb_items = nbItems;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
