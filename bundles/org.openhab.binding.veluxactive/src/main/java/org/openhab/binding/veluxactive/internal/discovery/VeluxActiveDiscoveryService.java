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
package org.openhab.binding.veluxactive.internal.discovery;

import static org.openhab.binding.veluxactive.internal.VeluxActiveBindingConstants.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.veluxactive.internal.dto.BasicDeviceModule;
import org.openhab.binding.veluxactive.internal.handler.VeluxActiveAccountBridgeHandler;
import org.openhab.core.config.discovery.AbstractThingHandlerDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link VeluxActiveDiscoveryService} is responsible for discovering the VeluxActive
 * thermostats that are associated with the VeluxActive Account, as well as the sensors
 * are associated with the VeluxActive thermostats.
 *
 * @author VOlker Daube - Initial contribution
 */
@Component(scope = ServiceScope.PROTOTYPE, service = VeluxActiveDiscoveryService.class)
@NonNullByDefault
public class VeluxActiveDiscoveryService extends AbstractThingHandlerDiscoveryService<VeluxActiveAccountBridgeHandler> {

    private final Logger logger = LoggerFactory.getLogger(VeluxActiveDiscoveryService.class);

    private @Nullable Future<?> discoveryJob;

    public VeluxActiveDiscoveryService() {
        super(VeluxActiveAccountBridgeHandler.class, SUPPORTED_DEVICE_THING_TYPES_UIDS, 8, true);
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return SUPPORTED_DEVICE_THING_TYPES_UIDS;
    }

    @Override
    protected void startBackgroundDiscovery() {
        logger.debug("VeluxActiveDiscovery: Starting background discovery job");
        Future<?> localDiscoveryJob = discoveryJob;
        if (localDiscoveryJob == null || localDiscoveryJob.isCancelled()) {
            discoveryJob = scheduler.scheduleWithFixedDelay(this::backgroundDiscover, DISCOVERY_INITIAL_DELAY_SECONDS,
                    DISCOVERY_INTERVAL_SECONDS, TimeUnit.SECONDS);
        }
    }

    @Override
    protected void stopBackgroundDiscovery() {
        logger.debug("VeluxActiveDiscovery: Stopping background discovery job");
        Future<?> localDiscoveryJob = discoveryJob;
        if (localDiscoveryJob != null) {
            localDiscoveryJob.cancel(true);
            discoveryJob = null;
        }
    }

    @Override
    public void startScan() {
        logger.debug("VeluxActiveDiscovery: Starting discovery scan");
        discover();
    }

    private void backgroundDiscover() {
        if (!thingHandler.isBackgroundDiscoveryEnabled()) {
            logger.debug("VeluxActiveDiscovery: background discovery is disabled, skipping discovery");
            return;
        }
        discover();
    }

    private void discover() {
        if (thingHandler.getThing().getStatus() != ThingStatus.ONLINE) {
            logger.debug("VeluxActiveDiscovery: Skipping discovery because Account Bridge thing is not ONLINE");
            return;
        }
        logger.debug("VeluxActiveDiscovery: Discovering VeluxActive devices");
        discoverDevices();
    }

    private synchronized void discoverDevices() {
        logger.debug("VeluxActiveDiscovery: Discovering devices");
        for (BasicDeviceModule device : thingHandler.getRegisteredDevices()) {
            String name = device.getName();
            String identifier = device.getID();
            if (identifier != null && name != null) {
                ThingUID thingUID = new ThingUID(UID_DEVICE_BRIDGE, thingHandler.getThing().getUID(), identifier);
                thingDiscovered(createDeviceDiscoveryResult(thingUID, identifier, name));
                logger.debug("VeluxActiveDiscovery: Device '{}' and name '{}' added with UID '{}'", identifier, name,
                        thingUID);
            }
        }
    }

    private DiscoveryResult createDeviceDiscoveryResult(ThingUID deviceUID, String identifier, String name) {
        VeluxActiveAccountBridgeHandler bridgeHandler = thingHandler;
        Map<String, Object> properties = new HashMap<>();
        properties.put(CONFIG_DEVICE_ID, identifier);
        return DiscoveryResultBuilder.create(deviceUID).withProperties(properties)
                .withRepresentationProperty(CONFIG_DEVICE_ID).withBridge(bridgeHandler.getThing().getUID())
                .withLabel(String.format("VeluxActive device %s", name)).build();
    }
}
