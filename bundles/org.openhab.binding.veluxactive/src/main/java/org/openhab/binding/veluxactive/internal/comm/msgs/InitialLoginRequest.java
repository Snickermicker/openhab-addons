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

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * @author Volker Daube - Initial contribution
 */
@NonNullByDefault
public class InitialLoginRequest extends PostRequestMsg {

    private String grant_type = "password";
    private String client_id = "";
    private String client_secret = "";
    private String username = "";
    private String password = "";
    private String user_prefix = "velux";

    public InitialLoginRequest(String client_id, String client_secret, String username, String password) {
        super("https://app.velux-active.com/oauth2/token");
        this.client_id = client_id;
        this.client_secret = client_secret;
        this.username = username;
        this.password = password;

        Map<String, String> requestProperties = getRequestProperties();
        requestProperties.put("grant_type", this.grant_type);
        requestProperties.put("client_id", this.client_id);
        requestProperties.put("client_secret", this.client_secret);
        requestProperties.put("username", this.username);
        requestProperties.put("password", this.password);
        requestProperties.put("user_prefix", this.user_prefix);
    }
}
