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
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.veluxactive.internal.dto.BasicDeviceModule;
import org.openhab.binding.veluxactive.internal.dto.BasicModuleDeserializer;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * @author Volker Daube - Initial contribution
 */
@NonNullByDefault
public class MsgFactory {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MsgFactory.class);
    private static MsgFactory mInstance = new MsgFactory();

    private @Nullable static Gson mGson = null;

    private MsgFactory() {
        // private constructor to prevent instantiation
    }

    public static MsgFactory getInstance() {
        return mInstance;
    }

    @SuppressWarnings("null")
    public @Nullable <T> T createMsg(String stringJson, Class<T> classOfT) {

        if (mGson == null) {
            initGson();
        }

        return mGson.fromJson(stringJson, classOfT);
    }

    public @Nullable <T> T createMsg(byte @Nullable [] bytesJson, Class<T> classOfT) {
        if (bytesJson == null) {
            return null;
        }
        String resultString = new String(bytesJson, StandardCharsets.UTF_8);
        return createMsg(resultString, classOfT);
    }

    public @Nullable Message createMsg(String jsonString) {
        Message resultMessage = null;
        Map<String, Object> jsonParams = getParams(jsonString);
        if (jsonParams.containsKey("type") && jsonParams.containsKey("push_type")) {
            if ("Websocket".equals(jsonParams.get("type")) && "embedded_json".equals(jsonParams.get("push_type"))) {
                // This is a message from the websocket connection
                resultMessage = createMsg(jsonString, ModuleUpdateMsg.class);
            } else {
                logger.warn("Received unknown websocket message: {}", jsonString);
                logger.debug("Keys: {}", jsonParams.keySet());
            }
        } else if (jsonParams.containsKey("body")) {
            resultMessage = createMsg(jsonString, HomesDataResponse.class);
        } else if ((jsonParams.keySet().size() == 3) && jsonParams.containsKey("status")
                && jsonParams.containsKey("time_exec") && jsonParams.containsKey("time_server")) {
            resultMessage = createMsg(jsonString, StatusMsg.class);
        } else {
            logger.warn("Received unknown message: {}", jsonString);
            logger.debug("Keys: {}", jsonParams.keySet());
        }

        return resultMessage;
    }

    private Map<String, Object> getParams(String jsonString) {
        Map<String, Object> jsonMap = new LinkedHashMap<String, Object>();
        if (mGson == null) {
            initGson();
        }
        @SuppressWarnings("null")
        JsonElement element = mGson.fromJson(jsonString, JsonElement.class);
        if (element.isJsonObject()) {

            JsonObject obj = element.getAsJsonObject();
            obj.entrySet().forEach(entry -> {
                String key = entry.getKey();
                JsonElement value = entry.getValue();
                if (value.isJsonPrimitive()) {
                    if (value.getAsJsonPrimitive().isString()) {
                        jsonMap.put(key, value.getAsString());
                    } else if (value.getAsJsonPrimitive().isNumber()) {
                        jsonMap.put(key, value.getAsNumber());
                    } else if (value.getAsJsonPrimitive().isBoolean()) {
                        jsonMap.put(key, value.getAsBoolean());
                    }
                } else if (value.isJsonObject()) {
                    jsonMap.put(key, getParams(value.toString()));
                } else if (value.isJsonArray()) {
                    jsonMap.put(key, value.getAsJsonArray());
                }
            });
        } else {
            logger.error("Received invalid JSON: {}", jsonString);
        }
        return jsonMap;
    }

    private void initGson() {
        if (mGson == null) {
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(BasicDeviceModule.class, new BasicModuleDeserializer());
            gsonBuilder.setLenient();
            mGson = gsonBuilder.create();
        }
    }
}
