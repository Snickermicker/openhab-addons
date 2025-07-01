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
package org.openhab.binding.veluxactive.internal.comm;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.veluxactive.internal.comm.msgs.GetRequestMsg;
import org.openhab.binding.veluxactive.internal.comm.msgs.PostRequestMsg;
import org.openhab.binding.veluxactive.internal.comm.msgs.RequestMsg;
import org.openhab.binding.veluxactive.internal.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * Handles HTTP communication (GET and POST requests) for the VeluxActive system.
 * Implements singleton pattern.
 *
 * @author Volker Daube - Initial contribution
 */

public class Communication {

    private static Communication mInstance = null;
    private static Config config = Config.getInstance();

    private static final Logger logger = LoggerFactory.getLogger(Communication.class);

    /**
     * Returns the singleton instance of Communication.
     */
    public static @NonNull Communication getInstance() {
        if (mInstance == null) {
            synchronized (Communication.class) {
                if (mInstance == null) {
                    mInstance = new Communication();
                }
            }
        }
        return mInstance;
    }

    /**
     * Sends a GET request using the provided GetRequestMsg.
     * Handles HTTP redirects and proxy configuration.
     *
     * @param msg The GET request message.
     * @return The result of the GET request.
     */
    public @NonNull GetPostResult sendRequest(@NonNull RequestMsg msg, boolean sendInJsonFormat) {
        GetPostResult result;
        do {
            result = new GetPostResult();
            logger.debug("Sending: {}", msg);

            try {
                URI uri = URI.create(msg.getBaseUrl());
                URL url = uri.toURL();
                Proxy proxy;
                // Use proxy if configured
                if (config.useProxy()) {
                    logger.debug("Using proxy to send request.");
                    proxy = new Proxy(Proxy.Type.HTTP,
                            new InetSocketAddress(config.getProxyIP(), config.getProxyPort()));
                } else {
                    proxy = Proxy.NO_PROXY;
                }

                HttpURLConnection conn = (HttpURLConnection) url.openConnection(proxy);
                conn.setUseCaches(false);
                conn.setInstanceFollowRedirects(false);
                conn.setRequestProperty("charset", "utf-8");

                // Set additional request properties
                for (Map.Entry<String, String> entry : msg.getRequestProperties().entrySet()) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }

                if (msg instanceof GetRequestMsg) {
                    conn.setRequestMethod("GET");
                    conn.setDoOutput(false);
                } else if (msg instanceof PostRequestMsg) {
                    PostRequestMsg postMsg = (PostRequestMsg) msg;
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    byte[] postData = postMsg.getPostData(sendInJsonFormat);
                    conn.setRequestProperty("Content-Length", Integer.toString(postData.length));

                    // Set content type based on format
                    if (sendInJsonFormat) {
                        conn.setRequestProperty("Content-Type", "application/json");
                    } else {
                        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    }

                    // Write POST data
                    DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
                    wr.write(postData);
                    wr.flush();
                    wr.close();

                } else {
                    logger.error("Unsupported request message type: {}", msg.getClass().getName());
                    return result; // Return empty result for unsupported types
                }

                result.setHttpResponseCode(conn.getResponseCode());
                switch (result.mHttpResponseCode) {
                    case HttpURLConnection.HTTP_MOVED_PERM:
                    case HttpURLConnection.HTTP_MOVED_TEMP:
                        result.setHeaderFields(conn.getHeaderFields());
                        List<String> newURLStringList = result.getHeaderFields().get("Location");
                        logger.debug("Redirected: {}", newURLStringList.get(0));
                        msg.setBaseUrl(newURLStringList.get(0));
                        break;
                    case HttpURLConnection.HTTP_OK:
                        InputStream re = conn.getInputStream();
                        result.setData(re.readAllBytes());
                        result.setHeaderFields(conn.getHeaderFields());
                        result.setSuccess(true);
                        break;
                    case 426:
                        result.setHeaderFields(conn.getHeaderFields());
                        result.setSuccess(false);
                        break;
                    default:
                        result.setSuccess(false);
                        logger.error("Failed with response code: {}", result.getHttpResponseCode());
                        break;
                }
                conn.disconnect();
            } catch (MalformedURLException e) {
                result.setSuccess(false);
                logger.error("Exception: {}", e.getMessage());
            } catch (ProtocolException e) {
                result.setSuccess(false);
                logger.error("Exception: {}", e.getMessage());
            } catch (IOException e) {
                result.setSuccess(false);
                logger.error("Exception: {}", e.getMessage());
            }

        } while (result.mHttpResponseCode == HttpURLConnection.HTTP_MOVED_PERM);
        return result;
    }

    // /**
    // * Sends a GET request using the provided GetRequestMsg.
    // * Handles HTTP redirects and proxy configuration.
    // *
    // * @param msg The GET request message.
    // * @return The result of the GET request.
    // */
    // public @NonNull GetPostResult sendGetRequest(@NonNull GetRequestMsg msg) {
    // GetPostResult result = null;
    //
    // do {
    // result = new GetPostResult();
    // try {
    // logger.debug("Sending: {}", msg);
    // URI uri = URI.create(msg.getBaseUrl());
    // URL url = uri.toURL();
    // Proxy proxy;
    // // Use proxy if configured
    // if (config.useProxy()) {
    // logger.debug("Using proxy to send GET request.");
    // proxy = new Proxy(Proxy.Type.HTTP,
    // new InetSocketAddress(config.getProxyIP(), config.getProxyPort()));
    // } else {
    // proxy = Proxy.NO_PROXY;
    // }
    //
    // HttpURLConnection conn;
    // conn = (HttpURLConnection) url.openConnection(proxy);
    // conn.setDoOutput(false);
    // conn.setUseCaches(false);
    // conn.setInstanceFollowRedirects(false);
    // conn.setRequestMethod("GET");
    // conn.setRequestProperty("charset", "utf-8");
    //
    // // Set additional request properties
    // for (Map.Entry<String, String> entry : msg.getRequestProperties().entrySet()) {
    // conn.setRequestProperty(entry.getKey(), entry.getValue());
    // }
    // result.setHttpResponseCode(conn.getResponseCode());
    //
    // // Handle HTTP response codes
    // switch (result.mHttpResponseCode) {
    // case HttpURLConnection.HTTP_MOVED_PERM:
    // case HttpURLConnection.HTTP_MOVED_TEMP:
    // result.setHeaderFields(conn.getHeaderFields());
    // List<String> newURLStringList = result.getHeaderFields().get("Location");
    // logger.debug("Redirected: {}", newURLStringList.get(0));
    // msg.setBaseUrl(newURLStringList.get(0));
    // break;
    // case HttpURLConnection.HTTP_OK:
    // InputStream re = conn.getInputStream();
    // result.setData(re.readAllBytes());
    // result.setHeaderFields(conn.getHeaderFields());
    // result.setSuccess(true);
    // break;
    // case 426:
    // result.setHeaderFields(conn.getHeaderFields());
    // result.setSuccess(false);
    // break;
    // default:
    // result.setSuccess(false);
    // logger.error("Failed with response code: {}", result.getHttpResponseCode());
    // break;
    // }
    // conn.disconnect();
    //
    // } catch (MalformedURLException e) {
    // result.setSuccess(false);
    // logger.error("Exception: {}", e.getMessage());
    // } catch (ProtocolException e) {
    // result.setSuccess(false);
    // logger.error("Exception: {}", e.getMessage());
    // } catch (IOException e) {
    // result.setSuccess(false);
    // logger.error("Exception: {}", e.getMessage());
    // }
    // } while (result.mHttpResponseCode == HttpURLConnection.HTTP_MOVED_PERM);
    // return result;
    // }
    //
    // /**
    // * Sends a POST request using the provided PostRequestMsg.
    // * Supports sending data as JSON or form-urlencoded.
    // *
    // * @param msg The POST request message.
    // * @param sendInJsonFormat Whether to send data as JSON.
    // * @return The result of the POST request.
    // */
    // public @NonNull GetPostResult sendPostRequest(@NonNull PostRequestMsg msg, boolean sendInJsonFormat) {
    // GetPostResult result = new GetPostResult();
    //
    // byte[] postData = msg.getPostData(sendInJsonFormat);
    //
    // try {
    // URI uri = URI.create(msg.getBaseUrl());
    // URL url = uri.toURL();
    // Proxy proxy;
    // // Use proxy if configured
    // if (config.useProxy()) {
    // logger.debug("Using proxy to send GET request.");
    // proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(config.getProxyIP(), config.getProxyPort()));
    // } else {
    // proxy = Proxy.NO_PROXY;
    // }
    //
    // HttpURLConnection conn = (HttpURLConnection) url.openConnection(proxy);
    //
    // conn.setDoOutput(true);
    // conn.setInstanceFollowRedirects(false);
    // conn.setRequestMethod("POST");
    // // Set additional request properties
    // for (Map.Entry<String, String> entry : msg.getRequestProperties().entrySet()) {
    // conn.setRequestProperty(entry.getKey(), entry.getValue());
    // }
    //
    // // Set content type based on format
    // if (sendInJsonFormat) {
    // conn.setRequestProperty("Content-Type", "application/json");
    // } else {
    // conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
    // }
    // conn.setRequestProperty("charset", "utf-8");
    // conn.setRequestProperty("Content-Length", Integer.toString(postData.length));
    // conn.setUseCaches(false);
    //
    // // Write POST data
    // DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
    // wr.write(postData);
    // wr.flush();
    // wr.close();
    //
    // result.setHttpResponseCode(conn.getResponseCode());
    // if (result.mHttpResponseCode == HttpURLConnection.HTTP_OK) {
    // InputStream re = conn.getInputStream();
    // result.setData(re.readAllBytes());
    // conn.disconnect();
    // result.setHeaderFields(conn.getHeaderFields());
    // result.setSuccess(true);
    // } else {
    // conn.disconnect();
    // result.setSuccess(false);
    // logger.error("Failed with response code: {}", result.getHttpResponseCode());
    // }
    //
    // } catch (MalformedURLException e) {
    // result.setSuccess(false);
    // logger.error("Exception: {}", e.getMessage());
    // } catch (ProtocolException e) {
    // result.setSuccess(false);
    // logger.error("Exception: {}", e.getMessage());
    // } catch (IOException e) {
    // result.setSuccess(false);
    // logger.error("Exception: {}", e.getMessage());
    // }
    //
    // return result;
    // }

    /**
     * Encapsulates the result of a GET or POST HTTP request.
     */
    public class GetPostResult {
        private boolean mSuccess = false;
        private int mHttpResponseCode = 0;
        private byte @Nullable [] mData = null;
        private @NonNull Map<String, List<String>> mHeaderFields = new HashMap<String, List<String>>();

        public GetPostResult() {
        }

        public GetPostResult(boolean success, int httpResponseCode, byte @Nullable [] data) {
            this.mSuccess = success;
            this.mHttpResponseCode = httpResponseCode;
            this.mData = data;
        }

        public boolean isSuccess() {
            return mSuccess;
        }

        public int getHttpResponseCode() {
            return mHttpResponseCode;
        }

        public byte[] getData() {
            return mData;
        }

        public void setSuccess(boolean success) {
            this.mSuccess = success;
        }

        public void setHttpResponseCode(int httpResponseCode) {
            this.mHttpResponseCode = httpResponseCode;
        }

        public void setData(byte[] data) {
            this.mData = data;
        }

        public @NonNull Map<String, List<String>> getHeaderFields() {
            return mHeaderFields;
        }

        public void setHeaderFields(Map<String, List<String>> headerFields) {
            mHeaderFields = headerFields;
        }

        /**
         * Returns a JSON representation of the result for logging/debugging.
         */
        @Override
        public String toString() {
            Gson gson = new Gson();
            return this.getClass().getName() + ": " + gson.toJson(this);
        }
    }
}
