package org.apache.syncope.common.rest.api.batch;

import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
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

    private static final BatchPayloadBuilder BOUND_10 = BatchPayloadBuilder.builder()
            .boundary("a really long boundary, containing a number of characters greater than 70!11!1!")
            .delimiter()
            .line()
            .beginPart()
                .line("The question is:")
                .line("is this parser compliant to RFC 2046?")
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
                Arguments.of(BOUND_09, Optional.of(1), Optional.empty()),
                Arguments.of(BOUND_10, Optional.of(1), Optional.empty())
        );
    }

    @Test
    public void printInputs() {
        int i = 1;
        for (Arguments argument : inputs().toList()) {
            StringBuilder s = new StringBuilder();
            s.append("\\begin{figure}[H]\n");
            s.append("\t\\centering\n");
            s.append("\t\\begin{lstlisting}[style=EBNF]\n");
            s.append(argument.get()[0]).append('\n');
            s.append("\t\\end{lstlisting}\n");
            s.append("\t\\caption{Payload del caso di test TC" + i + "}\n");
            s.append("\\label{fig:BatchPayloadParser:tests:" + i + "}\n");
            s.append("\\end{figure}\n");
            System.out.println(s.toString());
            i += 1;
        }
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

    private static Stream<Arguments> httpRequestInputs() {
        return Stream.of(
                Arguments.of(
                        BatchPayloadBuilder.builder()
                                .boundary("batch_555806432")
                                .delimiter()
                                .line()
                                .line("Content-Type: application/http")
                                .line("Content-Transfer-Encoding: binary")
                                .line()
                                .line("POST /users HTTP/1.1")
                                .line("Content-Type: application/json")
                                .line()
                                .line("{\"firstname\":\"mario\",\"lastname\":\"rossi\"}")
                                .delimiter()
                                .line()
                                .line("PATCH /groups?name=admin HTTP/1.1")
                                .closingDelimiter(),
                        List.of(
                                BatchRequestItemBuilder.builder()
                                        .method("POST")
                                        .uri("/users")
                                        .header("Content-Type", "application/json")
                                        .line("{\"firstname\":\"mario\",\"lastname\":\"rossi\"}")
                                        .create(),
                                BatchRequestItemBuilder.builder()
                                        .method("PATCH")
                                        .uri("/groups")
                                        .query("name=admin")
                                        .create()
                        )
                ),
                Arguments.of(
                        BatchPayloadBuilder.builder()
                                .boundary("my-boundary")
                                .delimiter()
                                .line()
                                .line("Content-Type: application/http")
                                .line("Content-Transfer-Encoding: binary")
                                .line()
                                .line("DELETE /anyObjects?k1=v1&k2=v2 HTTP/1.1")
                                .line("X-List: val1, val2, val3")
                                .line()
                                .line("Multi-line")
                                .text("content body")
                                .closingDelimiter(),
                        List.of(
                                BatchRequestItemBuilder.builder()
                                        .method("DELETE")
                                        .uri("/anyObjects")
                                        .query("k1=v1&k2=v2")
                                        .header("X-List", "val1")
                                        .header("X-List", "val2")
                                        .header("X-List", "val3")
                                        .content("Multi-line\r\ncontent body")
                                        .create()
                        )
                ),
                Arguments.of(
                        BatchPayloadBuilder.builder()
                                .boundary(" %6&")
                                .delimiter()
                                .line()
                                .line("Content-Type: application/http")
                                .line("Content-Transfer-Encoding: binary")
                                .line()
                                .line("GET /users? HTTP/1.1")
                                .line("Malformed Header Without Colon")
                                .closingDelimiter(),
                        List.of(
                                BatchRequestItemBuilder.builder()
                                        .content("Malformed Header Without Colon\r\n")
                                        .create()
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource("httpRequestInputs")
    public void testHttpRequests(BatchPayloadBuilder builder, List<BatchRequestItem> expectedBatch) throws IOException {
        List<BatchRequestItem> actualBatch = BatchPayloadParser.parse(
                new ByteArrayInputStream(builder.create()),
                mediaType(builder.getBoundary()),
                new BatchRequestItem());

        Assertions.assertEquals(expectedBatch.size(), actualBatch.size());
        for (int i = 0; i < expectedBatch.size(); i++) {
            BatchRequestItem expected = expectedBatch.get(i);
            BatchRequestItem actual = actualBatch.get(i);
            Assertions.assertEquals(expected.getMethod(), actual.getMethod());
            Assertions.assertEquals(expected.getRequestURI(), actual.getRequestURI());
            Assertions.assertEquals(expected.getQueryString(), actual.getQueryString());
            assertEquals(expected, actual);
        }
    }

    private static Stream<Arguments> httpResponseInputs() {
        return Stream.of(
                Arguments.of(
                        BatchPayloadBuilder.builder()
                                .boundary("res-boundary")
                                .delimiter()
                                .line()
                                .line("Content-Type: application/http")
                                .line("Content-Transfer-Encoding: binary")
                                .line()
                                .line("HTTP/1.1 200 OK")
                                .line("Content-Type: application/json")
                                .line()
                                .line("{\"status\":\"success\",")
                                .text("\"code\":200}")
                                .delimiter()
                                .line()
                                .line("Content-Type: application/http")
                                .line("Content-Transfer-Encoding: binary")
                                .line()
                                .line("HTTP/1.1 23$ OK")
                                .line()
                                .text("Content-Type: application/json")
                                .closingDelimiter(),
                        List.of(
                                BatchResponseItemBuilder.builder()
                                        .status(200)
                                        .header("Content-Type", "application/json")
                                        .content("{\"status\":\"success\",\r\n\"code\":200}")
                                        .create(),
                                BatchResponseItemBuilder.builder()
                                        .content("Content-Type: application/json")
                                        .create()
                        )
                ),
                Arguments.of(
                        BatchPayloadBuilder.builder()
                                .boundary("res-boundary")
                                .delimiter()
                                .line()
                                .line("Content-Type: application/http")
                                .line("Content-Transfer-Encoding: binary")
                                .line()
                                .line("HTTP/1.1 204 No Content")
                                .line()
                                .closingDelimiter(),
                        List.of(
                                BatchResponseItemBuilder.builder()
                                        .status(204)
                                        .content("")
                                        .create()
                        )
                ),
                Arguments.of(
                        BatchPayloadBuilder.builder()
                                .boundary("res-boundary")
                                .delimiter()
                                .line()
                                .line("Content-Type: application/http")
                                .line("Content-Transfer-Encoding: binary")
                                .line()
                                .line("HTTP/1.1 OK OK")
                                .line("X-Tags: tagA, tagB")
                                .line("Content-Type:\t\tapplication/json")
                                .line()
                                .text("Single-line payload")
                                .closingDelimiter(),
                        List.of(
                                BatchResponseItemBuilder.builder()
                                        .header("X-Tags", "tagA")
                                        .header("X-Tags", "tagB")
                                        .header("Content-Type", "application/json")
                                        .content("Single-line payload")
                                        .create()
                        )
                ),
                Arguments.of(
                        BatchPayloadBuilder.builder()
                                .boundary("res-boundary")
                                .delimiter()
                                .line()
                                .line("Content-Type: application/http")
                                .line("Content-Transfer-Encoding: binary")
                                .line()
                                .line("HTTP/1.1 400 Bad Request")
                                .line("Invalid-Header-Format")
                                .closingDelimiter(),
                        List.of(
                                BatchResponseItemBuilder.builder()
                                        .status(400)
                                        .content("Invalid-Header-Format\r\n")
                                        .create()
                        )
                ),
                Arguments.of(
                        BatchPayloadBuilder.builder()
                                .boundary("boundary")
                                .delimiter()
                                .line()
                                .line("Content-Type: application/http")
                                .line("Content-Transfer-Encoding: binary")
                                .line()
                                .line("HTTP/1.1 2O0 OK")
                                .line()
                                .text("Single-line payload")
                                .closingDelimiter(),
                        List.of(
                                BatchResponseItemBuilder.builder()
                                        .content("Single-line payload")
                                        .create()
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource("httpResponseInputs")
    public void testHttpResponses(BatchPayloadBuilder builder, List<BatchResponseItem> expectedBatch) throws IOException {
        List<BatchResponseItem> actualBatch = BatchPayloadParser.parse(
                new ByteArrayInputStream(builder.create()),
                mediaType(builder.getBoundary()),
                new BatchResponseItem());

        Assertions.assertEquals(expectedBatch.size(), actualBatch.size());
        for (int i = 0; i < expectedBatch.size(); ++i) {
            BatchResponseItem actual = actualBatch.get(i);
            BatchResponseItem expected = expectedBatch.get(i);
            Assertions.assertEquals(expected.getStatus(), actual.getStatus());
            assertEquals(expected, actual);
        }
    }

    @Test
    public void printHttpInputs() {
        int i = 1;
        for (Arguments argument : httpRequestInputs().toList()) {
            StringBuilder s = new StringBuilder();
            s.append("\\begin{figure}[H]\n");
            s.append("\t\\centering\n");
            s.append("\t\\begin{lstlisting}[style=EBNF]\n");
            s.append(argument.get()[0]).append('\n');
            s.append("\t\\end{lstlisting}\n");
            s.append("\t\\caption{Payload del caso di test TC" + i + "}\n");
            s.append("\\label{fig:BatchPayloadParser:tests:" + i + "}\n");
            s.append("\\end{figure}\n");
            System.out.println(s.toString());
            i += 1;
        }
    }

    private MediaType mediaType(String boundary) {
        MediaType t = mock(MediaType.class);
        when(t.getParameters()).thenReturn(Map.of("boundary", boundary));
        return t;
    }

    private void assertEquals(BatchItem expected, BatchItem actual) {
        Assertions.assertEquals(expected.getHeaders(), actual.getHeaders());
        Assertions.assertEquals(expected.getContent(), actual.getContent());
    }
}
