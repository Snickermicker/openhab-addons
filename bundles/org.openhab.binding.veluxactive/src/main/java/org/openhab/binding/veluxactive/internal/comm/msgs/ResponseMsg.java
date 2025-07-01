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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;

/**
 * @author Volker Daube - Initial contribution
 */
public class ResponseMsg extends Message {

    private transient @NonNull Map<String, List<String>> mHeaderFields = new HashMap<String, List<String>>();

    public ResponseMsg(Type messageType) {
        super(messageType);
    }

    public @NonNull Map<String, List<String>> getHeaderFields() {
        return mHeaderFields;
    }

    public void addHeaderFields(Map<String, List<String>> headerFields) {
        mHeaderFields = headerFields;
    }

    public void addHeaderField(String key, List<String> values) {
        mHeaderFields.put(key, values);
    }

    public void addHeaderField(String key, String value) {
        List<String> values;
        if (mHeaderFields.containsKey(key)) {
            values = mHeaderFields.get(key);
        } else {
            values = new LinkedList<String>();
            mHeaderFields.put(key, values);
        }

        values.add(value);
    }
}
