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

import org.eclipse.jdt.annotation.NonNull;

/**
 * @author Volker Daube - Initial contribution
 */
public class HomesDataRequest extends PostRequestMsg {

    public String app_version;
    public String app_type = "app_velux";
    public String[] device_types = { DeviceType.NXG.toString() };
    public boolean sync_measurements = false;

    public HomesDataRequest(@NonNull String accessToken, @NonNull String appVersion) {
        super("https://app.velux-active.com/api/homesdata");
        this.app_version = appVersion;
        getRequestProperties().put("Authorization", "Bearer " + accessToken);
    }
}
