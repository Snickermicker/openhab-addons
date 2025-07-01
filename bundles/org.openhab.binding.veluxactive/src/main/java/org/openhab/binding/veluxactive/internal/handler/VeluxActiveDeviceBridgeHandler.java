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

import static org.openhab.binding.veluxactive.internal.VeluxActiveBindingConstants.*;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.veluxactive.internal.action.VeluxActiveActions;
import org.openhab.binding.veluxactive.internal.comm.msgs.HomesDataResponse;
import org.openhab.binding.veluxactive.internal.config.VeluxActivDeviceConfiguration;
import org.openhab.binding.veluxactive.internal.dto.BasicDeviceModule;
import org.openhab.binding.veluxactive.internal.dto.SelectionDTO;
import org.openhab.binding.veluxactive.internal.dto.thermostat.AlertDTO;
import org.openhab.binding.veluxactive.internal.dto.thermostat.ClimateDTO;
import org.openhab.binding.veluxactive.internal.dto.thermostat.HouseDetailsDTO;
import org.openhab.binding.veluxactive.internal.dto.thermostat.LocationDTO;
import org.openhab.binding.veluxactive.internal.dto.thermostat.ManagementDTO;
import org.openhab.binding.veluxactive.internal.dto.thermostat.ProgramDTO;
import org.openhab.binding.veluxactive.internal.dto.thermostat.RemoteSensorDTO;
import org.openhab.binding.veluxactive.internal.dto.thermostat.SettingsDTO;
import org.openhab.binding.veluxactive.internal.dto.thermostat.TechnicianDTO;
import org.openhab.binding.veluxactive.internal.dto.thermostat.VersionDTO;
import org.openhab.binding.veluxactive.internal.dto.thermostat.WeatherDTO;
import org.openhab.binding.veluxactive.internal.dto.thermostat.WeatherForecastDTO;
import org.openhab.binding.veluxactive.internal.function.AbstractFunction;
import org.openhab.core.i18n.TimeZoneProvider;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.ImperialUnits;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeRegistry;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.openhab.core.types.StateDescription;
import org.openhab.core.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link VeluxActiveDeviceBridgeHandler} is the handler for an VeluxActive Device.
 *
 * @author Mark Hilbush - Initial contribution
 */
