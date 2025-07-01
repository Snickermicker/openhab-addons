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
package org.openhab.binding.veluxactive.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link VeluxActiveAuthState} represents that steps in the VeluxActive PIN
 * authorization process.
 *
 * @author Volker Daube - Initial contribution
 */
@NonNullByDefault
enum VeluxActiveAuthState {

    /*
     * This state indicates that an VeluxActive PIN request was successful, and that a "token" API
     * call is needed to complete the authorization and get the refresh and access tokens. In
     * order to get the tokens, the user must authorize the application by entering the PIN
     * into the VeluxActive web portal.
     */
    NEED_REFRESH_TOKEN,

    /*
     * This state indicates that an VeluxActive PIN request was successful, and that a "token" API
     * call is needed to complete the authorization and get the refresh and access tokens. In
     * order to get the tokens, the user must authorize the application by entering the PIN
     * into the VeluxActive web portal.
     */
    NEED_ACCESS_TOKEN,

    /*
     * This state indicates that the "authorize" and "token" steps were successful.
     */
    COMPLETE
}
