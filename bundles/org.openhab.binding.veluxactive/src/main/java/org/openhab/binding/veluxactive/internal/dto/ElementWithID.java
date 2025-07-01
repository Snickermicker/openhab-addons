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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The{@link ElementWithID} is used by elements that carry an ID.
 *
 * @author Volker Daube - Initial contribution
 */
@NonNullByDefault
public abstract class ElementWithID {
    private @Nullable String id;

    public @Nullable String getID() {
        return id;
    }

    public void setID(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "ElementWithID [id=" + id + ", converted=" + getID() + "]";
    }
}