@NonNullByDefault
public class VeluxActiveDeviceBridgeHandler extends BaseBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(VeluxActiveDeviceBridgeHandler.class);

    private TimeZoneProvider timeZoneProvider;
    private ChannelTypeRegistry channelTypeRegistry;

    private @NonNullByDefault({}) String deviceID;

    private final Map<String, VeluxActiveNXSThingHandler> sensorHandlers = new ConcurrentHashMap<>();

    private @Nullable BasicDeviceModule savedDevices;
    private List<String> validClimateRefs = new CopyOnWriteArrayList<>();
    private Map<String, State> stateCache = new ConcurrentHashMap<>();
    private Map<ChannelUID, Boolean> channelReadOnlyMap = new HashMap<>();
    private Map<Integer, String> symbolMap = new HashMap<>();
    private Map<Integer, String> skyMap = new HashMap<>();

    public VeluxActiveDeviceBridgeHandler(Bridge bridge, TimeZoneProvider timeZoneProvider,
            ChannelTypeRegistry channelTypeRegistry) {
        super(bridge);
        this.timeZoneProvider = timeZoneProvider;
        this.channelTypeRegistry = channelTypeRegistry;
    }

    @Override
    public void initialize() {
        // TODO WTF one device?
        VeluxActivDeviceConfiguration cfg = getConfigAs(VeluxActivDeviceConfiguration.class);
        logger.debug("DeviceBridge: config= '{}'", cfg);
        deviceID = cfg.deviceId;
        logger.debug("DeviceBridge: Initializing Device '{}'", deviceID);
        initializeReadOnlyChannels();
        clearSavedState();
        logger.debug("DeviceBridge: bridge '{}' status: {}", getBridge(), VeluxActiveUtils.isBridgeOnline(getBridge()));
        updateStatus(VeluxActiveUtils.isBridgeOnline(getBridge()) ? ThingStatus.ONLINE : ThingStatus.OFFLINE);
    }

    @Override
    public void dispose() {
        logger.debug("DeviceBridge: Disposing Device '{}'", deviceID);
    }

    @Override
    public void childHandlerInitialized(ThingHandler sensorHandler, Thing sensorThing) {
        String sensorId = (String) sensorThing.getConfiguration().get(CONFIG_SENSOR_ID);
        sensorHandlers.put(sensorId, (VeluxActiveNXSThingHandler) sensorHandler);
        logger.debug("DeviceBridge: Saving sensor handler for {} with id {}", sensorThing.getUID(), sensorId);
    }

    @Override
    public void childHandlerDisposed(ThingHandler sensorHandler, Thing sensorThing) {
        String sensorId = (String) sensorThing.getConfiguration().get(CONFIG_SENSOR_ID);
        sensorHandlers.remove(sensorId);
        logger.debug("DeviceBridge: Removing sensor handler for {} with id {}", sensorThing.getUID(), sensorId);
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        if (bridgeStatusInfo.getStatus() == ThingStatus.ONLINE) {
            updateStatus(ThingStatus.ONLINE);
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
        }
        logger.debug("New DeviceBridge status: bridge '{}' status: {}", getBridge(), bridgeStatusInfo.getStatus());
    }

    @SuppressWarnings("null")
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            State state = stateCache.get(channelUID.getId());
            if (state != null) {
                updateState(channelUID.getId(), state);
            }
            return;
        }
        if (isChannelReadOnly(channelUID)) {
            logger.debug("Can't apply command '{}' to '{}' because channel is readonly", command, channelUID.getId());
            return;
        }
        scheduler.execute(() -> {
            handleDeviceCommand(channelUID, command);
        });
    }

    /**
     * Called by the AccountBridgeHandler to create a Selection that
     * includes only the VeluxActive objects for which there's at least one
     * item linked to one of that object's channels.
     *
     * @return Selection
     */
    public SelectionDTO getSelection() {
        final SelectionDTO selection = new SelectionDTO();
        for (String group : CHANNEL_GROUPS) {
            for (Channel channel : thing.getChannelsOfGroup(group)) {
                if (isLinked(channel.getUID())) {
                    try {
                        Field field = selection.getClass()
                                .getField("include" + StringUtils.capitalizeByWhitespace(group));
                        logger.trace("DeviceBridge: Device thing '{}' including object '{}' in selection",
                                thing.getUID(), field.getName());
                        field.set(selection, Boolean.TRUE);
                        break;
                    } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException
                            | SecurityException e) {
                        logger.debug("DeviceBridge: Exception setting selection for group '{}'", group, e);
                    }
                }
            }
        }
        return selection;
    }

    public String getDeviceId() {
        return deviceID;
    }

    /*
     * Called by VeluxActiveActions to perform a Device function
     */
    public boolean actionPerformFunction(AbstractFunction function) {
        logger.debug("DeviceBridge: Perform function '{}' on Device {}", function.type, deviceID);
        // SelectionDTO selection = new SelectionDTO();
        // selection.setDevices(Set.of(deviceID));
        // FunctionRequest request = new FunctionRequest(selection);
        // request.functions = List.of(function);
        // VeluxActiveAccountBridgeHandler handler = getBridgeHandler();
        // if (handler != null) {
        // return handler.performDeviceFunction(request);
        // }
        return false;
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return List.of(VeluxActiveActions.class);
    }

    public void updateChannels(BasicDeviceModule device) {
        logger.debug("DeviceBridge: Updating channels for Device id {}", device.getID());
        savedDevices = device;
        // updateAlert(device.alerts);
        // updateHouseDetails(device.houseDetails);
        updateInfo(device);
        updateEquipmentStatus(device);
        // updateLocation(device.location);
        // updateManagement(device.management);
        // updateProgram(device.program);
        // updateEvent(device.events);
        // updateRemoteSensors(device.remoteSensors);
        // updateRuntime(device.runtime);
        // updateSettings(device.settings);
        // updateTechnician(device.technician);
        // updateVersion(device.version);
        // updateWeather(device.weather);
        // savedSensors = device.remoteSensors;
    }

    private void handleDeviceCommand(ChannelUID channelUID, Command command) {
        logger.debug("Got command '{}' for channel '{}' of thing '{}'", command, channelUID, getThing().getUID());
        String channelId = channelUID.getIdWithoutGroup();
        String groupId = channelUID.getGroupId();
        if (groupId == null) {
            logger.info("Can't handle command '{}' because channel's groupId is null", command);
            return;
        }
        BasicDeviceModule Device = new BasicDeviceModule();
        Field field;
        try {
            switch (groupId) {
                case CHGRP_INFO:
                    field = Device.getClass().getField(channelId);
                    setField(field, Device, command);
                    break;
                case CHGRP_SETTINGS:
                    SettingsDTO settings = new SettingsDTO();
                    field = settings.getClass().getField(channelId);
                    setField(field, settings, command);
                    // Device.settings = settings;
                    break;
                case CHGRP_LOCATION:
                    LocationDTO location = new LocationDTO();
                    field = location.getClass().getField(channelId);
                    setField(field, location, command);
                    // Device.location = location;
                    break;
                case CHGRP_HOUSE_DETAILS:
                    HouseDetailsDTO houseDetails = new HouseDetailsDTO();
                    field = houseDetails.getClass().getField(channelId);
                    setField(field, houseDetails, command);
                    // Device.houseDetails = houseDetails;
                    break;
                default:
                    // All other groups contain only read-only fields
                    return;
            }
            performDeviceUpdate(Device);
        } catch (NoSuchFieldException | SecurityException e) {
            logger.info("Unable to get field for '{}.{}'", groupId, channelId);
        }
    }

    private void setField(Field field, Object object, Command command) {
        logger.debug("Setting field '{}.{}' to value '{}'", object.getClass().getSimpleName().toLowerCase(),
                field.getName(), command);
        Class<?> fieldClass = field.getType();
        try {
            boolean success = false;
            if (String.class.isAssignableFrom(fieldClass)) {
                if (command instanceof StringType) {
                    logger.debug("Set field of type String to value of StringType");
                    field.set(object, command.toString());
                    success = true;
                }
            } else if (Integer.class.isAssignableFrom(fieldClass)) {
                if (command instanceof DecimalType decimalCommand) {
                    logger.debug("Set field of type Integer to value of DecimalType");
                    field.set(object, Integer.valueOf(decimalCommand.intValue()));
                    success = true;
                } else if (command instanceof QuantityType<?> quantityCommand) {
                    Unit<?> unit = quantityCommand.getUnit();
                    logger.debug("Set field of type Integer to value of QuantityType with unit {}", unit);
                    if (unit.equals(ImperialUnits.FAHRENHEIT) || unit.equals(SIUnits.CELSIUS)) {
                        QuantityType<?> quantity = quantityCommand.toUnit(ImperialUnits.FAHRENHEIT);
                        if (quantity != null) {
                            field.set(object, quantity.intValue() * 10);
                            success = true;
                        }
                    }
                }
            } else if (Boolean.class.isAssignableFrom(fieldClass)) {
                if (command instanceof OnOffType) {
                    logger.debug("Set field of type Boolean to value of OnOffType");
                    field.set(object, command == OnOffType.ON);
                    success = true;
                }
            }
            if (!success) {
                logger.info("Don't know how to convert command of type '{}' to {}.{}",
                        command.getClass().getSimpleName(), object.getClass().getSimpleName(), field.getName());
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            logger.info("Unable to set field '{}.{}' to value '{}'", object.getClass().getSimpleName(), field.getName(),
                    command, e);
        }
    }

    private void updateInfo(BasicDeviceModule Device) {
        final String grp = CHGRP_INFO + "#";
        // updateChannel(grp + CH_IDENTIFIER, VeluxActiveUtils.undefOrString(Device.identifier));
        // updateChannel(grp + CH_NAME, VeluxActiveUtils.undefOrString(Device.name));
        // updateChannel(grp + CH_Device_REV, VeluxActiveUtils.undefOrString(Device.DeviceRev));
        // updateChannel(grp + CH_IS_REGISTERED, VeluxActiveUtils.undefOrOnOff(Device.isRegistered));
        // updateChannel(grp + CH_MODEL_NUMBER, VeluxActiveUtils.undefOrString(Device.modelNumber));
        // updateChannel(grp + CH_BRAND, VeluxActiveUtils.undefOrString(Device.brand));
        // updateChannel(grp + CH_FEATURES, VeluxActiveUtils.undefOrString(Device.features));
        // updateChannel(grp + CH_LAST_MODIFIED, VeluxActiveUtils.undefOrDate(Device.lastModified, timeZoneProvider));
        // updateChannel(grp + CH_Device_TIME, VeluxActiveUtils.undefOrDate(Device.DeviceTime, timeZoneProvider));
    }

    private void updateEquipmentStatus(BasicDeviceModule Device) {
        final String grp = CHGRP_EQUIPMENT_STATUS + "#";
        // updateChannel(grp + CH_EQUIPMENT_STATUS, VeluxActiveUtils.undefOrString(Device.equipmentStatus));
    }

    // private void updateRuntime(@Nullable RuntimeDTO runtime) {
    // if (runtime == null) {
    // return;
    // }
    // final String grp = CHGRP_RUNTIME + "#";
    // updateChannel(grp + CH_RUNTIME_REV, VeluxActiveUtils.undefOrString(runtime.runtimeRev));
    // updateChannel(grp + CH_CONNECTED, VeluxActiveUtils.undefOrOnOff(runtime.connected));
    // updateChannel(grp + CH_FIRST_CONNECTED, VeluxActiveUtils.undefOrDate(runtime.firstConnected, timeZoneProvider));
    // updateChannel(grp + CH_CONNECT_DATE_TIME,
    // VeluxActiveUtils.undefOrDate(runtime.connectDateTime, timeZoneProvider));
    // updateChannel(grp + CH_DISCONNECT_DATE_TIME,
    // VeluxActiveUtils.undefOrDate(runtime.disconnectDateTime, timeZoneProvider));
    // updateChannel(grp + CH_RT_LAST_MODIFIED, VeluxActiveUtils.undefOrDate(runtime.lastModified, timeZoneProvider));
    // updateChannel(grp + CH_RT_LAST_STATUS_MODIFIED,
    // VeluxActiveUtils.undefOrDate(runtime.lastStatusModified, timeZoneProvider));
    // updateChannel(grp + CH_RUNTIME_DATE, VeluxActiveUtils.undefOrString(runtime.runtimeDate));
    // updateChannel(grp + CH_RUNTIME_INTERVAL, VeluxActiveUtils.undefOrDecimal(runtime.runtimeInterval));
    // if (runtime.actualTemperature > MIN_VALID_ACTUAL_TEMPERATURE) {
    // updateChannel(grp + CH_ACTUAL_TEMPERATURE, VeluxActiveUtils.undefOrTemperature(runtime.actualTemperature));
    // } else {
    // logger.debug("Skipping update of actual temperature because temperature {} below min threshold of {}",
    // runtime.actualTemperature, MIN_VALID_ACTUAL_TEMPERATURE);
    // }
    // updateChannel(grp + CH_ACTUAL_HUMIDITY,
    // VeluxActiveUtils.undefOrQuantity(runtime.actualHumidity, Units.PERCENT));
    // updateChannel(grp + CH_RAW_TEMPERATURE, VeluxActiveUtils.undefOrTemperature(runtime.rawTemperature));
    // updateChannel(grp + CH_SHOW_ICON_MODE, VeluxActiveUtils.undefOrDecimal(runtime.showIconMode));
    // updateChannel(grp + CH_DESIRED_HEAT, VeluxActiveUtils.undefOrTemperature(runtime.desiredHeat));
    // updateChannel(grp + CH_DESIRED_COOL, VeluxActiveUtils.undefOrTemperature(runtime.desiredCool));
    // updateChannel(grp + CH_DESIRED_HUMIDITY,
    // VeluxActiveUtils.undefOrQuantity(runtime.desiredHumidity, Units.PERCENT));
    // updateChannel(grp + CH_DESIRED_DEHUMIDITY,
    // VeluxActiveUtils.undefOrQuantity(runtime.desiredDehumidity, Units.PERCENT));
    // updateChannel(grp + CH_DESIRED_FAN_MODE, VeluxActiveUtils.undefOrString(runtime.desiredFanMode));
    // if (runtime.desiredHeatRange != null && runtime.desiredHeatRange.size() == 2) {
    // updateChannel(grp + CH_DESIRED_HEAT_RANGE_LOW,
    // VeluxActiveUtils.undefOrTemperature(runtime.desiredHeatRange.get(0)));
    // updateChannel(grp + CH_DESIRED_HEAT_RANGE_HIGH,
    // VeluxActiveUtils.undefOrTemperature(runtime.desiredHeatRange.get(1)));
    // }
    // if (runtime.desiredCoolRange != null && runtime.desiredCoolRange.size() == 2) {
    // updateChannel(grp + CH_DESIRED_COOL_RANGE_LOW,
    // VeluxActiveUtils.undefOrTemperature(runtime.desiredCoolRange.get(0)));
    // updateChannel(grp + CH_DESIRED_COOL_RANGE_HIGH,
    // VeluxActiveUtils.undefOrTemperature(runtime.desiredCoolRange.get(1)));
    // }
    // updateChannel(grp + CH_ACTUAL_AQ_ACCURACY, VeluxActiveUtils.undefOrLong(runtime.actualAQAccuracy));
    // updateChannel(grp + CH_ACTUAL_AQ_SCORE, VeluxActiveUtils.undefOrLong(runtime.actualAQScore));
    // updateChannel(grp + CH_ACTUAL_CO2,
    // VeluxActiveUtils.undefOrQuantity(runtime.actualCO2, Units.PARTS_PER_MILLION));
    // updateChannel(grp + CH_ACTUAL_VOC,
    // VeluxActiveUtils.undefOrQuantity(runtime.actualVOC, Units.PARTS_PER_BILLION));
    // }

    private void updateSettings(@Nullable SettingsDTO settings) {
        if (settings == null) {
            return;
        }
        final String grp = CHGRP_SETTINGS + "#";
        updateChannel(grp + CH_HVAC_MODE, VeluxActiveUtils.undefOrString(settings.hvacMode));
        updateChannel(grp + CH_LAST_SERVICE_DATE, VeluxActiveUtils.undefOrString(settings.lastServiceDate));
        updateChannel(grp + CH_SERVICE_REMIND_ME, VeluxActiveUtils.undefOrOnOff(settings.serviceRemindMe));
        updateChannel(grp + CH_MONTHS_BETWEEN_SERVICE, VeluxActiveUtils.undefOrDecimal(settings.monthsBetweenService));
        updateChannel(grp + CH_REMIND_ME_DATE, VeluxActiveUtils.undefOrString(settings.remindMeDate));
        updateChannel(grp + CH_VENT, VeluxActiveUtils.undefOrString(settings.vent));
        updateChannel(grp + CH_VENTILATOR_MIN_ON_TIME, VeluxActiveUtils.undefOrDecimal(settings.ventilatorMinOnTime));
        updateChannel(grp + CH_SERVICE_REMIND_TECHNICIAN,
                VeluxActiveUtils.undefOrOnOff(settings.serviceRemindTechnician));
        updateChannel(grp + CH_EI_LOCATION, VeluxActiveUtils.undefOrString(settings.eiLocation));
        updateChannel(grp + CH_COLD_TEMP_ALERT, VeluxActiveUtils.undefOrTemperature(settings.coldTempAlert));
        updateChannel(grp + CH_COLD_TEMP_ALERT_ENABLED, VeluxActiveUtils.undefOrOnOff(settings.coldTempAlertEnabled));
        updateChannel(grp + CH_HOT_TEMP_ALERT, VeluxActiveUtils.undefOrTemperature(settings.hotTempAlert));
        updateChannel(grp + CH_HOT_TEMP_ALERT_ENABLED, VeluxActiveUtils.undefOrOnOff(settings.hotTempAlertEnabled));
        updateChannel(grp + CH_COOL_STAGES, VeluxActiveUtils.undefOrDecimal(settings.coolStages));
        updateChannel(grp + CH_HEAT_STAGES, VeluxActiveUtils.undefOrDecimal(settings.heatStages));
        updateChannel(grp + CH_MAX_SET_BACK, VeluxActiveUtils.undefOrDecimal(settings.maxSetBack));
        updateChannel(grp + CH_MAX_SET_FORWARD, VeluxActiveUtils.undefOrDecimal(settings.maxSetForward));
        updateChannel(grp + CH_QUICK_SAVE_SET_BACK, VeluxActiveUtils.undefOrDecimal(settings.quickSaveSetBack));
        updateChannel(grp + CH_QUICK_SAVE_SET_FORWARD, VeluxActiveUtils.undefOrDecimal(settings.quickSaveSetForward));
        updateChannel(grp + CH_HAS_HEAT_PUMP, VeluxActiveUtils.undefOrOnOff(settings.hasHeatPump));
        updateChannel(grp + CH_HAS_FORCED_AIR, VeluxActiveUtils.undefOrOnOff(settings.hasForcedAir));
        updateChannel(grp + CH_HAS_BOILER, VeluxActiveUtils.undefOrOnOff(settings.hasBoiler));
        updateChannel(grp + CH_HAS_HUMIDIFIER, VeluxActiveUtils.undefOrOnOff(settings.hasHumidifier));
        updateChannel(grp + CH_HAS_ERV, VeluxActiveUtils.undefOrOnOff(settings.hasErv));
        updateChannel(grp + CH_HAS_HRV, VeluxActiveUtils.undefOrOnOff(settings.hasHrv));
        updateChannel(grp + CH_CONDENSATION_AVOID, VeluxActiveUtils.undefOrOnOff(settings.condensationAvoid));
        updateChannel(grp + CH_USE_CELSIUS, VeluxActiveUtils.undefOrOnOff(settings.useCelsius));
        updateChannel(grp + CH_USE_TIME_FORMAT_12, VeluxActiveUtils.undefOrOnOff(settings.useTimeFormat12));
        updateChannel(grp + CH_LOCALE, VeluxActiveUtils.undefOrString(settings.locale));
        updateChannel(grp + CH_HUMIDITY, VeluxActiveUtils.undefOrString(settings.humidity));
        updateChannel(grp + CH_HUMIDIFIER_MODE, VeluxActiveUtils.undefOrString(settings.humidifierMode));
        updateChannel(grp + CH_BACKLIGHT_ON_INTENSITY, VeluxActiveUtils.undefOrDecimal(settings.backlightOnIntensity));
        updateChannel(grp + CH_BACKLIGHT_SLEEP_INTENSITY,
                VeluxActiveUtils.undefOrDecimal(settings.backlightSleepIntensity));
        updateChannel(grp + CH_BACKLIGHT_OFF_TIME, VeluxActiveUtils.undefOrDecimal(settings.backlightOffTime));
        updateChannel(grp + CH_SOUND_TICK_VOLUME, VeluxActiveUtils.undefOrDecimal(settings.soundTickVolume));
        updateChannel(grp + CH_SOUND_ALERT_VOLUME, VeluxActiveUtils.undefOrDecimal(settings.soundAlertVolume));
        updateChannel(grp + CH_COMPRESSOR_PROTECTION_MIN_TIME,
                VeluxActiveUtils.undefOrDecimal(settings.compressorProtectionMinTime));
        updateChannel(grp + CH_COMPRESSOR_PROTECTION_MIN_TEMP,
                VeluxActiveUtils.undefOrTemperature(settings.compressorProtectionMinTemp));
        updateChannel(grp + CH_STAGE1_HEATING_DIFFERENTIAL_TEMP,
                VeluxActiveUtils.undefOrDecimal(settings.stage1HeatingDifferentialTemp));
        updateChannel(grp + CH_STAGE1_COOLING_DIFFERENTIAL_TEMP,
                VeluxActiveUtils.undefOrDecimal(settings.stage1CoolingDifferentialTemp));
        updateChannel(grp + CH_STAGE1_HEATING_DISSIPATION_TIME,
                VeluxActiveUtils.undefOrDecimal(settings.stage1HeatingDissipationTime));
        updateChannel(grp + CH_STAGE1_COOLING_DISSIPATION_TIME,
                VeluxActiveUtils.undefOrDecimal(settings.stage1CoolingDissipationTime));
        updateChannel(grp + CH_HEAT_PUMP_REVERSAL_ON_COOL,
                VeluxActiveUtils.undefOrOnOff(settings.heatPumpReversalOnCool));
        updateChannel(grp + CH_FAN_CONTROLLER_REQUIRED, VeluxActiveUtils.undefOrOnOff(settings.fanControlRequired));
        updateChannel(grp + CH_FAN_MIN_ON_TIME, VeluxActiveUtils.undefOrDecimal(settings.fanMinOnTime));
        updateChannel(grp + CH_HEAT_COOL_MIN_DELTA, VeluxActiveUtils.undefOrDecimal(settings.heatCoolMinDelta));
        updateChannel(grp + CH_TEMP_CORRECTION, VeluxActiveUtils.undefOrDecimal(settings.tempCorrection));
        updateChannel(grp + CH_HOLD_ACTION, VeluxActiveUtils.undefOrString(settings.holdAction));
        updateChannel(grp + CH_HEAT_PUMP_GROUND_WATER, VeluxActiveUtils.undefOrOnOff(settings.heatPumpGroundWater));
        updateChannel(grp + CH_HAS_ELECTRIC, VeluxActiveUtils.undefOrOnOff(settings.hasElectric));
        updateChannel(grp + CH_HAS_DEHUMIDIFIER, VeluxActiveUtils.undefOrOnOff(settings.hasDehumidifier));
        updateChannel(grp + CH_DEHUMIDIFIER_MODE, VeluxActiveUtils.undefOrString(settings.dehumidifierMode));
        updateChannel(grp + CH_DEHUMIDIFIER_LEVEL, VeluxActiveUtils.undefOrDecimal(settings.dehumidifierLevel));
        updateChannel(grp + CH_DEHUMIDIFY_WITH_AC, VeluxActiveUtils.undefOrOnOff(settings.dehumidifyWithAC));
        updateChannel(grp + CH_DEHUMIDIFY_OVERCOOL_OFFSET,
                VeluxActiveUtils.undefOrDecimal(settings.dehumidifyOvercoolOffset));
        updateChannel(grp + CH_AUTO_HEAT_COOL_FEATURE_ENABLED,
                VeluxActiveUtils.undefOrOnOff(settings.autoHeatCoolFeatureEnabled));
        updateChannel(grp + CH_WIFI_OFFLINE_ALERT, VeluxActiveUtils.undefOrOnOff(settings.wifiOfflineAlert));
        updateChannel(grp + CH_HEAT_MIN_TEMP, VeluxActiveUtils.undefOrTemperature(settings.heatMinTemp));
        updateChannel(grp + CH_HEAT_MAX_TEMP, VeluxActiveUtils.undefOrTemperature(settings.heatMaxTemp));
        updateChannel(grp + CH_COOL_MIN_TEMP, VeluxActiveUtils.undefOrTemperature(settings.coolMinTemp));
        updateChannel(grp + CH_COOL_MAX_TEMP, VeluxActiveUtils.undefOrTemperature(settings.coolMaxTemp));
        updateChannel(grp + CH_HEAT_RANGE_HIGH, VeluxActiveUtils.undefOrTemperature(settings.heatRangeHigh));
        updateChannel(grp + CH_HEAT_RANGE_LOW, VeluxActiveUtils.undefOrTemperature(settings.heatRangeLow));
        updateChannel(grp + CH_COOL_RANGE_HIGH, VeluxActiveUtils.undefOrTemperature(settings.coolRangeHigh));
        updateChannel(grp + CH_COOL_RANGE_LOW, VeluxActiveUtils.undefOrTemperature(settings.coolRangeLow));
        updateChannel(grp + CH_USER_ACCESS_CODE, VeluxActiveUtils.undefOrString(settings.userAccessCode));
        updateChannel(grp + CH_USER_ACCESS_SETTING, VeluxActiveUtils.undefOrDecimal(settings.userAccessSetting));
        updateChannel(grp + CH_AUX_RUNTIME_ALERT, VeluxActiveUtils.undefOrDecimal(settings.auxRuntimeAlert));
        updateChannel(grp + CH_AUX_OUTDOOR_TEMP_ALERT,
                VeluxActiveUtils.undefOrTemperature(settings.auxOutdoorTempAlert));
        updateChannel(grp + CH_AUX_MAX_OUTDOOR_TEMP, VeluxActiveUtils.undefOrTemperature(settings.auxMaxOutdoorTemp));
        updateChannel(grp + CH_AUX_RUNTIME_ALERT_NOTIFY, VeluxActiveUtils.undefOrOnOff(settings.auxRuntimeAlertNotify));
        updateChannel(grp + CH_AUX_OUTDOOR_TEMP_ALERT_NOTIFY,
                VeluxActiveUtils.undefOrOnOff(settings.auxOutdoorTempAlertNotify));
        updateChannel(grp + CH_AUX_RUNTIME_ALERT_NOTIFY_TECHNICIAN,
                VeluxActiveUtils.undefOrOnOff(settings.auxRuntimeAlertNotifyTechnician));
        updateChannel(grp + CH_AUX_OUTDOOR_TEMP_ALERT_NOTIFY_TECHNICIAN,
                VeluxActiveUtils.undefOrOnOff(settings.auxOutdoorTempAlertNotifyTechnician));
        updateChannel(grp + CH_DISABLE_PREHEATING, VeluxActiveUtils.undefOrOnOff(settings.disablePreHeating));
        updateChannel(grp + CH_DISABLE_PRECOOLING, VeluxActiveUtils.undefOrOnOff(settings.disablePreCooling));
        updateChannel(grp + CH_INSTALLER_CODE_REQUIRED, VeluxActiveUtils.undefOrOnOff(settings.installerCodeRequired));
        updateChannel(grp + CH_DR_ACCEPT, VeluxActiveUtils.undefOrString(settings.drAccept));
        updateChannel(grp + CH_IS_RENTAL_PROPERTY, VeluxActiveUtils.undefOrOnOff(settings.isRentalProperty));
        updateChannel(grp + CH_USE_ZONE_CONTROLLER, VeluxActiveUtils.undefOrOnOff(settings.useZoneController));
        updateChannel(grp + CH_RANDOM_START_DELAY_COOL, VeluxActiveUtils.undefOrDecimal(settings.randomStartDelayCool));
        updateChannel(grp + CH_RANDOM_START_DELAY_HEAT, VeluxActiveUtils.undefOrDecimal(settings.randomStartDelayHeat));
        updateChannel(grp + CH_HUMIDITY_HIGH_ALERT,
                VeluxActiveUtils.undefOrQuantity(settings.humidityHighAlert, Units.PERCENT));
        updateChannel(grp + CH_HUMIDITY_LOW_ALERT,
                VeluxActiveUtils.undefOrQuantity(settings.humidityLowAlert, Units.PERCENT));
        updateChannel(grp + CH_DISABLE_HEAT_PUMP_ALERTS, VeluxActiveUtils.undefOrOnOff(settings.disableHeatPumpAlerts));
        updateChannel(grp + CH_DISABLE_ALERTS_ON_IDT, VeluxActiveUtils.undefOrOnOff(settings.disableAlertsOnIdt));
        updateChannel(grp + CH_HUMIDITY_ALERT_NOTIFY, VeluxActiveUtils.undefOrOnOff(settings.humidityAlertNotify));
        updateChannel(grp + CH_HUMIDITY_ALERT_NOTIFY_TECHNICIAN,
                VeluxActiveUtils.undefOrOnOff(settings.humidityAlertNotifyTechnician));
        updateChannel(grp + CH_TEMP_ALERT_NOTIFY, VeluxActiveUtils.undefOrOnOff(settings.tempAlertNotify));
        updateChannel(grp + CH_TEMP_ALERT_NOTIFY_TECHNICIAN,
                VeluxActiveUtils.undefOrOnOff(settings.tempAlertNotifyTechnician));
        updateChannel(grp + CH_MONTHLY_ELECTRICITY_BILL_LIMIT,
                VeluxActiveUtils.undefOrDecimal(settings.monthlyElectricityBillLimit));
        updateChannel(grp + CH_ENABLE_ELECTRICITY_BILL_ALERT,
                VeluxActiveUtils.undefOrOnOff(settings.enableElectricityBillAlert));
        updateChannel(grp + CH_ENABLE_PROJECTED_ELECTRICITY_BILL_ALERT,
                VeluxActiveUtils.undefOrOnOff(settings.enableProjectedElectricityBillAlert));
        updateChannel(grp + CH_ELECTRICITY_BILLING_DAY_OF_MONTH,
                VeluxActiveUtils.undefOrDecimal(settings.electricityBillingDayOfMonth));
        updateChannel(grp + CH_ELECTRICITY_BILL_CYCLE_MONTHS,
                VeluxActiveUtils.undefOrDecimal(settings.electricityBillCycleMonths));
        updateChannel(grp + CH_ELECTRICITY_BILL_START_MONTH,
                VeluxActiveUtils.undefOrDecimal(settings.electricityBillStartMonth));
        updateChannel(grp + CH_VENTILATOR_MIN_ON_TIME_HOME,
                VeluxActiveUtils.undefOrDecimal(settings.ventilatorMinOnTimeHome));
        updateChannel(grp + CH_VENTILATOR_MIN_ON_TIME_AWAY,
                VeluxActiveUtils.undefOrDecimal(settings.ventilatorMinOnTimeAway));
        updateChannel(grp + CH_BACKLIGHT_OFF_DURING_SLEEP,
                VeluxActiveUtils.undefOrOnOff(settings.backlightOffDuringSleep));
        updateChannel(grp + CH_AUTO_AWAY, VeluxActiveUtils.undefOrOnOff(settings.autoAway));
        updateChannel(grp + CH_SMART_CIRCULATION, VeluxActiveUtils.undefOrOnOff(settings.smartCirculation));
        updateChannel(grp + CH_FOLLOW_ME_COMFORT, VeluxActiveUtils.undefOrOnOff(settings.followMeComfort));
        updateChannel(grp + CH_VENTILATOR_TYPE, VeluxActiveUtils.undefOrString(settings.ventilatorType));
        updateChannel(grp + CH_IS_VENTILATOR_TIMER_ON, VeluxActiveUtils.undefOrOnOff(settings.isVentilatorTimerOn));
        updateChannel(grp + CH_VENTILATOR_OFF_DATE_TIME,
                VeluxActiveUtils.undefOrString(settings.ventilatorOffDateTime));
        updateChannel(grp + CH_HAS_UV_FILTER, VeluxActiveUtils.undefOrOnOff(settings.hasUVFilter));
        updateChannel(grp + CH_COOLING_LOCKOUT, VeluxActiveUtils.undefOrOnOff(settings.coolingLockout));
        updateChannel(grp + CH_VENTILATOR_FREE_COOLING, VeluxActiveUtils.undefOrOnOff(settings.ventilatorFreeCooling));
        updateChannel(grp + CH_DEHUMIDIFY_WHEN_HEATING, VeluxActiveUtils.undefOrOnOff(settings.dehumidifyWhenHeating));
        updateChannel(grp + CH_VENTILATOR_DEHUMIDIFY, VeluxActiveUtils.undefOrOnOff(settings.ventilatorDehumidify));
        updateChannel(grp + CH_GROUP_REF, VeluxActiveUtils.undefOrString(settings.groupRef));
        updateChannel(grp + CH_GROUP_NAME, VeluxActiveUtils.undefOrString(settings.groupName));
        updateChannel(grp + CH_GROUP_SETTING, VeluxActiveUtils.undefOrDecimal(settings.groupSetting));
    }

    private void updateProgram(@Nullable ProgramDTO program) {
        if (program == null) {
            return;
        }
        final String grp = CHGRP_PROGRAM + "#";
        updateChannel(grp + CH_PROGRAM_CURRENT_CLIMATE_REF, VeluxActiveUtils.undefOrString(program.currentClimateRef));
        if (program.climates != null) {
            saveValidClimateRefs(program.climates);
        }
    }

    private void saveValidClimateRefs(List<ClimateDTO> climates) {
        validClimateRefs.clear();
        for (ClimateDTO climate : climates) {
            validClimateRefs.add(climate.climateRef);
        }
    }

    private void updateAlert(@Nullable List<AlertDTO> alerts) {
        AlertDTO firstAlert;
        if (alerts == null || alerts.isEmpty()) {
            firstAlert = EMPTY_ALERT;
        } else {
            firstAlert = alerts.get(0);
        }
        final String grp = CHGRP_ALERT + "#";
        updateChannel(grp + CH_ALERT_ACKNOWLEDGE_REF, VeluxActiveUtils.undefOrString(firstAlert.acknowledgeRef));
        updateChannel(grp + CH_ALERT_DATE, VeluxActiveUtils.undefOrString(firstAlert.date));
        updateChannel(grp + CH_ALERT_TIME, VeluxActiveUtils.undefOrString(firstAlert.time));
        updateChannel(grp + CH_ALERT_SEVERITY, VeluxActiveUtils.undefOrString(firstAlert.severity));
        updateChannel(grp + CH_ALERT_TEXT, VeluxActiveUtils.undefOrString(firstAlert.text));
        updateChannel(grp + CH_ALERT_ALERT_NUMBER, VeluxActiveUtils.undefOrDecimal(firstAlert.alertNumber));
        updateChannel(grp + CH_ALERT_ALERT_TYPE, VeluxActiveUtils.undefOrString(firstAlert.alertType));
        updateChannel(grp + CH_ALERT_IS_OPERATOR_ALERT, VeluxActiveUtils.undefOrOnOff(firstAlert.isOperatorAlert));
        updateChannel(grp + CH_ALERT_REMINDER, VeluxActiveUtils.undefOrString(firstAlert.reminder));
        updateChannel(grp + CH_ALERT_SHOW_IDT, VeluxActiveUtils.undefOrOnOff(firstAlert.showIdt));
        updateChannel(grp + CH_ALERT_SHOW_WEB, VeluxActiveUtils.undefOrOnOff(firstAlert.showWeb));
        updateChannel(grp + CH_ALERT_SEND_EMAIL, VeluxActiveUtils.undefOrOnOff(firstAlert.sendEmail));
        updateChannel(grp + CH_ALERT_ACKNOWLEDGEMENT, VeluxActiveUtils.undefOrString(firstAlert.acknowledgement));
        updateChannel(grp + CH_ALERT_REMIND_ME_LATER, VeluxActiveUtils.undefOrOnOff(firstAlert.remindMeLater));
        // updateChannel(grp + CH_ALERT_DEVICE_IDENTIFIER, VeluxActiveUtils.undefOrString(firstAlert.DeviceIdentifier));
        updateChannel(grp + CH_ALERT_NOTIFICATION_TYPE, VeluxActiveUtils.undefOrString(firstAlert.notificationType));
    }

    // private void updateEvent(@Nullable List<EventDTO> events) {
    // EventDTO runningEvent = EMPTY_EVENT;
    // if (events != null && !events.isEmpty()) {
    // for (EventDTO event : events) {
    // if (event.running) {
    // runningEvent = event;
    // break;
    // }
    // }
    // }
    // final String grp = CHGRP_EVENT + "#";
    // updateChannel(grp + CH_EVENT_NAME, VeluxActiveUtils.undefOrString(runningEvent.name));
    // updateChannel(grp + CH_EVENT_TYPE, VeluxActiveUtils.undefOrString(runningEvent.type));
    // updateChannel(grp + CH_EVENT_RUNNING, VeluxActiveUtils.undefOrOnOff(runningEvent.running));
    // updateChannel(grp + CH_EVENT_START_DATE, VeluxActiveUtils.undefOrString(runningEvent.startDate));
    // updateChannel(grp + CH_EVENT_START_TIME, VeluxActiveUtils.undefOrString(runningEvent.startTime));
    // updateChannel(grp + CH_EVENT_END_DATE, VeluxActiveUtils.undefOrString(runningEvent.endDate));
    // updateChannel(grp + CH_EVENT_END_TIME, VeluxActiveUtils.undefOrString(runningEvent.endTime));
    // updateChannel(grp + CH_EVENT_IS_OCCUPIED, VeluxActiveUtils.undefOrOnOff(runningEvent.isOccupied));
    // updateChannel(grp + CH_EVENT_IS_COOL_OFF, VeluxActiveUtils.undefOrOnOff(runningEvent.isCoolOff));
    // updateChannel(grp + CH_EVENT_IS_HEAT_OFF, VeluxActiveUtils.undefOrOnOff(runningEvent.isHeatOff));
    // updateChannel(grp + CH_EVENT_COOL_HOLD_TEMP, VeluxActiveUtils.undefOrTemperature(runningEvent.coolHoldTemp));
    // updateChannel(grp + CH_EVENT_HEAT_HOLD_TEMP, VeluxActiveUtils.undefOrTemperature(runningEvent.heatHoldTemp));
    // updateChannel(grp + CH_EVENT_FAN, VeluxActiveUtils.undefOrString(runningEvent.fan));
    // updateChannel(grp + CH_EVENT_VENT, VeluxActiveUtils.undefOrString(runningEvent.vent));
    // updateChannel(grp + CH_EVENT_VENTILATOR_MIN_ON_TIME,
    // VeluxActiveUtils.undefOrDecimal(runningEvent.ventilatorMinOnTime));
    // updateChannel(grp + CH_EVENT_IS_OPTIONAL, VeluxActiveUtils.undefOrOnOff(runningEvent.isOptional));
    // updateChannel(grp + CH_EVENT_IS_TEMPERATURE_RELATIVE,
    // VeluxActiveUtils.undefOrOnOff(runningEvent.isTemperatureRelative));
    // updateChannel(grp + CH_EVENT_COOL_RELATIVE_TEMP,
    // VeluxActiveUtils.undefOrDecimal(runningEvent.coolRelativeTemp));
    // updateChannel(grp + CH_EVENT_HEAT_RELATIVE_TEMP,
    // VeluxActiveUtils.undefOrDecimal(runningEvent.heatRelativeTemp));
    // updateChannel(grp + CH_EVENT_IS_TEMPERATURE_ABSOLUTE,
    // VeluxActiveUtils.undefOrOnOff(runningEvent.isTemperatureAbsolute));
    // updateChannel(grp + CH_EVENT_DUTY_CYCLE_PERCENTAGE,
    // VeluxActiveUtils.undefOrDecimal(runningEvent.dutyCyclePercentage));
    // updateChannel(grp + CH_EVENT_FAN_MIN_ON_TIME, VeluxActiveUtils.undefOrDecimal(runningEvent.fanMinOnTime));
    // updateChannel(grp + CH_EVENT_OCCUPIED_SENSOR_ACTIVE,
    // VeluxActiveUtils.undefOrOnOff(runningEvent.occupiedSensorActive));
    // updateChannel(grp + CH_EVENT_UNOCCUPIED_SENSOR_ACTIVE,
    // VeluxActiveUtils.undefOrOnOff(runningEvent.unoccupiedSensorActive));
    // updateChannel(grp + CH_EVENT_DR_RAMP_UP_TEMP, VeluxActiveUtils.undefOrDecimal(runningEvent.drRampUpTemp));
    // updateChannel(grp + CH_EVENT_DR_RAMP_UP_TIME, VeluxActiveUtils.undefOrDecimal(runningEvent.drRampUpTime));
    // updateChannel(grp + CH_EVENT_LINK_REF, VeluxActiveUtils.undefOrString(runningEvent.linkRef));
    // updateChannel(grp + CH_EVENT_HOLD_CLIMATE_REF, VeluxActiveUtils.undefOrString(runningEvent.holdClimateRef));
    // }

    private void updateWeather(@Nullable WeatherDTO weather) {
        if (weather == null || weather.forecasts == null) {
            return;
        }
        final String weatherGrp = CHGRP_WEATHER + "#";

        updateChannel(weatherGrp + CH_WEATHER_TIMESTAMP,
                VeluxActiveUtils.undefOrDate(weather.timestamp, timeZoneProvider));
        updateChannel(weatherGrp + CH_WEATHER_WEATHER_STATION, VeluxActiveUtils.undefOrString(weather.weatherStation));

        for (int index = 0; index < weather.forecasts.size(); index++) {
            final String grp = CHGRP_FORECAST + String.format("%d", index) + "#";
            WeatherForecastDTO forecast = weather.forecasts.get(index);
            if (forecast != null) {
                updateChannel(grp + CH_FORECAST_WEATHER_SYMBOL,
                        VeluxActiveUtils.undefOrDecimal(forecast.weatherSymbol));
                updateChannel(grp + CH_FORECAST_WEATHER_SYMBOL_TEXT,
                        VeluxActiveUtils.undefOrString(symbolMap.get(forecast.weatherSymbol)));
                updateChannel(grp + CH_FORECAST_DATE_TIME,
                        VeluxActiveUtils.undefOrDate(forecast.dateTime, timeZoneProvider));
                updateChannel(grp + CH_FORECAST_CONDITION, VeluxActiveUtils.undefOrString(forecast.condition));
                updateChannel(grp + CH_FORECAST_TEMPERATURE, VeluxActiveUtils.undefOrTemperature(forecast.temperature));
                updateChannel(grp + CH_FORECAST_PRESSURE,
                        VeluxActiveUtils.undefOrQuantity(forecast.pressure, Units.MILLIBAR));
                updateChannel(grp + CH_FORECAST_RELATIVE_HUMIDITY,
                        VeluxActiveUtils.undefOrQuantity(forecast.relativeHumidity, Units.PERCENT));
                updateChannel(grp + CH_FORECAST_DEWPOINT, VeluxActiveUtils.undefOrTemperature(forecast.dewpoint));
                updateChannel(grp + CH_FORECAST_VISIBILITY,
                        VeluxActiveUtils.undefOrQuantity(forecast.visibility, SIUnits.METRE));
                updateChannel(grp + CH_FORECAST_WIND_SPEED,
                        VeluxActiveUtils.undefOrQuantity(forecast.windSpeed, ImperialUnits.MILES_PER_HOUR));
                updateChannel(grp + CH_FORECAST_WIND_GUST,
                        VeluxActiveUtils.undefOrQuantity(forecast.windGust, ImperialUnits.MILES_PER_HOUR));
                updateChannel(grp + CH_FORECAST_WIND_DIRECTION, VeluxActiveUtils.undefOrString(forecast.windDirection));
                updateChannel(grp + CH_FORECAST_WIND_BEARING,
                        VeluxActiveUtils.undefOrQuantity(forecast.windBearing, Units.DEGREE_ANGLE));
                updateChannel(grp + CH_FORECAST_POP, VeluxActiveUtils.undefOrQuantity(forecast.pop, Units.PERCENT));
                updateChannel(grp + CH_FORECAST_TEMP_HIGH, VeluxActiveUtils.undefOrTemperature(forecast.tempHigh));
                updateChannel(grp + CH_FORECAST_TEMP_LOW, VeluxActiveUtils.undefOrTemperature(forecast.tempLow));
                updateChannel(grp + CH_FORECAST_SKY, VeluxActiveUtils.undefOrDecimal(forecast.sky));
                updateChannel(grp + CH_FORECAST_SKY_TEXT, VeluxActiveUtils.undefOrString(skyMap.get(forecast.sky)));
            }
        }
    }

    private void updateVersion(@Nullable VersionDTO version) {
        if (version == null) {
            return;
        }
        final String grp = CHGRP_VERSION + "#";
        // updateChannel(grp + CH_Device_FIRMWARE_VERSION,
        // VeluxActiveUtils.undefOrString(version.DeviceFirmwareVersion));
    }

    private void updateLocation(@Nullable LocationDTO loc) {
        LocationDTO location = EMPTY_LOCATION;
        if (loc != null) {
            location = loc;
        }
        final String grp = CHGRP_LOCATION + "#";
        updateChannel(grp + CH_TIME_ZONE_OFFSET_MINUTES,
                VeluxActiveUtils.undefOrDecimal(location.timeZoneOffsetMinutes));
        updateChannel(grp + CH_TIME_ZONE, VeluxActiveUtils.undefOrString(location.timeZone));
        updateChannel(grp + CH_IS_DAYLIGHT_SAVING, VeluxActiveUtils.undefOrOnOff(location.isDaylightSaving));
        updateChannel(grp + CH_STREET_ADDRESS, VeluxActiveUtils.undefOrString(location.streetAddress));
        updateChannel(grp + CH_CITY, VeluxActiveUtils.undefOrString(location.city));
        updateChannel(grp + CH_PROVINCE_STATE, VeluxActiveUtils.undefOrString(location.provinceState));
        updateChannel(grp + CH_COUNTRY, VeluxActiveUtils.undefOrString(location.country));
        updateChannel(grp + CH_POSTAL_CODE, VeluxActiveUtils.undefOrString(location.postalCode));
        updateChannel(grp + CH_PHONE_NUMBER, VeluxActiveUtils.undefOrString(location.phoneNumber));
        updateChannel(grp + CH_MAP_COORDINATES, VeluxActiveUtils.undefOrPoint(location.mapCoordinates));
    }

    private void updateHouseDetails(@Nullable HomesDataResponse hd) {
        HomesDataResponse houseDetails = EMPTY_HOUSEDETAILS;
        if (hd != null) {
            houseDetails = hd;
        }
        final String grp = CHGRP_HOUSE_DETAILS + "#";
        // updateChannel(grp + CH_HOUSEDETAILS_STYLE, VeluxActiveUtils.undefOrString(houseDetails.style));
        // updateChannel(grp + CH_HOUSEDETAILS_SIZE, VeluxActiveUtils.undefOrDecimal(houseDetails.size));
        // updateChannel(grp + CH_HOUSEDETAILS_NUMBER_OF_FLOORS,
        // VeluxActiveUtils.undefOrDecimal(houseDetails.numberOfFloors));
        // updateChannel(grp + CH_HOUSEDETAILS_NUMBER_OF_ROOMS,
        // VeluxActiveUtils.undefOrDecimal(houseDetails.getBody().getHomes()[0].getRooms().length));
        // updateChannel(grp + CH_HOUSEDETAILS_NUMBER_OF_OCCUPANTS,
        // VeluxActiveUtils.undefOrDecimal(houseDetails.numberOfOccupants));
        // updateChannel(grp + CH_HOUSEDETAILS_AGE, VeluxActiveUtils.undefOrDecimal(houseDetails.age));
        // updateChannel(grp + CH_HOUSEDETAILS_WINDOW_EFFICIENCY,
        // VeluxActiveUtils.undefOrDecimal(houseDetails.windowEfficiency));
    }

    private void updateManagement(@Nullable ManagementDTO mgmt) {
        ManagementDTO management = EMPTY_MANAGEMENT;
        if (mgmt != null) {
            management = mgmt;
        }
        final String grp = CHGRP_MANAGEMENT + "#";
        updateChannel(grp + CH_MANAGEMENT_ADMIN_CONTACT,
                VeluxActiveUtils.undefOrString(management.administrativeContact));
        updateChannel(grp + CH_MANAGEMENT_BILLING_CONTACT, VeluxActiveUtils.undefOrString(management.billingContact));
        updateChannel(grp + CH_MANAGEMENT_NAME, VeluxActiveUtils.undefOrString(management.name));
        updateChannel(grp + CH_MANAGEMENT_PHONE, VeluxActiveUtils.undefOrString(management.phone));
        updateChannel(grp + CH_MANAGEMENT_EMAIL, VeluxActiveUtils.undefOrString(management.email));
        updateChannel(grp + CH_MANAGEMENT_WEB, VeluxActiveUtils.undefOrString(management.web));
        updateChannel(grp + CH_MANAGEMENT_SHOW_ALERT_IDT, VeluxActiveUtils.undefOrOnOff(management.showAlertIdt));
        updateChannel(grp + CH_MANAGEMENT_SHOW_ALERT_WEB, VeluxActiveUtils.undefOrOnOff(management.showAlertWeb));
    }

    private void updateTechnician(@Nullable TechnicianDTO tech) {
        TechnicianDTO technician = EMPTY_TECHNICIAN;
        if (tech != null) {
            technician = tech;
        }
        final String grp = CHGRP_TECHNICIAN + "#";
        updateChannel(grp + CH_TECHNICIAN_CONTRACTOR_REF, VeluxActiveUtils.undefOrString(technician.contractorRef));
        updateChannel(grp + CH_TECHNICIAN_NAME, VeluxActiveUtils.undefOrString(technician.name));
        updateChannel(grp + CH_TECHNICIAN_PHONE, VeluxActiveUtils.undefOrString(technician.phone));
        updateChannel(grp + CH_TECHNICIAN_STREET_ADDRESS, VeluxActiveUtils.undefOrString(technician.streetAddress));
        updateChannel(grp + CH_TECHNICIAN_CITY, VeluxActiveUtils.undefOrString(technician.city));
        updateChannel(grp + CH_TECHNICIAN_PROVINCE_STATE, VeluxActiveUtils.undefOrString(technician.provinceState));
        updateChannel(grp + CH_TECHNICIAN_COUNTRY, VeluxActiveUtils.undefOrString(technician.country));
        updateChannel(grp + CH_TECHNICIAN_POSTAL_CODE, VeluxActiveUtils.undefOrString(technician.postalCode));
        updateChannel(grp + CH_TECHNICIAN_EMAIL, VeluxActiveUtils.undefOrString(technician.email));
        updateChannel(grp + CH_TECHNICIAN_WEB, VeluxActiveUtils.undefOrString(technician.web));
    }

    private void updateChannel(String channelId, State state) {
        updateState(channelId, state);
        stateCache.put(channelId, state);
    }

    @SuppressWarnings("null")
    private void updateRemoteSensors(@Nullable List<RemoteSensorDTO> remoteSensors) {
        if (remoteSensors == null) {
            return;
        }
        logger.debug("DeviceBridge: Device '{}' has {} remote sensors", deviceID, remoteSensors.size());
        for (RemoteSensorDTO sensor : remoteSensors) {
            VeluxActiveNXSThingHandler handler = sensorHandlers.get(sensor.id);
            if (handler != null) {
                logger.debug("DeviceBridge: Sending data to sensor handler '{}({})' of type '{}'", sensor.id,
                        sensor.name, sensor.type);
                handler.updateChannels(sensor);
            }
        }
    }

    private void performDeviceUpdate(BasicDeviceModule Device) {
        SelectionDTO selection = new SelectionDTO();
        // selection.setDevices(Set.of(deviceID));
        // DeviceUpdateRequestDTO request = new DeviceUpdateRequestDTO(selection);
        // request.Device = Device;
        VeluxActiveAccountBridgeHandler handler = getBridgeHandler();
        if (handler != null) {
            // handler.performDeviceUpdate(request);
        }
    }

    private @Nullable VeluxActiveAccountBridgeHandler getBridgeHandler() {
        VeluxActiveAccountBridgeHandler handler = null;
        Bridge bridge = getBridge();
        if (bridge != null) {
            handler = (VeluxActiveAccountBridgeHandler) bridge.getHandler();
        }
        return handler;
    }

    @SuppressWarnings("null")
    private boolean isChannelReadOnly(ChannelUID channelUID) {
        Boolean isReadOnly = channelReadOnlyMap.get(channelUID);
        return isReadOnly != null ? isReadOnly : true;
    }

    private void clearSavedState() {
        savedDevices = null;
        stateCache.clear();
    }

    private void initializeReadOnlyChannels() {
        channelReadOnlyMap.clear();
        for (Channel channel : thing.getChannels()) {
            ChannelTypeUID channelTypeUID = channel.getChannelTypeUID();
            if (channelTypeUID != null) {
                ChannelType channelType = channelTypeRegistry.getChannelType(channelTypeUID, null);
                if (channelType != null) {
                    StateDescription state = channelType.getState();
                    if (state != null) {
                        channelReadOnlyMap.putIfAbsent(channel.getUID(), state.isReadOnly());
                    }
                }
            }
        }
    }
}
