package org.apache.syncope.common.rest.api.batch;

import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

public class BatchPayloadParserTest {
    private static final String line = "\r\n";
    private static final String MULTIPART_MIXED = "multipart/mixed";
    private static final String BCHARNOSPACES =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'()+_,-./:=?";

    private static final String WHITESPACES = " \t";

    private static final String BOUNDARY_CHARSET = BCHARNOSPACES + WHITESPACES;

    private static Random random;

    @BeforeAll
    public static void setUp() {
        random = new Random(123456789);
    }

    @Test
    public void testEmptyBodyAndNoEndDelimiter() throws IOException {
        String boundary = boundary(1);
        String delimiter = "\r\n--" + boundary;
        System.out.println(delimiter);
        byte[] s = new StringBuilder()
                .append("POST /batch HTTP/1.1")
                .append(line)
                .append("Content-Type: multipart/mixed;boundary=").append(boundary)
                .append(line)
                .append("This is a preamble.")
                .toString()
                .getBytes(StandardCharsets.US_ASCII);
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> BatchPayloadParser.parse(new ByteArrayInputStream(s), MediaType.valueOf("multipart/mixed;boundary=%s".formatted(boundary)), new BatchRequestItem()));
    }

    @Test
    public void testEmptyBody() throws IOException {
        String boundary = boundary(1);
        String delimiter = "\r\n--" + boundary;
        System.out.println(delimiter);
        byte[] s = new StringBuilder()
                .append("POST /batch HTTP/1.1")
                .append(line)
                .append("Content-Type: multipart/mixed;boundary=").append(boundary)
                .append(line)
                .append("This is a preamble.")
                .append(line)
                .append(delimiter).append("--")
                .toString()
                .getBytes(StandardCharsets.US_ASCII);
        List<BatchRequestItem> items = BatchPayloadParser.parse(new ByteArrayInputStream(s), MediaType.valueOf("multipart/mixed;boundary=%s".formatted(boundary)), new BatchRequestItem());
        Assertions.assertEquals(0, items.size());
    }

    @Test
    public void testEmptyBodyWithEpilogue() throws IOException {
        String boundary = boundary(1);
        String delimiter = "\r\n--" + boundary;
        System.out.println(delimiter);
        byte[] s = new StringBuilder()
                .append("POST /batch HTTP/1.1")
                .append(line)
                .append("Content-Type: multipart/mixed;boundary=").append(boundary)
                .append(line)
                .append(delimiter).append("--")
                .append(line)
                .append("This is an epilogue")
                .toString()
                .getBytes(StandardCharsets.US_ASCII);
        List<BatchRequestItem> items = BatchPayloadParser.parse(new ByteArrayInputStream(s), MediaType.valueOf("multipart/mixed;boundary=%s".formatted(boundary)), new BatchRequestItem());
        Assertions.assertEquals(0, items.size());
    }

    @Test
    public void testEmptyBodyWithEpilogueNoCRLF() throws IOException {
        String boundary = boundary(1);
        String delimiter = "\r\n--" + boundary;
        System.out.println(delimiter);
        byte[] s = new StringBuilder()
                .append("POST /batch HTTP/1.1")
                .append(line)
                .append("Content-Type: multipart/mixed;boundary=").append(boundary)
                .append(line)
                .append(delimiter).append("--")
                .append("This is another epilogue")
                .toString()
                .getBytes(StandardCharsets.US_ASCII);
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> BatchPayloadParser.parse(new ByteArrayInputStream(s), MediaType.valueOf("multipart/mixed;boundary=%s".formatted(boundary)), new BatchRequestItem()));
    }

    @Test
    public void test2() throws IOException {
        String boundary = boundary(0);
        String delimiter = "\r\n--" + boundary;
        System.out.println(delimiter);
        byte[] s = new StringBuilder()
                .append("POST /batch HTTP/1.1")
                .append(line)
                .append("Content-Type: multipart/mixed;boundary=").append(delimiter)
                .append(line)
                .append("Content-Type: application/http").append(line)
                .append("Content-Transfer-Encoding: binary").append(line)
                .append(line)
                .append("POST /users HTTP/1.1").append(line)
                .append("Accept: application/json").append(line)
                .append("Content-Length: 2").append(line)
                .append("Content-Type: application/json").append(line)
                .append(line)
                .append("{}")
                .append(line)
                .append("EPILOGUE")
                .toString()
                .getBytes(StandardCharsets.US_ASCII);
        List<BatchRequestItem> items = BatchPayloadParser.parse(new ByteArrayInputStream(s), MediaType.valueOf("multipart/mixed;boundary=%s".formatted(boundary)), new BatchRequestItem());
        System.out.println(items.size());
    }

    private String boundary(int boundaryLength) {
        if (boundaryLength < 0) {
            throw new IllegalArgumentException("Invalid boundary length");
        }
        if (boundaryLength == 0) {
            return "";
        }
        StringBuilder boundary = new StringBuilder();
        for (int i = 0; i < boundaryLength - 1; i++) {
            boundary.append(BOUNDARY_CHARSET.charAt(random.nextInt(BOUNDARY_CHARSET.length())));
        }
        boundary.append(BCHARNOSPACES.charAt(random.nextInt(BCHARNOSPACES.length())));
        return boundary.toString();
    }
}
