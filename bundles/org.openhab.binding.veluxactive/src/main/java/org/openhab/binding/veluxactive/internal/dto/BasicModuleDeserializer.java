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
package org.openhab.binding.veluxactive.internal.dto;

import java.lang.reflect.Type;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * @author Volker Daube - Initial contribution
 */
public class BasicModuleDeserializer implements JsonDeserializer<BasicDeviceModule> {

    private static final Logger logger = LoggerFactory.getLogger(BasicModuleDeserializer.class);

    @Override
    public BasicDeviceModule deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
        BasicDeviceModule result = null;

        if (json.isJsonObject()) {
            JsonObject obj = json.getAsJsonObject();
            String type = obj.getAsJsonPrimitive("type").getAsString();

            Gson gson = new Gson();

            switch (type) {
                case "NXO":
                    result = gson.fromJson(json, NXOModule.class);
                    break;
                case "NXG":
                    result = gson.fromJson(json, NXGModule.class);
                    break;
                default:
                    logger.error("Unknown module type: {}", type);
            }
        }
        return result;
    }
}
