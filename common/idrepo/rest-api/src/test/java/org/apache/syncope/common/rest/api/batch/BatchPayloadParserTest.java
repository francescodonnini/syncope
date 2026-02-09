package org.apache.syncope.common.rest.api.batch;

import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BatchPayloadParserTest {
    // The first batch of tests focuses on the parser extrapolating the correct number of
    // parts:

    // only the preamble is present, expecting 0 parts
    private static final BatchPayloadBuilder PART_00 = BatchPayloadBuilder.builder()
            .boundary("b")
            .line("POST /batch HTTP/1.1")
            .line()
            .line("Content-Type: multipart/mixed; boundary=b")
            .line()
            .line("This is a preamble.");

    // only the closing delimiter is present, expecting an error, BUT
    // the parser seems to treat the whole input as a preamble. RFC 2046
    // expects at least a part to be present in the payload of a batch request,
    // so a compliant parser should reject that input.
    private static final BatchPayloadBuilder PART_01 = BatchPayloadBuilder.builder()
            .boundary("bo")
            .text("--bo--");

    // missing closing delimiter, expecting IllegalArgumentException
    private static final BatchPayloadBuilder PART_02 = BatchPayloadBuilder.builder()
            .boundary("bb")
            .delimiter()
            .line()
            .line("BODY");

    // empty part, expecting one part
    private static final BatchPayloadBuilder PART_03 = BatchPayloadBuilder.builder()
            .boundary("empty-part")
            .delimiter()
            .beginPart()
            .endPart()
            .closingDelimiter();

    // one part with one line and transport padding, expecting 1 part with one line
    private static final BatchPayloadBuilder PART_04 = BatchPayloadBuilder.builder()
            .boundary("a-boundary")
            .delimiter(8)
            .line()
            .beginPart()
                .line("c")
            .endPart()
            .closingDelimiter(16);

    // expecting two parts
    private static final BatchPayloadBuilder PART_05 = BatchPayloadBuilder.builder()
            .boundary("123")
            .text("--123") // CRLF is optional for starting delimiter
            .line()
            .beginPart()
                .line("First part")
            .endPart()
            .delimiter(2)
            .line()
            .beginPart()
                .line("Second part")
                .line("This is the second part")
            .endPart()
            .closingDelimiter();

    // expecting two parts
    private static final BatchPayloadBuilder PART_06 = BatchPayloadBuilder.builder()
            .boundary("--")
            .text("This is a preamble.")
            .delimiter()
            .line()
            .beginPart()
                .line("First part")
            .endPart()
            .delimiter(2)
            .line()
            .beginPart()
                .line("Second part")
                .line("This is the second part")
            .endPart()
            .closingDelimiter();

    // expecting three parts
    private static final BatchPayloadBuilder PART_07 = BatchPayloadBuilder.builder()
            .boundary("complex-boundary-123-????--")
            .text("This is a preamble.")
            .delimiter()
            .line()
            .beginPart()
                .line("First part")
            .endPart()
            .delimiter(2)
            .line()
            .beginPart()
                .line("Second part")
                .text("This is the second part")
            .endPart()
            .delimiter(123)
            .line()
            .beginPart()
                .line("here it is the third part")
                .line("spread over multiple")
                .text("lines")
            .endPart()
            .closingDelimiter()
            .line()
            .line("don't mind me, I'm just an")
            .line("epilogue");

    // The second batch of tests focuses on the syntactical structure of the boundaries

    // mismatching boundaries: the closing boundary differs from the expected one, expecting IllegalArgumentException
    private static final BatchPayloadBuilder BOUND_01 = BatchPayloadBuilder.builder()
            .boundary("start")
            .delimiter()
            .line()
            .line("I am a payload of a part")
            .line("--end--");

    // The boundary is missing one of the two starting hyphens, expecting zero parts
    private static final BatchPayloadBuilder BOUND_02 = BatchPayloadBuilder.builder()
            .boundary("missing hyphen")
            .line()
            .line("-missing hyphen")
            .line()
            .line("request")
            .line("body")
            .closingDelimiter();

    // The closing boundary does not match with the expected one (they differ by the last letter), expecting IllegalArgumentException
    private static final BatchPayloadBuilder BOUND_03 = BatchPayloadBuilder.builder()
            .boundary("boundary")
            .delimiter()
            .line()
            .line("request")
            .line("body")
            .line()
            .line("--boundar--");

    // The closing boundary does not match with the expected one (they differ by the first letter), expecting IllegalArgumentException
    private static final BatchPayloadBuilder BOUND_04 = BatchPayloadBuilder.builder()
            .boundary("boundary")
            .delimiter()
            .line()
            .line("request")
            .line("body")
            .line()
            .line("--oundary--");

    // expecting one part
    private static final BatchPayloadBuilder BOUND_05 = BatchPayloadBuilder.builder()
            .boundary("-")
            .delimiter()
            .line()
            .beginPart()
                .line("request")
                .line("body")
            .endPart()
            .closingDelimiter();

    // If there isn't a preamble, then CRLF can be omitted before the first delimiter, expecting one part
    private static final BatchPayloadBuilder BOUND_06 = BatchPayloadBuilder.builder()
            .boundary("1")
            .line("--1")
            .beginPart()
                .line("request")
                .line("body")
            .endPart()
            .closingDelimiter();

    private static final BatchPayloadBuilder BOUND_07 = BatchPayloadBuilder.builder()
            .boundary("   (#?")
            .delimiter()
            .line()
            .beginPart()
                .line("request")
            .endPart()
            .closingDelimiter();

    private static final BatchPayloadBuilder BOUND_08 = BatchPayloadBuilder.builder()
            .boundary("boundary_1234 #")
            .delimiter()
            .line()
            .beginPart()
                .line("Here it is boundary_1234 #")
                .line(" --boundary_1234 #")
            .endPart()
            .delimiter()
            .line()
            .beginPart()
                .line("--boundary_1234")
                .line("this was close")
                .line("-boundary_1234 #")
            .endPart()
            .closingDelimiter();

    // According to RFC 2046 this test shouldn't pass because a boundary must have at least one character which is not
    //  whitespace, but it seems that this parser accepts empty strings as boundaries
    private static final BatchPayloadBuilder BOUND_09 = BatchPayloadBuilder.builder()
            .boundary("")
            .delimiter()
            .line()
            .beginPart()
                .line("request")
            .endPart()
            .closingDelimiter();

    private static Stream<Arguments> inputs() {
        return Stream.of(
                Arguments.of(PART_00, Optional.empty(), Optional.of(IllegalArgumentException.class)),
                Arguments.of(PART_01, Optional.of(0), Optional.empty()),
                Arguments.of(PART_02, Optional.empty(), Optional.of(IllegalArgumentException.class)),
                Arguments.of(PART_03, Optional.of(1), Optional.empty()),
                Arguments.of(PART_04, Optional.of(1), Optional.empty()),
                Arguments.of(PART_05, Optional.of(2), Optional.empty()),
                Arguments.of(PART_06, Optional.of(2), Optional.empty()),
                Arguments.of(PART_07, Optional.of(3), Optional.empty()),
                Arguments.of(BOUND_01, Optional.empty(), Optional.of(IllegalArgumentException.class)),
                Arguments.of(BOUND_02, Optional.of(0), Optional.empty()),
                Arguments.of(BOUND_03, Optional.empty(), Optional.of(IllegalArgumentException.class)),
                Arguments.of(BOUND_04, Optional.empty(), Optional.of(IllegalArgumentException.class)),
                Arguments.of(BOUND_05, Optional.of(1), Optional.empty()),
                Arguments.of(BOUND_06, Optional.of(1), Optional.empty()),
                Arguments.of(BOUND_07, Optional.of(1), Optional.empty()),
                Arguments.of(BOUND_08, Optional.of(2), Optional.empty()),
                Arguments.of(BOUND_09, Optional.of(1), Optional.empty())
                );
    }

    @ParameterizedTest
    @MethodSource("inputs")
    public void test(BatchPayloadBuilder builder, Optional<Integer> expectedNumOfParts, Optional<Class<Exception>> expectedException) throws IOException {
        if (expectedNumOfParts.isEmpty() && expectedException.isEmpty()) {
            throw new IllegalArgumentException("no expected value");
        }
        MediaType mediaType = mediaType(builder.getBoundary());
        if (expectedNumOfParts.isPresent()) {
            List<BatchRequestItem> actualParts = BatchPayloadParser.parse(new ByteArrayInputStream(builder.create()), mediaType, new BatchRequestItem());
            Assertions.assertEquals(expectedNumOfParts.get(), actualParts.size());
            List<String> expectedParts = builder.getParts();
            for (int i = 0; i < actualParts.size(); i++) {
                Assertions.assertEquals(expectedParts.get(i), actualParts.get(i).getContent());
            }
        } else {
            Assertions.assertThrows(expectedException.get(), () -> BatchPayloadParser.parse(new ByteArrayInputStream(builder.create()), mediaType, new BatchRequestItem()));
        }
    }

    @Test
    public void testEmptyStream() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> BatchPayloadParser.parse(InputStream.nullInputStream(), mediaType("bb"), new BatchRequestItem())
        );
    }

    @Test
    public void testNullInputStream() {
        Assertions.assertThrows(
                NullPointerException.class,
                () -> BatchPayloadParser.parse(null, mediaType("null"), new BatchRequestItem())
        );
    }

    @Test
    public void testNullMediaType() {
        Assertions.assertThrows(
                NullPointerException.class,
                () -> BatchPayloadParser.parse(new ByteArrayInputStream(PART_07.create()), null, new BatchRequestItem())
        );
    }

    @Test
    public void testNullTemplate() {
        Assertions.assertThrows(
                NullPointerException.class,
                () -> BatchPayloadParser.parse(new ByteArrayInputStream(PART_07.create()), mediaType(PART_07.getBoundary()), null)
        );
    }

    private MediaType mediaType(String boundary) {
        MediaType t = mock(MediaType.class);
        when(t.getParameters()).thenReturn(Map.of("boundary", boundary));
        return t;
    }

    private static Stream<Arguments> httpInputs() {
        return Stream.of(
                Arguments.of(
                        BatchPayloadBuilder.builder()
                                .boundary("batch_d1befdc3-0a33-4463-8361??")
                                .delimiter()
                                .line()
                                .line("POST /batch HTTP/1.1")
                                .line("Content-Type:")
                                .line()
                                .text("{}")
                                .delimiter()
                                .line()
                                .line("DELETE /path/to/point?k=v HTTP/1.1")
                                .line("X-SomeCustomKey:\t\t\ta")
                                .line()
                                .line("body spread")
                                .line("over multiple lines")
                                .delimiter()
                                .line()
                                .line("PATCH /some/uri?k1=v1&k2=v2 HTTP/1.1")
                                .line("Content-Transfer-Encoding: binary")
                                .text("""
                                      Here it is a content, batch_d1befdc3-0a33-4463-8361??
                                      --batch_d1befdc3-0a33-4463-8361?
                                      batch_d1befdc3-0a33-4463-8361??
                                      --batch_d1befdc3-a33-4463-8361??
                                      """)
                                .closingDelimiter(),
                        List.of(
                                BatchRequestItemBuilder.builder()
                                        .method("POST").uri("/batch")
                                        .header("Content-Type", "")
                                        .content("{}")
                                        .create(),
                                BatchRequestItemBuilder.builder()
                                        .method("DELETE").uri("/path/to/point").query("k=v")
                                        .header("X-SomeCustomKey", "a")
                                        .content("""
                                                body spread\r
                                                over multiple lines\r
                                                """)
                                        .create(),
                                BatchRequestItemBuilder.builder()
                                        .method("PATCH").uri("/some/uri").query("k1=v1&k2=v2")
                                        .header("Content-Transfer-Encoding", "binary")
                                        .content("""
                                                  Here it is a content, batch_d1befdc3-0a33-4463-8361??
                                                  --batch_d1befdc3-0a33-4463-8361?
                                                  batch_d1befdc3-0a33-4463-8361??
                                                  --batch_d1befdc3-a33-4463-8361??
                                                  """)
                                        .create()
                        )
                ),
                Arguments.of(
                        BatchPayloadBuilder.builder()
                                .boundary("#?1")
                                .delimiter()
                                .line()
                                .line("POST /batch HTTP/1.1")
                                .line("Content-Type: text/plain")
                                .text("")
                                .closingDelimiter(),
                        List.of(
                                BatchRequestItemBuilder.builder()
                                        .method("POST").uri("/batch")
                                        .header("Content-Type", "text/plain")
                                        .content("")
                                        .create()
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource("httpInputs")
    public void testHttpPayloads(BatchPayloadBuilder builder, List<BatchRequestItem> expectedBatch) throws IOException {
        List<BatchRequestItem> actualBatch = BatchPayloadParser.parse(new ByteArrayInputStream(builder.create()), mediaType(builder.getBoundary()), new BatchRequestItem());
        Assertions.assertEquals(expectedBatch.size(), actualBatch.size());
        for (int i = 0; i < expectedBatch.size(); i++) {
            BatchRequestItem expected = expectedBatch.get(i);
            BatchRequestItem actual = actualBatch.get(i);
            Assertions.assertEquals(expected.getMethod(), actual.getMethod());
            Assertions.assertEquals(expected.getRequestURI(), actual.getRequestURI());
            Assertions.assertEquals(expected.getQueryString(), actual.getQueryString());
            Assertions.assertEquals(expected.getHeaders(), actual.getHeaders());
            Assertions.assertEquals(expected.getContent(), actual.getContent());
        }
    }
}
