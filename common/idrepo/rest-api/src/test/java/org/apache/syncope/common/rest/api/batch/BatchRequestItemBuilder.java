package org.apache.syncope.common.rest.api.batch;

import java.util.ArrayList;

public final class BatchRequestItemBuilder {
    private final BatchRequestItem item = new BatchRequestItem();
    private final StringBuilder content = new StringBuilder();

    public static BatchRequestItemBuilder builder() {
        return new BatchRequestItemBuilder();
    }

    private BatchRequestItemBuilder() {
    }

    public BatchRequestItemBuilder method(final String method) {
        item.setMethod(method);
        return this;
    }

    public BatchRequestItemBuilder uri(final String uri) {
        item.setRequestURI(uri);
        return this;
    }

    public BatchRequestItemBuilder query(final String queryString) {
        item.setQueryString(queryString);
        return this;
    }

    public BatchRequestItemBuilder header(final String key, final String value) {
        item.getHeaders().computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        return this;
    }

    public BatchRequestItemBuilder content(final String content) {
        this.content.append(content);
        return this;
    }

    public BatchRequestItemBuilder line(final String line) {
        return line(line, false);
    }

    public BatchRequestItemBuilder line(final String line, final boolean unixEol) {
        content.append(line);
        if (unixEol) {
            content.append("\n");
        } else {
            content.append("\r\n");
        }
        return this;
    }

    public BatchRequestItem create() {
        item.setContent(content.toString());
        return item;
    }
}
