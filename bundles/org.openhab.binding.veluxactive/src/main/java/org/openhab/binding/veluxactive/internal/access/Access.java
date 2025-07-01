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
package org.openhab.binding.veluxactive.internal.access;

import java.time.Instant;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.veluxactive.internal.comm.Communication;
import org.openhab.binding.veluxactive.internal.comm.Communication.GetPostResult;
import org.openhab.binding.veluxactive.internal.comm.msgs.InitialLoginRequest;
import org.openhab.binding.veluxactive.internal.comm.msgs.InitialLoginResponse;
import org.openhab.binding.veluxactive.internal.comm.msgs.MsgFactory;
import org.openhab.binding.veluxactive.internal.comm.msgs.RefreshTokenRequest;
import org.openhab.binding.veluxactive.internal.comm.msgs.RefreshTokenResponse;
import org.openhab.binding.veluxactive.internal.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton class for managing access and refresh tokens for Velux Active API.
 * Handles token validation, refresh, and initial login logic.
 *
 * @author Volker Daube - Initial contribution
 */
@NonNullByDefault
public class Access {

    // Singleton instance
    private static final Access mInstance = new Access();
    // Logger for debugging and error reporting
    private static final Logger logger = LoggerFactory.getLogger(Access.class);
    // Communication manager singleton
    private static final Communication comMgr = Communication.getInstance();
    private final Config mConfig = Config.getInstance();

    /**
     * Private constructor to prevent instantiation.
     */
    private Access() {
        // private constructor to prevent instantiation
    }

    /**
     * Returns the singleton instance of Access.
     *
     * @return Access instance
     */
    public static Access getInstance() {
        return mInstance;
    }

    /**
     * Returns a valid access token, performing refresh or login if necessary.
     *
     * @return Access token string or null if unable to obtain
     */
    public @Nullable String getAccessToken() {
        String accessToken = null;

        // Check if current access token is valid
        if (!isAccessTokenValid()) {
            logger.debug("Trying to get new access token.");
            // If refresh token is also invalid, perform initial login
            if (!isRefreshTokenValid()) {
                logger.debug("Refresh token is invalid.");
                if (!doInitialLogin()) {
                    logger.warn("Login failed.");
                } else {
                    accessToken = mConfig.getAccessToken();
                }
            } else {
                // Try to refresh the access token
                logger.debug("Trying access token refresh.");
                if (!doTokenRefresh()) {
                    logger.warn("Access token refresh failed. Trying inital Login.");
                    if (!doInitialLogin()) {
                        logger.error("Login failed.");
                    } else {
                        accessToken = mConfig.getAccessToken();
                    }
                } else {
                    accessToken = mConfig.getAccessToken();
                }
            }
            // Save config if any changes occurred
            mConfig.save();
        } else {
            accessToken = mConfig.getAccessToken();
        }
        return accessToken;
    }

    /**
     * Checks if the current access token is valid (not expired and present).
     *
     * @return true if valid, false otherwise
     */
    private boolean isAccessTokenValid() {
        boolean isValid = false;
        String accessToken = mConfig.getAccessToken();
        // Access token must not be null or empty
        if (accessToken != null) {
            if (accessToken.length() > 0) {
                if (mConfig.getAccessTokenExpiration() > 0) {
                    if (mConfig.getAccessTokenTimestamp() > 0) {
                        long now = Instant.now().toEpochMilli();
                        long expireTimeStampMilli = mConfig.getAccessTokenTimestamp()
                                + (mConfig.getAccessTokenExpiration() * 1000l);
                        if (now < expireTimeStampMilli) {
                            isValid = true;
                        } else {
                            logger.debug("Access token timestamp has expired. Now: {} >= timestamp: {}",
                                    mConfig.getReadableTimestamp(now),
                                    mConfig.getReadableTimestamp(expireTimeStampMilli));
                        }
                    } else {
                        logger.debug("Access token timestamp is 0");
                    }
                } else {
                    logger.debug("Expires_in is 0");
                }
            } else {
                logger.debug("Access token length is >0");
            }
        } else {
            logger.debug("Access token is null");
        }
        return isValid;
    }

    /**
     * Checks if the current refresh token is valid (not null or empty).
     *
     * @return true if valid, false otherwise
     */
    private boolean isRefreshTokenValid() {
        boolean isValid = false;
        String refreshToken = mConfig.getRefreshToken();
        // Refresh token must not be null or empty
        if (refreshToken != null) {
            logger.debug("Refresh token is not null");
            if (refreshToken.length() > 0) {
                logger.debug("Refresh token length is >0");
                isValid = true;
            }
        }

        return isValid;
    }

    /**
     * Performs the initial login to obtain access and refresh tokens.
     *
     * @return true if login was successful, false otherwise
     */
    private boolean doInitialLogin() {
        boolean success = false;

        String username = mConfig.getUsername();
        String password = mConfig.getPassword();
        String clientID = mConfig.getClientID();
        String clientSecret = mConfig.getClientSecret();
        // TODO remove debug logging of credentials in production code
        logger.debug("Performing initial login with username: {}, password: {}, clientID: {}, clientSecret: {}",
                username, password, clientID, clientSecret);
        logger.debug("Performing initial login with username: {}, clientID: {}, clientSecret: {}", username, clientID,
                clientSecret);
        // Ensure all required fields are present
        if (username != null && password != null && clientID != null && clientSecret != null) {
            InitialLoginRequest loginReq = new InitialLoginRequest(clientID, clientSecret, username, password);
            // Send the login request and get the response
            GetPostResult response = comMgr.sendRequest(loginReq, false);

            if (response.isSuccess()) {
                InitialLoginResponse responseMsg = MsgFactory.getInstance().createMsg(response.getData(),
                        InitialLoginResponse.class);
                if (responseMsg != null) {
                    mConfig.setAccessToken(responseMsg.access_token);
                    mConfig.setRefreshToken(responseMsg.refresh_token);
                    mConfig.setAccessTokenExpiration(responseMsg.expires_in);
                    mConfig.setAccessTokenTimestampToNow();
                    mConfig.setExpireIn(responseMsg.expire_in);
                } else {
                    logger.error("Failed to parse initial login response.");
                    return false;
                }

                success = true;
            } else {
                logger.error("Initial login failed: {}", response);
            }
        } else {
            logger.error("Configuration is missing required fields for initial login.");
            return false;
        }
        return success;
    }

    /**
     * Refreshes the access token using the current refresh token.
     *
     * @return true if refresh was successful, false otherwise
     */
    private boolean doTokenRefresh() {

        boolean success = false;

        RefreshTokenRequest loginReq = new RefreshTokenRequest(mConfig.getRefreshToken(), mConfig.getClientID(),
                mConfig.getClientSecret());
        GetPostResult response = Communication.getInstance().sendRequest(loginReq, false);

        if (response.isSuccess()) {
            RefreshTokenResponse responseMsg = MsgFactory.getInstance().createMsg(response.getData(),
                    RefreshTokenResponse.class);
            if (responseMsg != null) {
                mConfig.setAccessToken(responseMsg.access_token);
                mConfig.setRefreshToken(responseMsg.refresh_token);
                mConfig.setAccessTokenExpiration(responseMsg.expires_in);
                mConfig.setAccessTokenTimestampToNow();
                mConfig.setExpireIn(responseMsg.expire_in);

                success = true;
            } else {
                logger.error("Failed to parse refresh token response.");
            }
        } else {
            logger.error("Token refresh failed: {}", response);
        }
        return success;
    }
}
