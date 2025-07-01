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

public class RefreshTokenRequest extends PostRequestMsg {

    public String grant_type = "refresh_token";
    public String refresh_token;
    public String client_id;
    public String client_secret;

    public RefreshTokenRequest(String refresh_token, String client_id, String client_secret) {
        super("https://app.velux-active.com/oauth2/token");
        this.refresh_token = refresh_token;
        this.client_id = client_id;
        this.client_secret = client_secret;
    }

    @Override
    public String getUrlParameters() {
        return "grant_type=" + grant_type + "&refresh_token=" + refresh_token + "&client_id=" + client_id
                + "&client_secret=" + client_secret;
    }
}
