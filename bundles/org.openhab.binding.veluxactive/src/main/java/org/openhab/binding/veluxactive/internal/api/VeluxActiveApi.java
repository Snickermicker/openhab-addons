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

package org.openhab.binding.veluxactive.internal.api;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.veluxactive.internal.access.Access;
import org.openhab.binding.veluxactive.internal.comm.Communication;
import org.openhab.binding.veluxactive.internal.comm.Communication.GetPostResult;
import org.openhab.binding.veluxactive.internal.comm.msgs.HomesDataRequest;
import org.openhab.binding.veluxactive.internal.comm.msgs.HomesDataResponse;
import org.openhab.binding.veluxactive.internal.comm.msgs.MsgFactory;
import org.openhab.binding.veluxactive.internal.comm.msgs.SubscribeMsg;
import org.openhab.binding.veluxactive.internal.comm.websocket.WebSocketClientHandler;
import org.openhab.binding.veluxactive.internal.config.Config;
import org.openhab.binding.veluxactive.internal.dto.BasicDeviceModule;
import org.openhab.binding.veluxactive.internal.handler.VeluxActiveAccountBridgeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * The{@link VeluxActiveApi} is responsible for managing all communication with
 * the VeluxActive API service.
 *
 * @author Volker Daube - Initial contribution
 */
@NonNullByDefault
public class VeluxActiveApi {

    private static final Gson GSON = new GsonBuilder()
            // .registerTypeAdapter(Instant.class, new InstantDeserializer());
            // .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeDeserializer())
            // .registerTypeAdapter(RevisionDTO.class, new RevisionDTODeserializer())
            // .registerTypeAdapter(RunningDTO.class, new RunningDTODeserializer())
            .create();
    private static final Config mConfig = Config.getInstance();
    private Access mAccess = Access.getInstance();

    //
    // private static final String VELUXACTIVE_THERMOSTAT_URL = VELUXACTIVE_BASE_URL + "1/thermostat";
    // private static final String VELUXACTIVE_THERMOSTAT_SUMMARY_URL = VELUXACTIVE_BASE_URL + "1/thermostatSummary";
    // private static final String VELUXACTIVE_THERMOSTAT_UPDATE_URL = VELUXACTIVE_THERMOSTAT_URL + "?format=json";
    //
    // // These errors from the API will require an VeluxActive authorization
    // private static final int VELUXACTIVE_TOKEN_EXPIRED = 14;
    // private static final int VELUXACTIVE_DEAUTHORIZED_TOKEN = 16;
    // private static final int TOKEN_EXPIRES_IN_BUFFER_SECONDS = 120;
    // private static final boolean FORCE_TOKEN_REFRESH = true;
    // private static final boolean DONT_FORCE_TOKEN_REFRESH = false;
    //
    // public static final Properties HTTP_HEADERS;
    // static {
    // HTTP_HEADERS = new Properties();
    // HTTP_HEADERS.put("Content-Type", "application/json;charset=UTF-8");
    // HTTP_HEADERS.put("User-Agent", "openhab-valuxactive-api/2.0");
    // }
    //
    public static Gson getGson() {
        return GSON;
    }

    private final Logger logger = LoggerFactory.getLogger(VeluxActiveApi.class);
    private final VeluxActiveAccountBridgeHandler mBridgeHandler;

    //
    // private final String veluxUsername;
    // private final String veluxPassword;
    // private final String clientID;
    // private final String clientSecret;
    // private int apiTimeout;
    // private final OAuthFactory oAuthFactory;
    // private final HttpClient httpClient;
    //
    // private @NonNullByDefault({}) OAuthClientService oAuthClientService;
    // private @NonNullByDefault({}) VeluxActiveAuth veluxActiveAuth;
    //
    // private @Nullable AccessTokenResponse accessTokenResponse;
    //
    // Reference to the WebSocket client
    private WebSocketClientHandler mWebSocketClientHandler;
    private int mRetryCount;
    private long mLastReconnectAttemptMillis = 0L;
    private HttpClient mHttpClient;

