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
package org.openhab.binding.veluxactive.internal.comm.msgs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.veluxactive.internal.dto.BasicDeviceModule;
import org.openhab.binding.veluxactive.internal.dto.Home;
import org.openhab.binding.veluxactive.internal.dto.User;

/**
 * @author Volker Daube - Initial contribution
 */
public class HomesDataResponse extends AbstractStatusMsg {

    private Body body;

    public HomesDataResponse() {
        super(Type.HOMES_DATA);
    }

    public Body getBody() {
        return body;
    }

    public void setBody(Body body) {
        this.body = body;
    }

    public class Body {
        public Home[] homes;
        public User user;

        public Home[] getHomes() {
            return homes;
        }

        public void setHomes(Home[] homes) {
            this.homes = homes;
        }

        public User getUser() {
            return user;
        }

        public void setUser(User user) {
            this.user = user;
        }

        public @Nullable Home getHomeByID(@Nullable String id) {
            Home result = null;
            if (homes != null) {
                for (Home home : homes) {
                    if (home.getID().equals(id)) {
                        result = home;
                        break;
                    }
                }
            }

            return result;
        }
    }

    public List<String> getDeviceIDs() {
        List<String> deviceIDs = new ArrayList<String>();
        if (body.homes != null && body.homes.length > 0) {
            for (Home home : body.homes) {
                // add to result set
                deviceIDs.addAll(home.getDeviceIDs());
            }
        }
        return deviceIDs;
    }

    public List<BasicDeviceModule> getDevices() {
        List<BasicDeviceModule> devices = new ArrayList<BasicDeviceModule>();
        if (body.homes != null && body.homes.length > 0) {
            for (Home home : body.homes) {
                // add to result set
                devices.addAll(home.getDevices());
            }
        }
        return devices;
    }

    public @Nullable BasicDeviceModule getDeviceById(String deviceID) {
        List<BasicDeviceModule> devices = getDevices();
        BasicDeviceModule result = null;
        for (BasicDeviceModule device : devices) {
            if (device.getID().equals(deviceID)) {
                result = device;
                break;
            }
        }
        return result;
    }
}
