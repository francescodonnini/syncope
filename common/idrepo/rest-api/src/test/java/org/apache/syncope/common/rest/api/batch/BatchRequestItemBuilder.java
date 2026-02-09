package org.apache.syncope.common.rest.api.batch;

import java.util.ArrayList;

public class BatchRequestItemBuilder {
    private final BatchRequestItem item = new BatchRequestItem();
    private final StringBuilder content = new StringBuilder();

    public static BatchRequestItemBuilder builder() {
        return new BatchRequestItemBuilder();
    }

    private BatchRequestItemBuilder() {
    }

    public BatchRequestItemBuilder method(String method) {
        item.setMethod(method);
        return this;
    }

    public BatchRequestItemBuilder uri(String uri) {
        item.setRequestURI(uri);
        return this;
    }

    public BatchRequestItemBuilder query(String queryString) {
        item.setQueryString(queryString);
        return this;
    }

    public BatchRequestItemBuilder header(String key, String value) {
        item.getHeaders().computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        return this;
    }

    public BatchRequestItemBuilder content(String content) {
        item.setContent(content);
        return this;
    }

    public BatchRequestItemBuilder line(String line) {
        return line(line, false);
    }

    public BatchRequestItemBuilder line(String line, boolean unixEol) {
        content.append(line);
        if (unixEol) {
            content.append("\n");
        } else {
            content.append("\r\n");
        }
        return this;
    }

    public BatchRequestItem create() {
        if (!content.isEmpty()) {
            item.setContent(content.toString());
        }
        return item;
    }
}
