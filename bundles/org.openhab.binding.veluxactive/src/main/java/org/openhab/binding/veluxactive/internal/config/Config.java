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
package org.openhab.binding.veluxactive.internal.config;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.storage.Storage;
import org.openhab.core.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton configuration manager for Velux Active integration.
 * Handles loading, saving, and providing access to static and dynamic configuration.
 *
 * @author Volker Daube - Initial contribution
 */
@NonNullByDefault
public final class Config {

    private static final String CONFIG_STORAGE = "veluxactive.storage";
    private static final String CONFIG_STORAGE_REFRESHTOKEN = "veluxactive.storage.refreshToken";
    private static final String CONFIG_STORAGE_ACCESSTOKEN = "veluxactive.storage.accessToken";
    private static final String CONFIG_STORAGE_ACCESSTOKEN_TIMESTAMP = "veluxactive.storage.accessToken.Timestamp";
    private static final String CONFIG_STORAGE_ACCESSTOKEN_EXPIRE_IN = "veluxactive.storage.accessToken.ExpiresIn";
    private static final String CONFIG_STORAGE_ACCESSTOKEN_EXPIRES_IN = "veluxactive.storage.accessToken.ExpireIn";
    // Logger for debugging and error reporting
    private static final Logger logger = LoggerFactory.getLogger(Config.class);

    // Singleton instance and load status
    private static Config mInstance = new Config();

    private @Nullable String username = null;
    private @Nullable String password = null;
    private @Nullable String refreshToken = null;
    private @Nullable String accessToken = null;
    private boolean mChanged = false;
    // Number of minutes until the token expires
    private int expiresIn = 0;
    // Unclear how this differs from expires_in
    private int expireIn = 0;
    private long accessTokenTimeStamp = 0L;
    private @Nullable StorageService mStorageService = null;

    private @Nullable String client_id = null;
    private @Nullable String client_secret = null;
    private @Nullable String app_version = null;

    // Whether to use a proxy for network connections
    private boolean use_proxy = false;
    // Proxy IP address (nullable)
    private @Nullable String proxy_ip = null;
    // Proxy port number
    private int proxy_port = 0;
    // Maximum number of reconnect attempts
    private int max_reconnect_attempts = 3;
    // Delay between reconnect attempts in milliseconds
    private long retry_delay_seconds = 5; // in milliseconds
    // Keep-alive interval in minutes
    private int keep_alive_intervall_minutes = 5; // in minutes

    /**
     * Private constructor to prevent instantiation.
     */
    private Config() {
        // private constructor to prevent instantiation
    }

    /**
     * Returns the singleton instance.
     *
     * @return Config instance
     */
    public static Config getInstance() {
        return mInstance;
    }

    /**
     * @param storageService
     * @param veluxConfig
     */
    public void setAndLoadStorageService(StorageService storageService, VeluxActiveAccountConfiguration veluxConfig) {
        mStorageService = storageService;
        load();
        if (veluxConfig.username != null) {
            setUsername(veluxConfig.username);
        } else {
            logger.error("AccountBridge: Velux username is not set in the configuration");
        }
        if (veluxConfig.password != null) {
            setPassword(veluxConfig.password);
        } else {
            logger.error("AccountBridge: Velux password is not set in the configuration");
        }
        if (veluxConfig.clientID != null) {
            setClientID(veluxConfig.clientID);
        } else {
            logger.error("AccountBridge: Velux client ID is not set in the configuration");
        }
        if (veluxConfig.clientSecret != null) {
            setClientSecret(veluxConfig.clientSecret);
        } else {
            logger.error("AccountBridge: Velux client secret is not set in the configuration");
        }
        if (veluxConfig.appVersion != null) {
            setAppVersion(veluxConfig.appVersion);
        } else {
            logger.error("AccountBridge: Velux app version is not set in the configuration");
        }

        if (veluxConfig.proxyHostname != null) {
            setProxyHostname(veluxConfig.proxyHostname);
        } else {
            if (useProxy()) {
                logger.debug("AccountBridge: use proxy hostname is not set in the configuration");
            }
        }

        Integer value;
        value = veluxConfig.proxyPort;
        setProxyPort(value == null ? 0 : value);
        value = veluxConfig.maxReconnectAttempts;
        setMaxReconnectAttempts(value == null ? 0 : value);
        value = veluxConfig.retryDelaySeconds;
        setRetryDelaySeconds(value == null ? 0 : value);
        value = veluxConfig.keepAliveIntervallMinutes;
        setKeepAliveIntervallMinutes(value == null ? 0 : value);

        Boolean booleanValue = veluxConfig.useProxy;
        setUseProxy(booleanValue == null ? false : booleanValue.booleanValue());
    }

