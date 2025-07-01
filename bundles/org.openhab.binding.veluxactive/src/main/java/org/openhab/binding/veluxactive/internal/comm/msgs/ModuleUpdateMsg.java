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
package org.openhab.binding.veluxactive.internal.comm.msgs;

import java.time.Instant;
import java.time.ZoneId;

import org.openhab.binding.veluxactive.internal.dto.BasicDeviceModule;

/**
 * @author Volker Daube - Initial contribution
 */
public class ModuleUpdateMsg extends ResponseMsg {

    private String type;
    private String push_type;
    private ExtraParams extra_params;
    private String app_type;
    private String app_version;
    private String device_version;
    private String os_version;
    private String user_id;
    private int ts_generated;
    private String _id;
    private Timestamp timestamp;

    public ModuleUpdateMsg() {
        super(Type.MODULE_UPDATE);
    }

    public class ExtraParams {
        private Home home;
        private long timestamp;

        public Home getHome() {
            return home;
        }

        public String getTimestamp() {
            Instant instant = Instant.ofEpochSecond(timestamp);
            return instant.atZone(ZoneId.systemDefault()).toString();
        }

        public long getTimestampSeconds() {
            return timestamp;
        }

        public class Home {
            private String id;
            private BasicDeviceModule[] modules;

            public String getID() {
                return id;
            }

            public BasicDeviceModule[] getModules() {
                return modules;
            }
        }
    }

    public class Timestamp {
        private int sec;
        private int usec;

        public int getSec() {
            return sec;
        }

        public int getUsec() {
            return usec;
        }
    }

    public String getType() {
        return type;
    }

    public String getPushType() {
        return push_type;
    }

    public ExtraParams getExtraParams() {
        return extra_params;
    }

    public String getAppType() {
        return app_type;
    }

    public String getAppVersion() {
        return app_version;
    }

    public String getDeviceVersion() {
        return device_version;
    }

    public String getOsVersion() {
        return os_version;
    }

    public String getUserID() {
        return user_id;
    }

    public int getTsGenerated() {
        return ts_generated;
    }

    public String getID() {
        return _id;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }
}
