package org.apache.syncope.common.rest.api.batch;

import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BatchPayloadParserCFTest {
    @Test
    public void testMissingCR() throws IOException {
        byte[] data = ("--bnd\r\n"
                + "some\n"
                + "    data\n"
                + "--bnd\n   "
                + " a bdiod\r\n"
                + "--bnd--").getBytes();
        List<BatchRequestItem> batches = BatchPayloadParser.parse(
                new ByteArrayInputStream(data),
                mediaType("bnd"),
                new BatchRequestItem());
        Assertions.assertEquals(2, batches.size());
    }

    @Test
    public void testRequestResponseMismatch() throws IOException {
        String boundary = "padded_boundary";
        byte[] payload = BatchPayloadBuilder.builder().boundary(boundary)
                .delimiter(5)
                .line()
                .line("Content-Type: application/http")
                .line("Content-Transfer-Encoding: binary")
                .line()
                .line("POST /users HTTP/1.1")
                .line("Accept: application/json")
                .closingDelimiter(10)
                .create();
        List<BatchResponseItem> items = BatchPayloadParser.parse(
                new ByteArrayInputStream(payload),
                mediaType(boundary),
                new BatchResponseItem());
        Assertions.assertEquals(1, items.size());
    }

    private MediaType mediaType(final String boundary) {
        MediaType t = mock(MediaType.class);
        when(t.getParameters()).thenReturn(Map.of("boundary", boundary));
        return t;
    }
}
