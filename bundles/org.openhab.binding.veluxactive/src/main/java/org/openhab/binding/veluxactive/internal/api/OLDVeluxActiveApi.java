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
// package org.openhab.binding.veluxactive.internal.api;
//
// import static org.openhab.binding.veluxactive.internal.VeluxActiveBindingConstants.*;
//
// import java.io.ByteArrayInputStream;
// import java.io.EOFException;
// import java.io.IOException;
// import java.net.URLEncoder;
// import java.nio.charset.StandardCharsets;
// import java.time.Instant;
// import java.time.LocalDateTime;
// import java.util.List;
// import java.util.Properties;
// import java.util.Set;
// import java.util.concurrent.TimeoutException;
//
// import org.eclipse.jdt.annotation.NonNullByDefault;
// import org.eclipse.jdt.annotation.Nullable;
// import org.eclipse.jetty.client.HttpClient;
// import org.openhab.binding.veluxactive.internal.dto.AbstractResponseDTO;
// import org.openhab.binding.veluxactive.internal.dto.SelectionDTO;
// import org.openhab.binding.veluxactive.internal.dto.SelectionType;
// import org.openhab.binding.veluxactive.internal.dto.thermostat.InstantDeserializer;
// import org.openhab.binding.veluxactive.internal.dto.thermostat.LocalDateTimeDeserializer;
// import org.openhab.binding.veluxactive.internal.dto.thermostat.ThermostatDTO;
// import org.openhab.binding.veluxactive.internal.dto.thermostat.ThermostatRequestDTO;
// import org.openhab.binding.veluxactive.internal.dto.thermostat.ThermostatResponseDTO;
// import org.openhab.binding.veluxactive.internal.dto.thermostat.ThermostatUpdateRequestDTO;
// import org.openhab.binding.veluxactive.internal.dto.thermostat.summary.RevisionDTO;
// import org.openhab.binding.veluxactive.internal.dto.thermostat.summary.RevisionDTODeserializer;
// import org.openhab.binding.veluxactive.internal.dto.thermostat.summary.RunningDTO;
// import org.openhab.binding.veluxactive.internal.dto.thermostat.summary.RunningDTODeserializer;
// import org.openhab.binding.veluxactive.internal.dto.thermostat.summary.SummaryResponseDTO;
// import org.openhab.binding.veluxactive.internal.function.FunctionRequest;
// import org.openhab.binding.veluxactive.internal.handler.VeluxActiveAccountBridgeHandler;
// import org.openhab.binding.veluxactive.internal.util.ExceptionUtils;
// import org.openhab.core.auth.client.oauth2.AccessTokenResponse;
// import org.openhab.core.auth.client.oauth2.OAuthClientService;
// import org.openhab.core.auth.client.oauth2.OAuthException;
// import org.openhab.core.auth.client.oauth2.OAuthFactory;
// import org.openhab.core.auth.client.oauth2.OAuthResponseException;
// import org.openhab.core.io.net.http.HttpUtil;
// import org.openhab.core.thing.ThingStatus;
// import org.openhab.core.thing.ThingStatusDetail;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
//
// import com.google.gson.Gson;
// import com.google.gson.GsonBuilder;
// import com.google.gson.JsonSyntaxException;
//
/**
 * The {@link VeluxActiveApi} is responsible for managing all communication with
 * the VeluxActive API service.
 *
 * @author Mark Hilbush - Initial contribution
 */
