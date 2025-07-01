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

import org.openhab.binding.veluxactive.internal.comm.msgs.DeviceType;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * @author Volker Daube - Initial contribution
 */
public class BasicDeviceModule extends ElementWithID {

    private String type;
    private String name;
    private long last_seen;

    public DeviceType getType() {
        DeviceType result = DeviceType.valueOf(type);
        return result;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLastSeen() {
        Instant instant = Instant.ofEpochSecond(last_seen);
        return instant.atZone(ZoneId.systemDefault()).toString();
    }

    public long getLastSeenSeconds() {
        return last_seen;
    }

    public void setLastSeen(long lastSeenSeconds) {
        this.last_seen = lastSeenSeconds;
    }

    public Object toJson() {
        GsonBuilder builder = new GsonBuilder();
        builder.serializeNulls();
        builder.setLenient();
        Gson gson = builder.create();
        return gson.toJson(this);
    }
}
