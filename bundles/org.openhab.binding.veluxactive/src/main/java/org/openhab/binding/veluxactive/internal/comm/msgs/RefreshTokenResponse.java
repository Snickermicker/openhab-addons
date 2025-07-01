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
public class RefreshTokenResponse extends ResponseMsg {

    public String access_token;
    public String refresh_token;
    public String[] scope;
    public int expires_in;
    public int expire_in;

    public RefreshTokenResponse() {
        super(Type.RESPONSE);
    }

    enum Scopes {
        all_scopes,
        access_velux,
        read_velux,
        write_velux,
        unknown
    }
}