// @NonNullByDefault
// public class VeluxActiveApi {
//
// private static final Gson GSON = new GsonBuilder().registerTypeAdapter(Instant.class, new InstantDeserializer())
// .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeDeserializer())
// .registerTypeAdapter(RevisionDTO.class, new RevisionDTODeserializer())
// .registerTypeAdapter(RunningDTO.class, new RunningDTODeserializer()).create();
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
// public static Gson getGson() {
// return GSON;
// }
//
// private final Logger logger = LoggerFactory.getLogger(VeluxActiveApi.class);
//
// private final VeluxActiveAccountBridgeHandler bridgeHandler;
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
// public VeluxActiveApi(final VeluxActiveAccountBridgeHandler bridgeHandler, final int apiTimeout,
// org.openhab.core.auth.client.oauth2.OAuthFactory oAuthFactory, HttpClient httpClient) {
// this.bridgeHandler = bridgeHandler;
// this.veluxUsername = username;
// this.veluxPassword = password;
// this.clientID = clientID;
// this.clientSecret = clientSecret;
// this.apiTimeout = apiTimeout;
// this.oAuthFactory = oAuthFactory;
// this.httpClient = httpClient;
//
// createOAuthClientService();
// }
//
// public void createOAuthClientService() {
// String bridgeUID = bridgeHandler.getThing().getUID().getAsString();
// logger.debug("API: Creating OAuth Client Service for {}", bridgeUID);
// OAuthClientService service = oAuthFactory.createOAuthClientService(bridgeUID, VELUXACTIVE_TOKEN_URL, "",
// clientID, clientSecret, VELUXACTIVE_SCOPE, false);
// veluxActiveAuth = new VeluxActiveAuth(bridgeHandler, veluxUsername, apiTimeout, service, httpClient);
// oAuthClientService = service;
// }
//
// public void deleteOAuthClientService() {
// String bridgeUID = bridgeHandler.getThing().getUID().getAsString();
// logger.debug("API: Deleting OAuth Client Service for {}", bridgeUID);
// oAuthFactory.deleteServiceAndAccessToken(bridgeUID);
// }
//
// public void closeOAuthClientService() {
// String bridgeUID = bridgeHandler.getThing().getUID().getAsString();
// logger.debug("API: Closing OAuth Client Service for {}", bridgeUID);
// oAuthFactory.ungetOAuthService(bridgeUID);
// }
//
// /**
// * Check to see if the VeluxActive authorization process is complete. This will be determined
// * by requesting an AccessTokenResponse from the OHC OAuth service. If we get a valid
// * response, then assume that the VeluxActive authorization process is complete. Otherwise,
// * start the VeluxActive authorization process.
// */
// private boolean isAuthorized(boolean forceTokenRefresh) {
// boolean isAuthorized = false;
// try {
// AccessTokenResponse localAccessTokenResponse = oAuthClientService.getAccessTokenResponse();
// if (localAccessTokenResponse != null) {
// logger.trace("API: Got AccessTokenResponse from OAuth service: {}", localAccessTokenResponse);
// if (forceTokenRefresh
// || localAccessTokenResponse.isExpired(Instant.now(), TOKEN_EXPIRES_IN_BUFFER_SECONDS)) {
// logger.debug("API: Refreshing access token");
// localAccessTokenResponse = oAuthClientService.refreshToken();
// }
// veluxActiveAuth.setState(VeluxActiveAuthState.COMPLETE);
// isAuthorized = true;
// } else {
// logger.debug(
// "API: Didn't get an AccessTokenResponse from OAuth service - doVeluxActiveAuthorization!!!");
// if (veluxActiveAuth.isComplete()) {
// veluxActiveAuth.setState(VeluxActiveAuthState.NEED_PIN);
// }
// }
// accessTokenResponse = localAccessTokenResponse;
// veluxActiveAuth.doAuthorization();
// } catch (OAuthException | IOException | RuntimeException e) {
// if (logger.isDebugEnabled()) {
// logger.warn("API: Got exception trying to get access token from OAuth service", e);
// } else {
// logger.warn("API: Got {} trying to get access token from OAuth service: {}",
// e.getClass().getSimpleName(), e.getMessage());
// }
// } catch (VeluxActiveAuthException e) {
// if (logger.isDebugEnabled()) {
// logger.warn("API: The VeluxActive authorization process threw an exception", e);
// } else {
// logger.warn("API: The VeluxActive authorization process threw an exception: {}", e.getMessage());
// }
// veluxActiveAuth.setState(VeluxActiveAuthState.NEED_PIN);
// } catch (OAuthResponseException e) {
// handleOAuthException(e);
// }
// return isAuthorized;
// }
//
// private boolean isAuthorized() {
// return isAuthorized(DONT_FORCE_TOKEN_REFRESH);
// }
//
// private void handleOAuthException(OAuthResponseException e) {
// if ("invalid_grant".equalsIgnoreCase(e.getError())) {
// // Usually indicates that the refresh token is no longer valid and will require reauthorization
// logger.debug(
// "API: Received 'invalid_grant' error response. Please reauthorize application with VeluxActive");
// deleteOAuthClientService();
// createOAuthClientService();
// } else {
// // Other errors may not require reauthorization and/or may not apply
// logger.debug("API: Exception getting access token: error='{}', description='{}'", e.getError(),
// e.getErrorDescription());
// }
// }
//
// public @Nullable SummaryResponseDTO performThermostatSummaryQuery() {
// logger.debug("API: Perform thermostat summary query");
// if (!isAuthorized()) {
// return null;
// }
// SelectionDTO selection = new SelectionDTO();
// selection.selectionType = SelectionType.REGISTERED;
// selection.includeEquipmentStatus = Boolean.TRUE;
// String requestJson = GSON.toJson(new ThermostatRequestDTO(selection), ThermostatRequestDTO.class);
// String response = executeGet(VELUXACTIVE_THERMOSTAT_SUMMARY_URL, requestJson);
// if (response != null) {
// try {
// SummaryResponseDTO summaryResponse = GSON.fromJson(response, SummaryResponseDTO.class);
// if (isSuccess(summaryResponse)) {
// return summaryResponse;
// }
// } catch (JsonSyntaxException e) {
// logJSException(e, response);
// }
// }
// return null;
// }
//
// public List<ThermostatDTO> queryRegisteredThermostats() {
// return performThermostatQuery(null);
// }
//
// public List<ThermostatDTO> performThermostatQuery(final @Nullable Set<String> thermostatIds) {
// logger.debug("API: Perform query on thermostat: '{}'", thermostatIds);
// if (!isAuthorized()) {
// return EMPTY_THERMOSTATS;
// }
// SelectionDTO selection = bridgeHandler.getSelection();
// selection.setThermostats(thermostatIds);
// String requestJson = GSON.toJson(new ThermostatRequestDTO(selection), ThermostatRequestDTO.class);
// String response = executeGet(VELUXACTIVE_THERMOSTAT_URL, requestJson);
// if (response != null) {
// try {
// ThermostatResponseDTO thermostatsResponse = GSON.fromJson(response, ThermostatResponseDTO.class);
// if (isSuccess(thermostatsResponse)) {
// if (thermostatsResponse.thermostatList != null) {
// return thermostatsResponse.thermostatList;
// }
// }
// } catch (JsonSyntaxException e) {
// logJSException(e, response);
// }
// }
// return EMPTY_THERMOSTATS;
// }
//
// public boolean performThermostatFunction(FunctionRequest request) {
// logger.debug("API: Perform function on thermostat: '{}'", request.selection.selectionMatch);
// if (!isAuthorized()) {
// return false;
// }
// return executePost(VELUXACTIVE_THERMOSTAT_URL, GSON.toJson(request, FunctionRequest.class));
// }
//
// public boolean performThermostatUpdate(ThermostatUpdateRequestDTO request) {
// logger.debug("API: Perform update on thermostat: '{}'", request.selection.selectionMatch);
// if (!isAuthorized()) {
// return false;
// }
// return executePost(VELUXACTIVE_THERMOSTAT_UPDATE_URL, GSON.toJson(request, ThermostatUpdateRequestDTO.class));
// }
//
// private String buildQueryUrl(String baseUrl, String requestJson) {
// final StringBuilder urlBuilder = new StringBuilder(baseUrl);
// urlBuilder.append("?json=");
// urlBuilder.append(URLEncoder.encode(requestJson, StandardCharsets.UTF_8));
// return urlBuilder.toString();
// }
//
// private @Nullable String executeGet(String url, String json) {
// String response = null;
// try {
// long startTime = System.currentTimeMillis();
// logger.trace("API: Get Request json is '{}'", json);
// response = HttpUtil.executeUrl("GET", buildQueryUrl(url, json), setHeaders(), null, null, apiTimeout);
// logger.trace("API: Response took {} msec: {}", System.currentTimeMillis() - startTime, response);
// } catch (IOException e) {
// logIOException(e);
// } catch (VeluxActiveAuthException e) {
// logger.debug("API: Unable to execute GET: {}", e.getMessage());
// }
// return response;
// }
//
// private boolean executePost(String url, String json) {
// try {
// logger.trace("API: Post request json is '{}'", json);
// long startTime = System.currentTimeMillis();
// String response = HttpUtil.executeUrl("POST", url, setHeaders(), new ByteArrayInputStream(json.getBytes()),
// "application/json", apiTimeout);
// logger.trace("API: Response took {} msec: {}", System.currentTimeMillis() - startTime, response);
// try {
// ThermostatResponseDTO thermostatsResponse = GSON.fromJson(response, ThermostatResponseDTO.class);
// return isSuccess(thermostatsResponse);
// } catch (JsonSyntaxException e) {
// logJSException(e, response);
// }
// } catch (IOException e) {
// logIOException(e);
// } catch (VeluxActiveAuthException e) {
// logger.debug("API: Unable to execute POST: {}", e.getMessage());
// }
// return false;
// }
//
// private void logIOException(Exception e) {
// Throwable rootCause = ExceptionUtils.getRootThrowable(e);
// if (rootCause instanceof TimeoutException || rootCause instanceof EOFException) {
// // These are "normal" errors and should be logged as DEBUG
// logger.debug("API: Call to VeluxActive API failed with exception: {}: {}",
// rootCause.getClass().getSimpleName(), rootCause.getMessage());
// } else {
// // What's left are unexpected errors that should be logged as WARN with a full stack trace
// logger.warn("API: Call to VeluxActive API failed", e);
// }
// }
//
// private void logJSException(Exception e, String response) {
// // The API sometimes returns an HTML page complaining of an SSL error
// logger.debug("API: JsonSyntaxException parsing response: {}", response, e);
// }
//
// private boolean isSuccess(@Nullable AbstractResponseDTO response) {
// if (response == null) {
// logger.debug("API: VeluxActive API returned null response");
// } else if (response.status.code.intValue() != 0) {
// logger.debug("API: VeluxActive API returned unsuccessful status: code={}, message={}", response.status.code,
// response.status.message);
// if (response.status.code == VELUXACTIVE_DEAUTHORIZED_TOKEN) {
// // Token has been deauthorized, so restart the authorization process from the beginning
// logger.warn("API: Reset OAuth Client Service due to deauthorized token");
// deleteOAuthClientService();
// createOAuthClientService();
// } else if (response.status.code == VELUXACTIVE_TOKEN_EXPIRED) {
// logger.debug("API: Unable to complete API call because token is expired. Try to refresh the token...");
// // Log some additional debug information about the current AccessTokenResponse
// AccessTokenResponse localAccessTokenResponse = accessTokenResponse;
// if (localAccessTokenResponse != null) {
// logger.debug("API: AccessTokenResponse created on: {}", localAccessTokenResponse.getCreatedOn());
// logger.debug("API: AccessTokenResponse expires in: {}", localAccessTokenResponse.getExpiresIn());
// }
// logger.debug("API: VeluxActive API attempting to force an access token refresh");
// if (isAuthorized(FORCE_TOKEN_REFRESH)) {
// return true;
// } else {
// logger.warn("API: isAuthorized was NOT successful forcing the access token refresh");
// bridgeHandler.updateBridgeStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
// "Unable to force refresh the access token");
// }
// }
// } else {
// bridgeHandler.updateBridgeStatus(ThingStatus.ONLINE);
// return true;
// }
// return false;
// }
//
// private Properties setHeaders() throws VeluxActiveAuthException {
// AccessTokenResponse atr = accessTokenResponse;
// if (atr == null) {
// throw new VeluxActiveAuthException("Can not set auth header because access token is null");
// }
// Properties headers = new Properties();
// headers.putAll(HTTP_HEADERS);
// headers.put("Authorization", "Bearer " + atr.getAccessToken());
// return headers;
// }
// }
