package org.apache.syncope.common.rest.api.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.junit.jupiter.api.Test;

public class BatchPayloadParserGeminiTest {

    private MediaType createMediaTypeWithBoundary(final String boundary) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(RESTHeaders.BOUNDARY_PARAMETER, boundary);
        return new MediaType("multipart", "mixed", parameters);
    }

    private InputStream createInputStream(final String payload) {
        return new ByteArrayInputStream(payload.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testParseSingleRequestItem() throws Exception {
        String boundary = "batch_boundary_123";
        String payload = "--" + boundary + "\r\n"
                + "Content-Type: application/http\r\n"
                + "Content-Transfer-Encoding: binary\r\n"
                + "\r\n"
                + "POST /users?active=true HTTP/1.1\r\n"
                + "Content-Type: application/json\r\n"
                + "Accept: application/json\r\n"
                + "\r\n"
                + "{\"username\":\"john.doe\"}\r\n"
                + "--" + boundary + "--\r\n";

        InputStream in = createInputStream(payload);
        MediaType mediaType = createMediaTypeWithBoundary(boundary);

        List<BatchRequestItem> items = BatchPayloadParser.parse(in, mediaType, new BatchRequestItem());

        assertNotNull(items);
        assertEquals(1, items.size());

        BatchRequestItem item = items.getFirst();
        assertEquals("POST", item.getMethod());
        assertEquals("/users", item.getRequestURI());
        assertEquals("active=true", item.getQueryString());

        // Assert Content without the trailing \r\n
        assertEquals("{\"username\":\"john.doe\"}", item.getContent());

        // Assert Headers
        assertTrue(item.getHeaders().containsKey("Content-Type"));
        assertEquals("application/json", item.getHeaders().get("Content-Type").getFirst());
    }

    @Test
    public void testParseSingleResponseItem() throws Exception {
        String boundary = "batch_boundary_456";
        String payload = "--" + boundary + "\r\n"
                + "Content-Type: application/http\r\n"
                + "\r\n"
                + "HTTP/1.1 201 Created\r\n"
                + "Content-Type: application/json\r\n"
                + "ETag: \"123456\"\r\n"
                + "\r\n"
                + "{\"id\":\"user123\"}\r\n"
                + "--" + boundary + "--\r\n";

        InputStream in = createInputStream(payload);
        MediaType mediaType = createMediaTypeWithBoundary(boundary);

        List<BatchResponseItem> items = BatchPayloadParser.parse(in, mediaType, new BatchResponseItem());

        assertNotNull(items);
        assertEquals(1, items.size());

        BatchResponseItem item = items.getFirst();
        assertEquals(201, item.getStatus());

        // Assert Content without the trailing \r\n
        assertEquals("{\"id\":\"user123\"}", item.getContent());

        // Assert Headers
        assertTrue(item.getHeaders().containsKey("ETag"));
        assertEquals("\"123456\"", item.getHeaders().get("ETag").getFirst());
    }

    @Test
    public void testParseMultipleItems() throws Exception {
        String boundary = "batch_boundary_789";
        String payload = "--" + boundary + "\r\n"
                + "Content-Type: application/http\r\n"
                + "\r\n"
                + "POST /users HTTP/1.1\r\n"
                + "\r\n"
                + "{\"name\":\"user1\"}\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Type: application/http\r\n"
                + "\r\n"
                + "PUT /users/user1 HTTP/1.1\r\n"
                + "\r\n"
                + "{\"name\":\"user1_updated\"}\r\n"
                + "--" + boundary + "--\r\n";

        InputStream in = createInputStream(payload);
        MediaType mediaType = createMediaTypeWithBoundary(boundary);

        List<BatchRequestItem> items = BatchPayloadParser.parse(in, mediaType, new BatchRequestItem());

        assertNotNull(items);
        assertEquals(2, items.size());

        assertEquals("POST", items.getFirst().getMethod());
        assertEquals("/users", items.getFirst().getRequestURI());
        assertNull(items.get(0).getQueryString());

        // Assert Content without the trailing \r\n
        assertEquals("{\"name\":\"user1\"}", items.get(0).getContent());

        assertEquals("PUT", items.get(1).getMethod());
        assertEquals("/users/user1", items.get(1).getRequestURI());

        // Assert Content without the trailing \r\n
        assertEquals("{\"name\":\"user1_updated\"}", items.get(1).getContent());
    }

    @Test
    public void testMissingCloseBoundaryDelimiterThrowsException() {
        String boundary = "batch_boundary_err";
        String payload = "--" + boundary + "\r\n"
                + "Content-Type: application/http\r\n"
                + "\r\n"
                + "GET /users HTTP/1.1\r\n"
                + "\r\n"
                + "Some content without terminating boundary delimiter...";

        InputStream in = createInputStream(payload);
        MediaType mediaType = createMediaTypeWithBoundary(boundary);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            BatchPayloadParser.parse(in, mediaType, new BatchRequestItem());
        });

        assertTrue(exception.getMessage().contains("Missing close boundary delimiter"));
    }

    @Test
    public void testEmptyPayloadThrowsException() {
        String boundary = "batch_boundary_empty";
        String payload = "";

        InputStream in = createInputStream(payload);
        MediaType mediaType = createMediaTypeWithBoundary(boundary);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            BatchPayloadParser.parse(in, mediaType, new BatchRequestItem());
        });

        assertTrue(exception.getMessage().contains("Missing close boundary delimiter"));
    }
}