    /**
     * Returns the singleton instance, loading configuration.
     *
     * @return boolean success
     */
    private boolean load() {
        boolean successfullyLoaded = false;
        if (mStorageService == null) {
            logger.error("StorageService is not set. Cannot load configuration.");
        } else {
            @SuppressWarnings("null")
            Storage<String> storage = mStorageService.getStorage(CONFIG_STORAGE);

            refreshToken = storage.get(CONFIG_STORAGE_REFRESHTOKEN);

            accessToken = storage.get(CONFIG_STORAGE_ACCESSTOKEN);

            String timestampString = storage.get(CONFIG_STORAGE_ACCESSTOKEN_TIMESTAMP);
            if (timestampString != null) {
                try {
                    accessTokenTimeStamp = Long.parseLong(timestampString);
                } catch (NumberFormatException e) {
                    logger.error("Access token timestamp format is wrong: {}. {}", timestampString, e.getMessage());
                    accessTokenTimeStamp = 0L;
                }
            } else {
                accessTokenTimeStamp = 0L;
            }

            String expiresInString = storage.get(CONFIG_STORAGE_ACCESSTOKEN_EXPIRES_IN);
            if (expiresInString != null) {
                try {
                    expiresIn = Integer.parseInt(expiresInString);
                } catch (NumberFormatException e) {
                    logger.error("Error: {}", e.getMessage());
                    expiresIn = 0;
                }
            } else {
                expiresIn = 0;
            }

            String expireInString = storage.get(CONFIG_STORAGE_ACCESSTOKEN_EXPIRE_IN);
            if (expireInString != null) {
                try {
                    expireIn = Integer.parseInt(expireInString);
                } catch (NumberFormatException e) {
                    logger.error("Error: {}", e.getMessage());
                    expireIn = 0;
                }
            } else {
                expireIn = 0;
            }
            successfullyLoaded = true;
        }
        return successfullyLoaded;
    }

    public @Nullable String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(@Nullable String refreshToken) {
        mChanged = true;
        this.refreshToken = refreshToken;
    }

    public @Nullable String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(@Nullable String accessToken) {
        mChanged = true;
        this.accessToken = accessToken;
    }

    public int getAccessTokenExpiration() {
        return expiresIn;
    }

    public void setAccessTokenExpiration(int expiresIn) {
        mChanged = true;
        this.expiresIn = expiresIn;
    }

    public int getExpireIn() {
        return expireIn;
    }

    public void setExpireIn(int expireIn) {
        mChanged = true;
        this.expireIn = expireIn;
    }

    public long getAccessTokenTimestamp() {
        return accessTokenTimeStamp;
    }

    public void setAccessTokenTimestampToNow() {
        mChanged = true;
        setAccessTokenTimestamp(Instant.now().toEpochMilli());
    }

    public void setAccessTokenTimestamp(long accessTokenTimeStamp) {
        mChanged = true;
        this.accessTokenTimeStamp = accessTokenTimeStamp;
    }

    public String getReadableAccessTokenTimestamp() {
        return getReadableTimestamp(getAccessTokenTimestamp());
    }

    /**
     * Converts a timestamp in milliseconds to a human-readable string.
     *
     * @param millis timestamp in milliseconds
     * @return formatted string
     */
    public String getReadableTimestamp(long millis) {
        Instant instant = Instant.ofEpochMilli(millis);
        ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss:SSS");
        String readableTimeStamp = formatter.format(zdt);
        return readableTimeStamp;
    }

    public @Nullable String getUsername() {
        return this.username;
    }

