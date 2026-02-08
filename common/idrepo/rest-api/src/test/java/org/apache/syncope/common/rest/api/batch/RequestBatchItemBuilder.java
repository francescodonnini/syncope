package org.apache.syncope.common.rest.api.batch;

import java.util.ArrayList;

public class RequestBatchItemBuilder {
    private final BatchRequestItem item = new BatchRequestItem();
    private final StringBuilder content = new StringBuilder();

    public static RequestBatchItemBuilder builder() {
        return new RequestBatchItemBuilder();
    }

    private RequestBatchItemBuilder() {
    }

    public RequestBatchItemBuilder method(String method) {
        item.setMethod(method);
        return this;
    }

    public RequestBatchItemBuilder uri(String uri) {
        item.setRequestURI(uri);
        return this;
    }

    public RequestBatchItemBuilder query(String queryString) {
        item.setQueryString(queryString);
        return this;
    }

    public RequestBatchItemBuilder header(String key, String value) {
        item.getHeaders().computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        return this;
    }

    public RequestBatchItemBuilder content(String content) {
        item.setContent(content);
        return this;
    }

    public RequestBatchItemBuilder line(String line) {
        return line(line, false);
    }

    public RequestBatchItemBuilder line(String line, boolean unixEol) {
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
