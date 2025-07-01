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
package org.openhab.binding.veluxactive.internal.comm.websocket;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.api.WebSocketPingPongListener;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.openhab.binding.veluxactive.internal.access.Access;
import org.openhab.binding.veluxactive.internal.comm.msgs.Message;
import org.openhab.binding.veluxactive.internal.comm.msgs.MsgFactory;
import org.openhab.binding.veluxactive.internal.comm.msgs.StatusMsg;
import org.openhab.binding.veluxactive.internal.comm.msgs.SubscribeMsg;
import org.openhab.binding.veluxactive.internal.config.Config;
import org.openhab.core.common.ThreadPoolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WebSocketClient manages a WebSocket connection using eclipse jetty WebSocket API.
 * Handles connection, disconnection, message sending/receiving, and notifies registered handlers.
 *
 * @author Volker Daube - Initial contribution
 */
@NonNullByDefault
public class WebSocketClientHandler {
    // Logger for debugging and error reporting
    private static final Logger logger = LoggerFactory.getLogger(WebSocketClientHandler.class);
    // Configuration and access token providers
    private Config mConfig = Config.getInstance();
    private Access mAccess = Access.getInstance();
    // Flag to indicate if disconnect was requested by user
    private boolean mShouldDisconnect = false;
    private ClientEndPoint mClientEndPoint = new ClientEndPoint();
    private WebSocketClient mWebSocketClient;
    private HttpClient mHttpClient;

    public WebSocketClientHandler(HttpClient httpClient) {
        // Initialize the WebSocket client with the provided HttpClient}
        mHttpClient = httpClient;
        // Creat WebSocketClient.
        mWebSocketClient = new WebSocketClient(mHttpClient);
    }

    /**
     * Connects to the WebSocket server at the given URI.
     * Handles proxy configuration if enabled.
     *
     * @param serverURI The URI of the WebSocket server
     * @return true if connection was successful, false otherwise
     */
    public boolean connect(URI serverURI) {
        mShouldDisconnect = false;
        boolean successfullyConnected = false;

        logger.debug("Trying to connect to websocket: {}", serverURI.toString());

        try {
            // Start WebSocketClient.
            mWebSocketClient.start();
            // Connect the client EndPoint to the server.
            mWebSocketClient.connect(mClientEndPoint, serverURI);

            logger.debug("Websocket started, waiting for connection to be established...");
            successfullyConnected = true;
        } catch (Exception e) {
            logger.error("Error connecting to websocket: {}", e.getMessage());

        }

        return successfullyConnected;
    }

    /**
     * Disconnects from the WebSocket server.
     * Closes the current session if it exists.
     */
    public void disconnect() {
        logger.debug("Trying to disconnect");
        mShouldDisconnect = true;
        logger.debug("Closing websocket");
        // Stop WebSocketClient.
        // Use LifeCycle.stop(...) to rethrow checked exceptions as unchecked.
        new Thread(() -> LifeCycle.stop(mWebSocketClient)).start();
    }

    private class ClientEndPoint implements WebSocketListener, WebSocketPingPongListener {
        private @Nullable Session session = null;
        // List of registered message handlers
        private List<MessageHandlerInterface> mMessageHandlerList = new ArrayList<>();
        // Logger for debugging and error reporting
        private static final Logger logger = LoggerFactory.getLogger(ClientEndPoint.class);
        private boolean mSubscribeMsgSent = false;
        private @Nullable Future<?> keepAliveJob;
        private static final String WEBSOCKET_HANDLER_THREADPOOL_NAME = "veluxactive_websocket_handler";
        private final ScheduledExecutorService scheduler = ThreadPoolManager
                .getScheduledPool(WEBSOCKET_HANDLER_THREADPOOL_NAME);

        @Override
        public void onWebSocketConnect(@Nullable Session session) {
            logger.debug("Websocket connected");
            if (session == null) {
                logger.error("WebSocket session is null");
                return;
            }

            this.session = session;
            List<MessageHandlerInterface> handlerCopyList;

            synchronized (this) {
                handlerCopyList = this.mMessageHandlerList;
            }
            handlerCopyList.forEach(handler -> {
                try {
                    handler.webSocketConnected();
                } catch (Exception e) {
                    logger.error("Error handling websocket message: {}", e.getMessage());
                }
            });

            // Send subscribe message after connection is established
            SubscribeMsg subscribeMsg = new SubscribeMsg(mAccess.getAccessToken(), mConfig.getAppVersion());
            logger.debug("Sending subscribe message.");
            sendMessage(subscribeMsg.toJson());
            mSubscribeMsgSent = true;
        }

        @Override
        public void onWebSocketText(@Nullable String messageString) {
            if (messageString == null || messageString.isEmpty()) {
                logger.warn("Received empty or null message from WebSocket");
                return;
            }
            logger.debug("Received websocket text: {}", messageString);
            // De-serialize the incoming message
            Message message = deserializeMessage(messageString);
            if (message == null) {
                logger.error("Received null message after deserialization. Message: {}", messageString);
                return;
            }

            if (mSubscribeMsgSent) {
                // Consume status message from the server for subscription
                if (message.getMsgType() == Message.Type.STATUS) {
                    StatusMsg statusMessage = (StatusMsg) message;
                    if (statusMessage.isSuccess()) {
                        logger.debug("Subscription successful, status: {}", statusMessage.getStatus());
                        mSubscribeMsgSent = false;

                        startKeepingAlive();
                    } else {
                        logger.error("Subscription failed with status: {}", statusMessage.getStatus());
                        mSubscribeMsgSent = false;
                    }
                }
            } else {
                List<MessageHandlerInterface> handlerCopyList;
                synchronized (this) {
                    handlerCopyList = this.mMessageHandlerList;
                }
                handlerCopyList.forEach(handler -> {
                    handler.handleWebsocketMessage(message);
                });
            }
        }

