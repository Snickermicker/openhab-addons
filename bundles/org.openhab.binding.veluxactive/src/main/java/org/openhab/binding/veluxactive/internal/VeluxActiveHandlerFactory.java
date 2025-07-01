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
package org.openhab.binding.veluxactive.internal;

import static org.openhab.binding.veluxactive.internal.VeluxActiveBindingConstants.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.veluxactive.internal.handler.VeluxActiveAccountBridgeHandler;
import org.openhab.binding.veluxactive.internal.handler.VeluxActiveDeviceBridgeHandler;
import org.openhab.core.i18n.TimeZoneProvider;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.storage.StorageService;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.openhab.core.thing.type.ChannelTypeRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link VeluxActiveHandlerFactory} is responsible for creating thing handlers
 * for the account bridge, thermostat bridge, and sensor thing.
 *
 * @author Volker Daube - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.veluxactive", service = ThingHandlerFactory.class)
public class VeluxActiveHandlerFactory extends BaseThingHandlerFactory {

    private final Logger logger = LoggerFactory.getLogger(VeluxActiveHandlerFactory.class);
    private final TimeZoneProvider timeZoneProvider;
    private final ChannelTypeRegistry channelTypeRegistry;
    private final HttpClient httpClient;
    private final StorageService storageService;

    @Activate
    public VeluxActiveHandlerFactory(@Reference TimeZoneProvider timeZoneProvider,
            @Reference ChannelTypeRegistry channelTypeRegistry, @Reference HttpClientFactory httpClientFactory,
            @Reference StorageService storageService) {
        this.storageService = storageService;
        this.timeZoneProvider = timeZoneProvider;
        this.channelTypeRegistry = channelTypeRegistry;
        this.httpClient = httpClientFactory.getCommonHttpClient();
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();
        logger.debug("Creating handler for thing '{}'", thing.getUID());

        if (SUPPORTED_ACCOUNT_BRIDGE_THING_TYPES_UIDS.contains(thingTypeUID)) {
            logger.debug("Creating handler for account bridge '{}'", thing.getUID());
            return new VeluxActiveAccountBridgeHandler((Bridge) thing, httpClient, storageService);
        }
        // TODO: Re-enable when gateway bridge is implemented
        // if (SUPPORTED_GATEWAY_BRIDGE_THING_TYPES_UIDS.contains(thingTypeUID)) {
        // return new VeluxActiveGatewayBridgeHandler((Bridge) thing, httpClient, storageService);
        // }
        if (SUPPORTED_DEVICES_BRIDGE_THING_TYPES_UIDS.contains(thingTypeUID)) {
            logger.debug("Creating handler for device bridge '{}'", thing.getUID());
            return new VeluxActiveDeviceBridgeHandler((Bridge) thing, timeZoneProvider, channelTypeRegistry);
        }
        return null;
    }
}
