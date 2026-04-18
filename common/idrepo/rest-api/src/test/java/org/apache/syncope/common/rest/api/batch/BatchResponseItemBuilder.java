/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.common.rest.api.batch;

import java.util.ArrayList;

public final class BatchResponseItemBuilder {
    private final BatchResponseItem item = new BatchResponseItem();
    private final StringBuilder content = new StringBuilder();

    public static BatchResponseItemBuilder builder() {
        return new BatchResponseItemBuilder();
    }

    private BatchResponseItemBuilder() {
    }

    public BatchResponseItemBuilder status(final int status) {
        item.setStatus(status);
        return this;
    }

    public BatchResponseItemBuilder header(final String key, final String value) {
        item.getHeaders().computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        return this;
    }

    public BatchResponseItemBuilder content(final String content) {
        this.content.append(content);
        return this;
    }

    public BatchResponseItemBuilder line(final String line) {
        return line(line, false);
    }

    public BatchResponseItemBuilder line(final String line, final boolean unixEol) {
        content.append(line);
        if (unixEol) {
            content.append("\n");
        } else {
            content.append("\r\n");
        }
        return this;
    }

    public BatchResponseItem create() {
        item.setContent(content.toString());
        return item;
    }
}
