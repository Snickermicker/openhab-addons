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
package org.openhab.binding.veluxactive.internal.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link ExceptionUtils} class defines static Exception related methods
 *
 * @author Volker Daube - Initial contribution
 */
@NonNullByDefault
public class ExceptionUtils {

    public static Throwable getRootThrowable(Throwable throwable) {
        Throwable newThrowable = throwable;
        List<Throwable> list = new ArrayList<>();
        while (!list.contains(throwable)) {
            list.add(throwable);
            Throwable throwableLocal = throwable.getCause();
            if (throwableLocal != null) {
                newThrowable = throwableLocal;
            }
        }
        return newThrowable;
    }
}