    public void setUsername(@Nullable String username) {
        this.username = username;
    }

    public @Nullable String getPassword() {
        return this.password;
    }

    public void setPassword(@Nullable String password) {
        this.password = password;
    }

    public @Nullable String getClientID() {
        return client_id;
    }

    public void setClientID(@Nullable String clientID) {
        this.client_id = clientID;
    }

    public @Nullable String getClientSecret() {
        return client_secret;
    }

    public void setClientSecret(@Nullable String clientSecret) {
        this.client_secret = clientSecret;
    }

    public @Nullable String getAppVersion() {
        return app_version;
    }

    public void setAppVersion(@Nullable String appVersion) {
        this.app_version = appVersion;
    }

    /**
     * Returns whether a proxy should be used.
     */
    public boolean useProxy() {
        return use_proxy;
    }

    /**
     * Sets whether to use a proxy.
     */
    public void setUseProxy(boolean useProxy) {
        this.use_proxy = useProxy;
    }

    /**
     * Returns the proxy IP address.
     */
    public @Nullable String getProxyIP() {
        return proxy_ip;
    }

    /**
     * Sets the proxy IP address.
     */
    public void setProxyHostname(@Nullable String proxyIP) {
        this.proxy_ip = proxyIP;
    }

    /**
     * Returns the proxy port number.
     */
    public int getProxyPort() {
        return proxy_port;
    }

    /**
     * Sets the proxy port number.
     */
    public void setProxyPort(int proxyPort) {
        this.proxy_port = proxyPort;
    }

    /**
     * Returns the maximum number of reconnect attempts.
     */
    public int getMaxReconnectAttempts() {
        return max_reconnect_attempts;
    }

    /**
     * Sets the maximum number of reconnect attempts.
     */
    public void setMaxReconnectAttempts(int maxReconnectAttempts) {
        this.max_reconnect_attempts = maxReconnectAttempts;
    }

    /**
     * Returns the delay between reconnect attempts in milliseconds.
     */
    public long getRetryDelaySeconds() {
        return retry_delay_seconds;
    }

    /**
     * Sets the delay between reconnect attempts in milliseconds.
     */
    public void setRetryDelaySeconds(long retryDelaySeconds) {
        this.retry_delay_seconds = retryDelaySeconds;
    }

    /**
     * Returns the keep-alive interval in minutes.
     */
    public int getKeepAliveIntervallMinutes() {
        return keep_alive_intervall_minutes;
    }

    /**
     * Sets the keep-alive interval in minutes.
     */
    public void setKeepAliveIntervallMinutes(int keepAliveIntervallMinutes) {
        this.keep_alive_intervall_minutes = keepAliveIntervallMinutes;
    }

    /**
     * Returns whether any storable config paramter has changed since last save.
     *
     * @return true if changed
     */
    public boolean isChanged() {
        return mChanged;
    }

    /**
     * Resets the changed flag after saving.
     */
    public void resetChangedFlag() {
        mChanged = false;
    }

    /**
     * Saves the configuration.
     *
     * @return true if saved successfully, false otherwise
     */
    public boolean save() {
        boolean saved = false;
        if (isChanged()) {
            if (mStorageService == null) {
                logger.error("StorageService is not set. Cannot save configuration.");
            } else {
                @SuppressWarnings("null")
                Storage<String> storage = mStorageService.getStorage(CONFIG_STORAGE);
                storage.put(CONFIG_STORAGE_REFRESHTOKEN, refreshToken);
                storage.put(CONFIG_STORAGE_ACCESSTOKEN, accessToken);
                storage.put(CONFIG_STORAGE_ACCESSTOKEN_TIMESTAMP, String.valueOf(accessTokenTimeStamp));
                storage.put(CONFIG_STORAGE_ACCESSTOKEN_EXPIRES_IN, String.valueOf(expiresIn));
                storage.put(CONFIG_STORAGE_ACCESSTOKEN_EXPIRE_IN, String.valueOf(expireIn));
                resetChangedFlag();
                saved = true;
            }
        }
        return saved;
    }

    /**
     * Returns a JSON string representation of the config.
     *
     * @return JSON string
     */
}
