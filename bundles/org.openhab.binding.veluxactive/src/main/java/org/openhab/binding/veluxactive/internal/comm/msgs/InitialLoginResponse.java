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

import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;

/**
 * @author Volker Daube - Initial contribution
 */
public class InitialLoginResponse extends ResponseMsg {
    public String access_token;
    public String refresh_token;
    public String[] scope;
    public int expires_in;
    public int expire_in;

    public InitialLoginResponse() {
        super(Type.RESPONSE);
    }

    public static InitialLoginResponse parse(String jsonString) {
        Gson gson = new Gson();
        InitialLoginResponse response = gson.fromJson(jsonString, InitialLoginResponse.class);
        return response;
    }

    public static InitialLoginResponse parse(byte[] result) {
        String resultString = new String(result, StandardCharsets.UTF_8);
        return parse(resultString);
    }
}
