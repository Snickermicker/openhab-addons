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
package org.openhab.binding.veluxactive.internal.handler;

import static org.openhab.binding.veluxactive.internal.VeluxActiveBindingConstants.CONFIG_DEVICE_ID;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpProxy;
import org.openhab.binding.veluxactive.internal.api.VeluxActiveApi;
import org.openhab.binding.veluxactive.internal.config.Config;
import org.openhab.binding.veluxactive.internal.config.VeluxActiveAccountConfiguration;
import org.openhab.binding.veluxactive.internal.discovery.VeluxActiveDiscoveryService;
import org.openhab.binding.veluxactive.internal.dto.BasicDeviceModule;
import org.openhab.binding.veluxactive.internal.dto.thermostat.ThermostatUpdateRequestDTO;
import org.openhab.binding.veluxactive.internal.function.FunctionRequest;
import org.openhab.core.storage.StorageService;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link VeluxActiveAccountBridgeHandler} is responsible for managing
 * communication with the VeluxActive API.
 *
 * @author Volker Daube - Initial contribution
 */
@NonNullByDefault
public class VeluxActiveAccountBridgeHandler extends BaseBridgeHandler {

    private static final int REFRESH_INITIAL_DELAY_SECONDS = 3;
    // private static final int REFRESH_INTERVAL_SECONDS = 1;
    // private static final int DEFAULT_REFRESH_INTERVAL_NORMAL_SECONDS = 20;
    // private static final int DEFAULT_REFRESH_INTERVAL_QUICK_SECONDS = 5;
    // private static final int DEFAULT_API_TIMEOUT_SECONDS = 20;

    private final Logger logger = LoggerFactory.getLogger(VeluxActiveAccountBridgeHandler.class);

    private final HttpClient httpClient;
    private final StorageService storageService;

    private @NonNullByDefault({}) VeluxActiveApi mApi;
    // private int refreshIntervalNormal;
    private int refreshIntervalQuick;
    private int apiTimeout;
    private boolean discoveryEnabled = true;

    private final Map<String, VeluxActiveDeviceBridgeHandler> deviceHandlers = new ConcurrentHashMap<>();
    private final Set<String> deviceIds = new CopyOnWriteArraySet<>();

    private @Nullable Future<?> refreshJob;
    private final AtomicInteger refreshDevicesCounter = new AtomicInteger(REFRESH_INITIAL_DELAY_SECONDS);

    // private @Nullable SummaryResponseDTO previousSummary;
    private Config mConfig = Config.getInstance();

    public VeluxActiveAccountBridgeHandler(final Bridge bridge, HttpClient httpClient, StorageService storageService) {
        super(bridge);
        this.httpClient = httpClient;
        this.storageService = storageService;
    }

