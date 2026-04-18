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
