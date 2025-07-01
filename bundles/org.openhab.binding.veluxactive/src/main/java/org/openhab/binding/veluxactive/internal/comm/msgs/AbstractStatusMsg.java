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

import java.time.Instant;
import java.time.ZoneId;

/**
 * @author Volker Daube - Initial contribution
 */
public abstract class AbstractStatusMsg extends Message {

    private String status = "";
    private double time_exec = 0.0;
    private long time_server = 0L;

    public AbstractStatusMsg(Type messageType) {
        super(messageType);
    }

    public boolean isSuccess() {
        return "ok".equals(status);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getTimeExec() {
        return time_exec;
    }

    public void setTimeExec(double timeExec) {
        this.time_exec = timeExec;
    }

    public long getServerTimeSeconds() {
        return time_server;
    }

    public String getServerTime() {
        Instant instant = Instant.ofEpochSecond(time_server);
        return instant.atZone(ZoneId.systemDefault()).toString();
    }

    public void setServerTime(long serverTime) {
        this.time_server = serverTime;
    }
}