    @Override
    public void initialize() {
        logger.debug("AccountBridge: Initializing");

        VeluxActiveAccountConfiguration veluxConfig = getConfigAs(VeluxActiveAccountConfiguration.class);
        mConfig.setAndLoadStorageService(storageService, veluxConfig);

        mApi = new VeluxActiveApi(this, apiTimeout, httpClient);
        // Configure proxy if enabled in config
        if (mConfig.useProxy()) {
            logger.debug("Using proxy to connections host: {}, port: {}", mConfig.getProxyIP(), mConfig.getProxyPort());
            httpClient.getProxyConfiguration().getProxies()
                    .add(new HttpProxy(mConfig.getProxyIP(), mConfig.getProxyPort()));
        }

        mApi.startReceivingWebSocketMessages();

        scheduleRefreshJob();
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_PENDING, "Checking authorization");
    }

    @Override
    public void dispose() {
        cancelRefreshJob();
        // api.closeOAuthClientService();
        mApi.stopWebSocket();
        logger.debug("AccountBridge: Disposing");
    }

    @Override
    public void handleRemoval() {
        // TODO: Implement cleanup logic if necessary
        // oAuthFactory.deleteServiceAndAccessToken(thing.getUID().getAsString());
        super.handleRemoval();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Set.of(VeluxActiveDiscoveryService.class);
    }

    @Override
    public void childHandlerInitialized(ThingHandler deviceHandler, Thing deviceThing) {
        String devicetId = (String) deviceThing.getConfiguration().get(CONFIG_DEVICE_ID);
        deviceHandlers.put(devicetId, (VeluxActiveDeviceBridgeHandler) deviceHandler);
        deviceIds.add(devicetId);
        scheduleQuickPoll();
        logger.debug("AccountBridge: Adding device handler for {} with id {}", deviceThing.getUID(), devicetId);
    }

    @Override
    public void childHandlerDisposed(ThingHandler deviceHandler, Thing deviceThing) {
        String deviceId = (String) deviceThing.getConfiguration().get(CONFIG_DEVICE_ID);
        deviceHandlers.remove(deviceId);
        deviceIds.remove(deviceId);
        logger.debug("AccountBridge: Removing device handler for {} with id {}", deviceThing.getUID(), deviceId);
    }

    public boolean isBackgroundDiscoveryEnabled() {
        return discoveryEnabled;
    }

    public void updateBridgeStatus(ThingStatus status) {
        updateStatus(status);
    }

    public void updateBridgeStatus(ThingStatus status, ThingStatusDetail statusDetail, String statusMessage) {
        updateStatus(status, statusDetail, statusMessage);
    }

    public boolean performThermostatFunction(FunctionRequest request) {
        // TODO: Implement device function handling
        boolean success = false;
        // boolean success = api.performThermostatFunction(request);
        // if (success) {
        // scheduleQuickPoll();
        // }
        return success;
    }

    public boolean performThermostatUpdate(ThermostatUpdateRequestDTO request) {
        // TODO: Implement thermostat function handling
        boolean success = false;

        // boolean success = api.performThermostatUpdate(request);
        // if (success) {
        // scheduleQuickPoll();
        // }
        return success;
    }

    public void markOnline() {
        updateStatus(ThingStatus.ONLINE);
    }

    public void markOffline() {
        updateStatus(ThingStatus.OFFLINE);
    }

    public List<BasicDeviceModule> getRegisteredDevices() {
        return mApi.queryRegistereDevices();
    }

    /*
     * The refresh job updates channels on the refresh interval set in the thing config.
     * The is also required since the Websocket client will be disconnected by the server if not performed at regular
     * intervalls.
     * Updates the homes status by requesting homes data and then the status for each
     * home.
     *
     * @return true if successful, false otherwise
     */
    private boolean updateHomesStatus() {
        boolean success = false;
        if (mApi.getHomesData().isSuccess()) {
            markOnline();
            success = true;
            // Request status for each home
            // TODO udate openhab status for each home
            // VeluxActiveDeviceBridgeHandler handler = deviceHandlers.get(thermostat.identifier);
            // if (handler != null) {
            // handler.updateChannels(thermostat);
            // }

            // if (refreshDevicesCounter.getAndDecrement() == 0) {
            // refreshDevicesCounter.set(refreshIntervalNormal);
            // SummaryResponseDTO summary = api.performThermostatSummaryQuery();
            // if (summary != null && summary.hasChanged(previousSummary) && !deviceIds.isEmpty()) {
            // List<ThermostatDTO> thermostats = api.performThermostatQuery(deviceIds);
            // if (thermostats != null) {
            // for (ThermostatDTO thermostat : thermostats) {
            // VeluxActiveThermostatBridgeHandler handler = deviceHandlers.get(thermostat.identifier);
            // if (handler != null) {
            // handler.updateChannels(thermostat);
            // }
            // }
            // }
            // }
            // previousSummary = summary;
            // }
        } else {
            markOffline();
            logger.error("Homes data request failed, cannot update home status.");
            success = false;
        }
        return success;
    }

    private void scheduleQuickPoll() {
        if (refreshDevicesCounter.get() > refreshIntervalQuick) {
            logger.debug("AccountBridge: Scheduling quick poll");
            refreshDevicesCounter.set(refreshIntervalQuick);
            forceFullNextPoll();
        }
    }

    private void scheduleRefreshJob() {
        logger.debug("AccountBridge: Scheduling refresh job");
        cancelRefreshJob();
        refreshJob = scheduler.scheduleWithFixedDelay(this::updateHomesStatus, REFRESH_INITIAL_DELAY_SECONDS,
                mConfig.getKeepAliveIntervallMinutes() * 60, TimeUnit.SECONDS);
    }

    private void cancelRefreshJob() {
        Future<?> localRefreshDevicesJob = refreshJob;
        if (localRefreshDevicesJob != null) {
            forceFullNextPoll();
            localRefreshDevicesJob.cancel(true);
            logger.debug("AccountBridge: Canceling refresh job");
        }
    }

    private void forceFullNextPoll() {
        // previousSummary = null;
    }
}