        @Override
        public void onWebSocketClose(int statusCode, @Nullable String reason) {
            List<MessageHandlerInterface> handlerCopyList;

            logger.debug("websocket closed with status code: {}, reason: {}", statusCode, reason);
            if (keepAliveJob != null) {
                keepAliveJob.cancel(true);
                keepAliveJob = null;
            }

            synchronized (this) {
                handlerCopyList = this.mMessageHandlerList;
            }
            handlerCopyList.forEach(handler -> {
                try {
                    handler.webSocketDisconnected(mShouldDisconnect, reason);
                } catch (Exception e) {
                    logger.error("Error handling websocket message: {}", e.getMessage());
                }
            });

            this.session = null;
        }

        @Override
        public void onWebSocketError(@Nullable Throwable cause) {
            if (cause == null) {
                logger.warn("WebSocket error occurred, but cause is null");
                return;
            }
        }

        @Override
        public void onWebSocketBinary(byte @Nullable [] payload, int offset, int len) {
            if (payload != null) {
                logger.warn("Unexpected WebSocket binary message received.");
                return;
            }
        }

        @Override
        public void onWebSocketPing(@Nullable ByteBuffer payload) {
            logger.debug("Received WebSocket ping");

            sendPong(payload); // Respond with a pong message
        }

        @Override
        public void onWebSocketPong(@Nullable ByteBuffer payload) {
            if (payload == null || payload.remaining() < 8) {
                logger.warn("Received invalid WebSocket pong message");
            } else {
                // The remote peer echoed back the local nanoTime.
                long start = payload.getLong();

                // Calculate the round-trip time.
                long roundTrip = System.nanoTime() - start;
                logger.debug("Received WebSocket pong round-trip time: {} ns", roundTrip);
            }
        }

        /**
         * Sends a text message over the WebSocket connection.
         *
         * @param message The message to send
         */
        public void sendMessage(String message) {
            // Copy the session reference to minimize lock duration
            Session sessionCopy;
            synchronized (this) {
                sessionCopy = this.session;
            }
            if (sessionCopy == null) {
                logger.error("User session is null");
                return;
            }
            try {
                sessionCopy.getRemote().sendString(message);
            } catch (IOException e) {
                logger.error("Error sending message: {}", e.getMessage());
            }
        }

        private void startKeepingAlive() {
            logger.debug("WebSocketClientHandler: Scheduling keep alive job");
            keepAliveJob = scheduler.scheduleWithFixedDelay(this::sendPing, 0, 60, TimeUnit.SECONDS);
        }

        private void sendPing() {
            // Send to the remote peer the local nanoTime.
            ByteBuffer buffer = ByteBuffer.allocate(8).putLong(System.nanoTime()).flip();
            // Copy the session reference to minimize lock duration
            Session sessionCopy;
            synchronized (this) {
                sessionCopy = this.session;
            }
            if (sessionCopy == null) {
                logger.error("User session is null");
                return;
            }
            try {
                logger.debug("Sending Websocket ping to remote peer");
                sessionCopy.getRemote().sendPing(buffer);
            } catch (IOException e) {
                logger.error("Error sending Websocket ping: {}", e.getMessage());
            }
        }

        private void sendPong(@Nullable ByteBuffer payload) {
            // Copy the session reference to minimize lock duration
            Session sessionCopy;
            synchronized (this) {
                sessionCopy = this.session;
            }
            if (sessionCopy == null) {
                logger.error("User session is null");
                return;
            }
            try {
                logger.debug("Sending Websocket pong to remote peer");
                sessionCopy.getRemote().sendPong(payload);
            } catch (IOException e) {
                logger.error("Error sending Websocket pong: {}", e.getMessage());
            }
        }

        /**
         * De-serializes a JSON message string into a Message object using MsgFactory.
         *
         * @param jsonMessage The JSON message string
         * @return The de-serialized Message object, or null if de-serialization fails
         */
        private @Nullable Message deserializeMessage(String jsonMessage) {
            Message message = MsgFactory.getInstance().createMsg(jsonMessage);
            if (message == null) {
                logger.debug("Received null message from MsgFactory. Message: {}", jsonMessage);
            }
            return message;
        }

        /**
         * Registers a new message handler to receive WebSocket events.
         *
         * @param msgHandler The handler to register
         */
        public synchronized void addMessageHandler(MessageHandlerInterface msgHandler) {
            this.mMessageHandlerList.add(msgHandler);
        }

        /**
         * Interface for handling WebSocket events and messages.
         */
        public static interface MessageHandlerInterface {
            public void handleWebsocketMessage(Message message);

            public void webSocketDisconnected(boolean shouldDisconnect, @Nullable String reason);

            public void webSocketConnected();
        }
    }
}
