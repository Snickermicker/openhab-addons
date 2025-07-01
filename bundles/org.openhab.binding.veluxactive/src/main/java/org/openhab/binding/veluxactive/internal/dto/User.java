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
public class User extends ElementWithID {
    private String email;
    private String language;
    private String locale;
    private String country;
    private int feel_like_algorithm;
    private int unit_pressure;
    private int unit_system;
    private int unit_wind;
    private boolean all_linked;
    private String type;
    private boolean app_telemetry;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public int getFeelLikeAlgorithm() {
        return feel_like_algorithm;
    }

    public void setFeelLikeAlgorithm(int feelLikeAlgorithm) {
        this.feel_like_algorithm = feelLikeAlgorithm;
    }

    public int getPressureUnit() {
        return unit_pressure;
    }

    public void setPressureUnit(int pressureUnit) {
        this.unit_pressure = pressureUnit;
    }

    public int getUnitSystem() {
        return unit_system;
    }

    public void setUnitSystem(int unitSystem) {
        this.unit_system = unitSystem;
    }

    public int getWindUnit() {
        return unit_wind;
    }

    public void setWindUnit(int windUnit) {
        this.unit_wind = windUnit;
    }

    public boolean isAllLinked() {
        return all_linked;
    }

    public void setAllLinked(boolean allLinked) {
        this.all_linked = allLinked;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isAppTelemetry() {
        return app_telemetry;
    }

    public void setAppTelemetry(boolean appTelemetry) {
        this.app_telemetry = appTelemetry;
    }
}
