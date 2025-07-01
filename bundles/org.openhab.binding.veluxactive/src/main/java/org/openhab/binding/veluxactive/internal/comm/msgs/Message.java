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

import org.eclipse.jdt.annotation.NonNullByDefault;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * @author Volker Daube - Initial contribution
 */
@NonNullByDefault
public abstract class Message {
    private transient String mBaseUrl;
    private transient Type mMessageType;

    public Message(Type messageType) {
        mMessageType = messageType;
        mBaseUrl = "";
    }

    public Message(Type messageType, String baseUrl) {
        mMessageType = messageType;
        mBaseUrl = baseUrl;
    }

    public String toJson() {
        GsonBuilder builder = new GsonBuilder();
        builder.serializeNulls();
        builder.setLenient();
        Gson gson = builder.create();
        return gson.toJson(this);
    }

    public void setBaseUrl(String baseUrl) {
        mBaseUrl = baseUrl;
    }

    public String getBaseUrl() {
        return mBaseUrl;
    }

    @Override
    public String toString() {
        GsonBuilder builder = new GsonBuilder();
        builder.serializeNulls();
        builder.setLenient();
        Gson gson = builder.create();
        return this.getClass().getName() + " URL: " + getBaseUrl() + ": " + gson.toJson(this);
    }

    public Type getMsgType() {
        return mMessageType;
    }

    public enum Type {
        POST_REQUEST,
        GET_REQUEST,
        RESPONSE,
        WS_REQUEST,
        BLIND_POSITION_INDICATION,
        WINDOW_POSITION_INDICATION,
        HOMES_DATA,
        STATUS,
        USER_RESPONSE,
        HOME_STATUS,
        MODULE_UPDATE
    };
}
