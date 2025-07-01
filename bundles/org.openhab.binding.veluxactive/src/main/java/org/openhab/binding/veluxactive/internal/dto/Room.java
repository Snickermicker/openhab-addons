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
package org.openhab.binding.veluxactive.internal.dto;

/**
 * @author Volker Daube - Initial contribution
 */
public class Room extends ElementWithID {
    private String name;
    private String type;
    private String[] module_ids;
    private String[] modules;
    private int algo_status;
    private int auto_close_ts;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String[] getModule_ids() {
        return module_ids;
    }

    public void setModuleIDs(String[] moduleIDs) {
        this.module_ids = moduleIDs;
    }

    public String[] getModules() {
        return modules;
    }

    public void setModules(String[] modules) {
        this.modules = modules;
    }

    public int getAlgoStatus() {
        return algo_status;
    }

    public void setAlgoStatus(int algoStatus) {
        this.algo_status = algoStatus;
    }

    public int getAutoCloseTs() {
        return auto_close_ts;
    }

    public void setAutoCloseTs(int autoCloseTs) {
        this.auto_close_ts = autoCloseTs;
    }
}
