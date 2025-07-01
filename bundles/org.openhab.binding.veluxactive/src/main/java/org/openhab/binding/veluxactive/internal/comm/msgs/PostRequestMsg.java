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
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * @author Volker Daube - Initial contribution
 */
@NonNullByDefault
public abstract class PostRequestMsg extends RequestMsg {

    public PostRequestMsg(String url) {
        super(Type.POST_REQUEST, url);
    }

    public byte[] getPostData(boolean inJsonFormat) {
        byte[] result;
        if (inJsonFormat) {
            String resultString = toJson();
            result = resultString.getBytes(StandardCharsets.UTF_8);
        } else {
            result = getUrlParameters().getBytes(StandardCharsets.UTF_8);
        }
        return result;
    }

    @Override
    public @NonNull String getUrl() {
        return getBaseUrl() + "?" + getUrlParameters();
    }

    public String getUrlParameters() {
        StringBuilder stringBuilder = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : getRequestProperties().entrySet()) {
            if (first) {
                first = false;
            } else {
                stringBuilder.append("&");
            }
            stringBuilder.append(entry.getKey());
            stringBuilder.append("=");
            stringBuilder.append(entry.getValue());
        }
        return stringBuilder.toString();
    }
}
