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

/**
 * @author Volker Daube - Initial contribution
 */

public class SubscribeMsg extends WebsocketMessage {
    private String filter = "silent";
    private String access_token;
    private String app_type = "app_velux";
    private String action = "Subscribe";
    private String version;
    private String platform = "Android";

    public SubscribeMsg(String access_token, String version) {
        super("wss://app-ws.velux-active.com/ws/");
        this.access_token = access_token;
        this.version = version;
    }

    public String getFilter() {
        return filter;
    }

    public String getAccess_token() {
        return access_token;
    }

    public String getApp_type() {
        return app_type;
    }

    public String getAction() {
        return action;
    }

    public String getVersion() {
        return version;
    }

    public String getPlatform() {
        return platform;
    }
}
