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

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * @author Volker Daube - Initial contribution
 */
@NonNullByDefault
public abstract class RequestMsg extends Message {

    private transient Map<String, String> mRequestProperties = new LinkedHashMap<String, String>();

    public RequestMsg(Type messageType, String url) {
        super(messageType, url);
    }

    public Map<String, String> getRequestProperties() {
        return mRequestProperties;
    }

    public abstract String getUrl();

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append("{ request_properties: {");

        boolean first = true;
        for (Map.Entry<String, String> entry : mRequestProperties.entrySet()) {
            if (first) {
                first = false;
            } else {
                sb.append(",");
            }
            sb.append("\"");
            sb.append(entry.getKey());
            sb.append("\":");
            sb.append(entry.getValue());
        }
        sb.append("}}");

        return sb.toString();
    }
}