    public VeluxActiveApi(final VeluxActiveAccountBridgeHandler bridgeHandler, final int apiTimeout,
            HttpClient httpClient) {
        this.mBridgeHandler = bridgeHandler;
        mHttpClient = httpClient;
        mWebSocketClientHandler = new WebSocketClientHandler(mHttpClient);
    }

    /**
     * Requests homes data from the server and initializes the process image.
     *
     * @return true if successful
     */
    public HomesDataResponse getHomesData() {
        HomesDataResponse homesDataResponse = new HomesDataResponse();

        logger.debug("Sending request for homes data");
        String accessToken = Access.getInstance().getAccessToken();
        logger.debug("Using access token: {}", accessToken);
        String appVersion = mConfig.getAppVersion();
        if ((accessToken == null) || (appVersion == null)) {
            logger.error("Access token and/or appVersion is null, cannot proceed with homes data request.");
        } else {
            HomesDataRequest dataRequest = new HomesDataRequest(accessToken, appVersion);

            GetPostResult response = Communication.getInstance().sendRequest(dataRequest, true);
            if (response.isSuccess()) {
                logger.debug("Homes data request was successful.");
                HomesDataResponse responseDataMsg = MsgFactory.getInstance().createMsg(response.getData(),
                        HomesDataResponse.class);
                if (responseDataMsg != null) {
                    homesDataResponse = responseDataMsg;
                    logger.debug("Homes data response: {}", homesDataResponse);
                } else {
                    logger.error("Failed to parse homes data response.");
                }

            } else {
                logger.error("Homes data request failed.");
            }
        }
        return homesDataResponse;
    }

    public boolean login() {
        boolean success = false;
        String accessToken = Access.getInstance().getAccessToken();
        if (accessToken == null) {
            logger.error("Access token is null, cannot proceed with login.");
            success = false;
        } else {
            logger.debug("Using access token: {}", accessToken);
            success = true;
        }
        return success;
    }

    public boolean startReceivingWebSocketMessages() {
        return subscribeToWebSocket();
    }

    public void stopWebSocket() {
        mWebSocketClientHandler.disconnect();
    }

    /**
     * Subscribes to the WebSocket for receiving live updates. Registers message
     * handlers and connects to the server.
     *
     * @return the WebSocketClient instance if successful, null otherwise
     */
    private boolean subscribeToWebSocket() {
        boolean scuccess = false;

        SubscribeMsg subscribeMsg = new SubscribeMsg(mAccess.getAccessToken(), mConfig.getAppVersion());

        mLastReconnectAttemptMillis = System.currentTimeMillis();

        URI uri = URI.create(subscribeMsg.getBaseUrl());
        logger.debug("Connecting to websocket");
        if (mWebSocketClientHandler.connect(uri)) {
            logger.debug("Connected to websocket.");
            scuccess = true;
        } else {
            logger.error("Failed to connect to websocket.");
            scuccess = false;
        }

        return scuccess;
    }

    public List<BasicDeviceModule> queryRegistereDevices() {
        return performDeviceQuery(null);
    }

    public List<BasicDeviceModule> performDeviceQuery(final @Nullable Set<String> deviceIds) {
        logger.debug("API: Perform query on device: '{}'", deviceIds);
        List<BasicDeviceModule> devices = new ArrayList<BasicDeviceModule>();
        HomesDataResponse response = getHomesData(); // Ensure homes data is up-to-date
        if (response.isSuccess()) {
            List<BasicDeviceModule> allDevices = response.getDevices();
            if (deviceIds == null) {
                // If no specific device IDs are provided, return all devices
                devices.addAll(allDevices);
                logger.debug("API: Returning all devices");
            } else {
                // Filter devices based on provided IDs
                for (String deviceID : deviceIds) {
                    BasicDeviceModule device = response.getDeviceById(deviceID);
                    if (device != null) {
                        devices.add(device);
                        logger.debug("API: Found device: {}", device.getName());
                    } else {
                        logger.debug("API: Skipping device: {} (not in query set)", deviceID);
                    }
                }
            }
        }
        return devices;
    }
}
