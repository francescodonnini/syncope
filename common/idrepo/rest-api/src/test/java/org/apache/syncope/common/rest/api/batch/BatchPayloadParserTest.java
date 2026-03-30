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

    private static Arguments MH00 = Arguments.of(
            BatchPayloadBuilder.builder()
                    .boundary("mh00")
                    .delimiter()
                    .line()
                    .line("DELETE /groups/3 HTTP/1.1")
                    .closingDelimiter(),
            List.of(BatchRequestItemBuilder.builder().method("DELETE").uri("/groups/3").create())
    );

    private static Arguments MH01 = Arguments.of(
            BatchPayloadBuilder.builder()
                    .boundary("mh01")
                    .delimiter()
                    .line()
                    .line("content-type: application/http")
                    .line("DELETE /groups/42 HTTP/1.1")
                    .closingDelimiter(),
            List.of(BatchRequestItemBuilder.builder().method("DELETE").uri("/groups/42").create())
    );

    private static Arguments MH02 = Arguments.of(
            BatchPayloadBuilder.builder()
                    .boundary("mh02")
                    .delimiter()
                    .line()
                    .line("CONTENT-TRANSFER-ENCODING: BINARY")
                    .line("DELETE /groups/bcdf1742-ffe6-4ee1-8e92-3033e243a62b HTTP/1.1")
                    .closingDelimiter(),
            List.of(BatchRequestItemBuilder.builder().method("DELETE").uri("/groups/bcdf1742-ffe6-4ee1-8e92-3033e243a62b").create())
    );

    private static Arguments MH03 = Arguments.of(
            BatchPayloadBuilder.builder()
                    .boundary("mh02")
                    .delimiter()
                    .line()
                    .line("CONTENT-TRANSFER-ENCODING: BINARY")
                    .line("Content-Type: APPLICATION/json")
                    .line("PATCH /users/c40754aa-991e-41d2-b6c6-86c258ca3b1b HTTP/1.1")
                    .line("AccepT: application/json")
                    .line()
                    .line("{\"firstName\": \"Mario\", \"lastName\": \"Rossi\"}")
                    .delimiter()
                    .line()
                    .line("CONTENT-TRANSFER-ENCODING BINARY")
                    .line("DELETE /groups/bcdf1742-ffe6-4ee1-8e92-3033e243a62b HTTP/1.1")
                    .closingDelimiter(),
            List.of(
                    BatchRequestItemBuilder.builder()
                                    .method("PATCH").uri("/users/c40754aa-991e-41d2-b6c6-86c258ca3b1b")
                                    .header("Accept", "application/json")
                                    .line("{\"firstName\": \"Mario\", \"lastName\": \"Rossi\"}")
                                    .create(),
                    BatchRequestItemBuilder.builder()
                            .line("CONTENT-TRANSFER-ENCODING BINARY")
                            .line("DELETE /groups/bcdf1742-ffe6-4ee1-8e92-3033e243a62b HTTP/1.1")
                            .create())
    );

    private static Stream<Arguments> oldHttpRequestInputs() {
        return Stream.of(
                Arguments.of(
                        BatchPayloadBuilder.builder().boundary("padded_boundary")
                                .delimiter(5)
                                .line()
                                .line("Content-Type: application/http")
                                .line("Content-Transfer-Encoding: binary")
                                .line()
                                .line("POST /users HTTP/1.1")
                                .line("Accept: application/json")
                                .line()
                                .text("{\"name\":\"rossi\"}")
                                .delimiter(2)
                                .line()
                                .line("Content-Type: application/http")
                                .line()
                                .line("PATCH /groups/2 HTTP/1.1")
                                .line()
                                .closingDelimiter(10),
                        List.of(
                                BatchRequestItemBuilder.builder()
                                        .method("POST").uri("/users")
                                        .header("Accept", "application/json")
                                        .content("{\"name\":\"rossi\"}")
                                        .create(),
                                BatchRequestItemBuilder.builder()
                                        .method("PATCH").uri("/groups/2")
                                        .content("")
                                        .create()
                        )
                ),
                Arguments.of(
                        BatchPayloadBuilder.builder()
                                .boundary("1")
                                .delimiter()
                                .line()
                                .line("Content-Type: application/http") // Only one line present
                                .line()
                                .line("PUT /users/1 HTTP/1.1")
                                .line("Accept: application/json")
                                .line()
                                .text("body")
                                .closingDelimiter(),
                        List.of(BatchRequestItemBuilder.builder()
                                .method("PUT").uri("/users/1")
                                .header("Accept", "application/json")
                                .content("body")
                                .create())
                ),
                Arguments.of(
                        BatchPayloadBuilder.builder()
                                .boundary("specials:'()+_,-./=?")
                                .delimiter()
                                .line()
                                .line("ContentType application/http") // missing colon ':'
                                .line()
                                .line("POST /users HTTP/1.1")
                                .line()
                                .text("body")
                                .closingDelimiter(),
                        List.of(BatchRequestItemBuilder.builder()
                                .content("ContentType application/http\r\n\r\nPOST /users HTTP/1.1\r\n\r\nbody")
                                .create())
                ),
                // Missing envelope
                Arguments.of(
                        BatchPayloadBuilder.builder()
                                .boundary("boundary with internal spaces")
                                .delimiter()
                                .line()
                                .line("DELETE /users/1 HTTP/1.1")
                                .line("X-Header: value")
                                .line()
                                .text("body")
                                .closingDelimiter(),
                        List.of(BatchRequestItemBuilder.builder()
                                .method("DELETE").uri("/users/1")
                                .header("X-Header", "value")
                                .content("body")
                                .create())
                ),
                // Missing request-line
                Arguments.of(
                        BatchPayloadBuilder.builder().boundary("a-very-long-boundary-string-that-will-tests-the-limits-1234567890")
                                .delimiter()
                                .line()
                                .line("Content-Type: application/http")
                                .line("Content-Transfer-Encoding: binary")
                                .line()
                                .line("Accept: application/json")
                                .line()
                                .line("body")
                                .line("\tbody")
                                .line("a-very-long-boundary-string-that-will-tests-the-limits-1234567890")
                                .delimiter()
                                .line()
                                .line("Content-Type: application/http")
                                .line()
                                .line("PUT /users/5 HTTP/1.1")
                                .line()
                                .text("valid body")
                                .closingDelimiter(),
                        List.of(
                                BatchRequestItemBuilder.builder()
                                        .method(null)
                                        .uri(null)
                                        .header("Accept", "application/json")
                                        .line("body")
                                        .line("\tbody")
                                        .line("a-very-long-boundary-string-that-will-tests-the-limits-1234567890")
                                        .create(),
                                BatchRequestItemBuilder.builder()
                                        .method("PUT").uri("/users/5")
                                        .content("valid body")
                                        .create()
                        )
                ),
                Arguments.of(
                        BatchPayloadBuilder.builder()
                                .boundary("")
                                .delimiter()
                                .line()
                                .line("Content-Type: application/http")
                                .line("Content-Transfer-Encoding: binary")
                                .line()
                                .line("POST /users") // Missing HTTP/1.1
                                .line()
                                .closingDelimiter(),
                        List.of(BatchRequestItemBuilder.builder()
                                .content("POST /users\r\n\r\n")
                                .create())
                ),
                Arguments.of(
                        BatchPayloadBuilder.builder()
                                .boundary("-")
                                .delimiter()
                                .line()
                                .line("Content-Type: application/http")
                                .line("Content-Transfer-Encoding: binary")
                                .line()
                                .line("GET /users HTTP/1.1")
                                .line()
                                .text("body")
                                .closingDelimiter(),
                        List.of(BatchRequestItemBuilder.builder()
                                .content("body")
                                .create())
                ),
                Arguments.of(
                        BatchPayloadBuilder.builder().boundary("mixed_CASE_Boundary_123")
                                .delimiter()
                                .line()
                                .line("Content-Type: application/http")
                                .line()
                                .line("POST /users HTTP/1.1")
                                .line("X-Domain: Master, Other") // Comma-separated values
                                .line("X-Domain: Third") // Duplicated key, its value should be appended to the ones above
                                .line()
                                .text("body")
                                .delimiter()
                                .line()
                                .line("DELETE /users/1 HTTP/1.1")
                                .line()
                                .closingDelimiter(),
                        List.of(
                                BatchRequestItemBuilder.builder()
                                        .method("POST").uri("/users")
                                        .header("X-Domain", "Master")
                                        .header("X-Domain", "Other")
                                        .header("X-Domain", "Third")
                                        .content("body")
                                        .create(),
                                BatchRequestItemBuilder.builder()
                                        .method("DELETE").uri("/users/1")
                                        .content("")
                                        .create()
                        )
                ),
                Arguments.of(
                        BatchPayloadBuilder.builder()
                                .boundary("----a lot of hyphens!----")
                                .delimiter()
                                .line()
                                .line("Content-Type: application/http")
                                .line()
                                .line("POST /users HTTP/1.1")
                                .line("Accept: application/json")
                                .line("BadHeaderFormat") // Manca ':'
                                .text("body")
                                .closingDelimiter(),
                        List.of(BatchRequestItemBuilder.builder()
                                .method("POST").uri("/users")
                                .header("Accept", "application/json")
                                .line("BadHeaderFormat")
                                .content("body")
                                .create())
                ),

                Arguments.of(
                        BatchPayloadBuilder.builder()
                                .boundary("bnd_12")
                                .delimiter()
                                .line()
                                .line("Content-Type: application/http")
                                .line()
                                .line("POST /users HTTP/1.1")
                                .line("X-Empty-Header:\t\t\t\t")
                                .line()
                                .text("body")
                                .closingDelimiter(),
                        List.of(BatchRequestItemBuilder.builder()
                                .method("POST").uri("/users")
                                .header("X-Empty-Header", "")
                                .content("body")
                                .create())
                ),

                Arguments.of(
                        BatchPayloadBuilder.builder()
                                .boundary("bnd_13")
                                .delimiter()
                                .line()
                                .line("Content-Type: application/http")
                                .line()
                                .line("DELETE /users/1 HTTP/1.1")
                                .line()
                                .closingDelimiter(),
                        List.of(BatchRequestItemBuilder.builder()
                                .method("DELETE").uri("/users/1")
                                .content("")
                                .create())
                ),
                Arguments.of(
                        BatchPayloadBuilder.builder()
                                .boundary("query-boundary-padding")
                                .delimiter(3)
                                .line()
                                .line("Content-Type: application/http")
                                .line("Content-Transfer-Encoding: binary")
                                .line()
                                .line("PATCH /users?name=rossi&status=active HTTP/1.1")
                                .delimiter(3)
                                .line()
                                .line("PUT /groups?someStringWithoutEqualsAndAmpersands HTTP/1.1")
                                .line()
                                .line("Some text spread")
                                .line("over multiple lines!")
                                .closingDelimiter(5),
                        List.of(
                                BatchRequestItemBuilder.builder()
                                        .method("PATCH")
                                        .uri("/users")
                                        .query("name=rossi&status=active")
                                        .content("")
                                        .create(),
                                BatchRequestItemBuilder.builder()
                                        .method("PUT")
                                        .uri("/groups")
                                        .query("someStringWithoutEqualsAndAmpersands")
                                        .content("Some text spread\r\nover multiple lines!\r\n")
                                        .create()
                        )
                ),
                // A trailing '?' should be ignored
                Arguments.of(
                        BatchPayloadBuilder.builder()
                                .boundary("?")
                                .delimiter()
                                .line()
                                .line("Content-Type: application/http")
                                .line("Content-Transfer-Encoding: binary")
                                .line()
                                .line("POST /groups? HTTP/1.1")
                                .line()
                                .closingDelimiter(),
                        List.of(
                                BatchRequestItemBuilder.builder()
                                        .method("POST")
                                        .uri("/groups")
                                        .create()
                        )
                ),
                Arguments.of(
                        BatchPayloadBuilder.builder()
                                .boundary("trying to put tabs instead of spaces")
                                .delimiter()
                                .line()
                                .line("Content-Type: application/http")
                                .line("Content-Transfer-Encoding: binary")
                                .line()
                                .line("PUT\t/resources?what=this\tHTTP/1.1")
                                .line()
                                .closingDelimiter(),
                        List.of(
                                BatchRequestItemBuilder.builder()
                                        .method(null)
                                        .uri(null)
                                        .query(null)
                                        .create()
                        )
                )
                // Arguments.of(
                //         BatchPayloadBuilder.builder()
                //                 .boundary("bnd_9")
                //                 .delimiter()
                //                 .line()
                //                 .line("Content-Type: application/http")
                //                 .line("Content-Transfer-Encoding: binary")
                //                 .line()
                //                 .line("POST  /users HTTP/1.1")
                //                 .line()
                //                 .closingDelimiter(),
                //         List.of(BatchRequestItemBuilder.builder()
                //                 .method(null)
                //                 .uri(null)
                //                 .create())
                // ),
//                // A query string should end before '#'
//                Arguments.of(
//                        BatchPayloadBuilder.builder()
//                                .boundary("#boundary#")
//                                .delimiter()
//                                .line()
//                                .line("Content-Type: application/http")
//                                .line("Content-Transfer-Encoding: binary")
//                                .line()
//                                .line("DELETE /anyObjects? HTTP/1.1")
//                                .delimiter()
//                                .line()
//                                .line("DELETE /users?name=giuseppe&lastname=verdi#ignoreMe HTTP/1.1")
//                                .closingDelimiter(),
//                        List.of(
//                                BatchRequestItemBuilder.builder()
//                                        .method("DELETE")
//                                        .uri("/anyObjects")
//                                        .create(),
//                                BatchRequestItemBuilder.builder()
//                                        .method("DELETE")
//                                        .uri("/users")
//                                        .query("name=giuseppe&lastname=verdi")
//                                        .create()
//                        )
//                ),
//                // This test should pass because the second '?' should be treated as an integral part of the query,
//                // but it doesn't because the parser doesn't handle the query string correctly.
//                Arguments.of(
//                        BatchPayloadBuilder.builder()
//                                .boundary("multiple?question?marks")
//                                .delimiter()
//                                .line()
//                                .line("Content-Type: application/http")
//                                .line("Content-Transfer-Encoding: binary")
//                                .line()
//                                .line("PUT /resources?k1=v1?k2=v2 HTTP/1.1")
//                                .line()
//                                .closingDelimiter(),
//                        List.of(
//                                BatchRequestItemBuilder.builder()
//                                        .method("PUT")
//                                        .uri("/resources")
//                                        .query("k1=v1?k2=v2")
//                                        .content("")
//                                        .create()
//                        )
//                ),
//                Arguments.of(
//                        BatchPayloadBuilder.builder()
//                                .boundary("simple boundary")
//                                .delimiter()
//                                .line()
//                                .line("Content-Type: application/http")
//                                .line("Content-Transfer-Encoding: binary")
//                                .line()
//                                .line("POST HTTP/1.1") // Missing URI
//                                .line()
//                                .closingDelimiter(),
//                        List.of(BatchRequestItemBuilder.builder()
//                                .method("POST")
//                                .create())
//                )
        );
    }

    private static Stream<Arguments> httpRequestInputs() {
        return Stream.of(
                MH00, MH01, MH02, MH03
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

    private static Stream<Arguments> oldHttpResponseInputs() {
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

    private static Stream<Arguments> httpResponseInputs() {
        return Stream.of(
                Arguments.of(
                        BatchPayloadBuilder.builder()
                                .boundary("simple-boundary-1")
                                .delimiter()
                                .line()
                                .line("Content-Type: application/http")
                                .line("Content-Transfer-Encoding: binary")
                                .line()
                                .line("HTTP/1.1 200 OK")
                                .line("Content-Type: application/json")
                                .line()
                                .text("{\"status\":\"success\"}")
                                .closingDelimiter(),
                        List.of(BatchResponseItemBuilder.builder()
                                .status(200)
                                .header("Content-Type", "application/json")
                                .content("{\"status\":\"success\"}")
                                .create())),
                Arguments.of(
                        BatchPayloadBuilder.builder()
                                .boundary("Bnd_Mixed_CaSe_99")
                                .delimiter()
                                .line()
                                .line("Content-Transfer-Encoding:\t\tbinary")
                                .line()
                                .line("HTTP/1.1 201 Created")
                                .line("Location: /users/1")
                                .line()
                                .text("body")
                                .closingDelimiter(),
                        List.of(BatchResponseItemBuilder.builder()
                                .status(201)
                                .header("Location", "/users/1")
                                .content("body")
                                .create())),
                Arguments.of(
                        BatchPayloadBuilder.builder()
                                .boundary("a very long boundary string up-to 70 characters 123456789012345678")
                                .delimiter()
                                .line()
                                .line("ContentType application/http") // Missing colon
                                .line()
                                .line("HTTP/1.1 200 OK")
                                .line()
                                .line("body")
                                .closingDelimiter(),
                        List.of(BatchResponseItemBuilder.builder()
                                .content("ContentType application/http\r\n\r\nHTTP/1.1 200 OK\r\n\r\nbody\r\n")
                                .create())),
                Arguments.of(
                        BatchPayloadBuilder.builder()
                                .boundary("+-.2-./:=?")
                                .delimiter()
                                .line()
                                .line("HTTP/1.1 -200 OK")
                                .line("X-Header: value")
                                .line()
                                .text("body")
                                .closingDelimiter(),
                        List.of(BatchResponseItemBuilder.builder()
                                .content("HTTP/1.1 -200 OK\r\nX-Header: value\r\n\r\nbody")
                                .create())),
                Arguments.of(
                        BatchPayloadBuilder.builder()
                                .boundary("200 OK")
                                .delimiter()
                                .line()
                                .line("Content-Type: application/http")
                                .line("Content-Transfer-Encoding: binary")
                                .line()
                                .line("200 OK")
                                .line()
                                .closingDelimiter(),
                        List.of(BatchResponseItemBuilder.builder()
                                .content("200 OK\r\n\r\n")
                                .create())),
                Arguments.of(
                        BatchPayloadBuilder.builder()
                                .boundary("another-boundary-======")
                                .delimiter()
                                .line()
                                .line("Content-Type: application/http")
                                .line("Content-Transfer-Encoding: binary")
                                .line()
                                .line("HTTP/1.1 12K OK")
                                .line()
                                .line("body")
                                .delimiter()
                                .line()
                                .line("Content-Type: application/http")
                                .line("Content-Transfer-Encoding: binary")
                                .line()
                                .line("HTTP/1.1  200 OK")
                                .line()
                                .text("body")
                                .closingDelimiter(),
                        List.of(
                                BatchResponseItemBuilder.builder()
                                        .content("HTTP/1.1 12K OK\r\n\r\nbody\r\n")
                                        .create(),
                                BatchResponseItemBuilder.builder()
                                        .content("HTTP/1.1  200 OK\r\n\r\nbody")
                                        .create())),
                Arguments.of(
                        BatchPayloadBuilder.builder()
                                .boundary("a lot of spaces")
                                .delimiter()
                                .line()
                                .line("Content-Type: application/http")
                                .line("Content-Transfer-Encoding: binary")
                                .line()
                                .line("HTTP/1.1 204") // No reason-phrase
                                .line()
                                .delimiter()
                                .line()
                                .line("Content-Type: application/http")
                                .line()
                                .line("HTTP/1.1 200 OK")
                                .line("X-tags:       tagA,       tagB")
                                .line("X-Tags: tagC")
                                .line()
                                .line("b")
                                .line("\to")
                                .line("\t\td")
                                .line("\t\t\ty")
                                .delimiter()
                                .line()
                                .line("Content-Type: application/http")
                                .line()
                                .line("HTTP/1.1 200 OK")
                                .line("BadHeaderFormat")
                                .text("body")
                                .closingDelimiter(),
                        List.of(
                                BatchResponseItemBuilder.builder()
                                        .status(204)
                                        .create(),
                                BatchResponseItemBuilder.builder()
                                        .status(200)
                                        .header("X-Tags", "tagA")
                                        .header("X-Tags", "tagB")
                                        .header("X-Tags", "tagC")
                                        .line("b")
                                        .line("\to")
                                        .line("\t\td")
                                        .line("\t\t\ty")
                                        .create(),
                                BatchResponseItemBuilder.builder()
                                        .status(200)
                                        .content("BadHeaderFormat\r\nbody")
                                        .create())),
                Arguments.of(
                        BatchPayloadBuilder.builder().boundary("too-many-digit-status-+-")
                                .delimiter()
                                .line()
                                .line("Content-Type: application/http")
                                .line()
                                .line("HTTP/1.1 123456789 No Content")
                                .line()
                                .closingDelimiter(),
                        List.of(BatchResponseItemBuilder.builder()
                                .status(123456789)
                                .create())),
                Arguments.of(
                        BatchPayloadBuilder.builder().boundary("duplicated-keys-boundary-!?_")
                                .delimiter()
                                .line()
                                .line("Content-Type: application/http")
                                .line()
                                .line("HTTP/3.14 200")
                                .delimiter()
                                .line()
                                .line("HTTP/1.1 404")
                                .line("A-Key: 1")
                                .line("a-Key: 2")
                                .line("A-Key: 3")
                                .closingDelimiter(),
                        List.of(
                                BatchResponseItemBuilder.builder()
                                        .content("HTTP/3.14 200\r\n")
                                        .create(),
                                BatchResponseItemBuilder.builder()
                                        .status(404)
                                        .header("a-Key", "1")
                                        .header("a-Key", "2")
                                        .header("A-Key", "3")
                                        .create()))
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

    private MediaType mediaType(String boundary) {
        MediaType t = mock(MediaType.class);
        when(t.getParameters()).thenReturn(Map.of("boundary", boundary));
        return t;
    }

    private void assertEquals(BatchItem expected, BatchItem actual) {
        Assertions.assertEquals(expected.getHeaders(), actual.getHeaders());
        Assertions.assertEquals(expected.getContent(), actual.getContent());
    }

    @Test
    public void printHttpInputs() {
        int i = 37;
        for (Arguments argument : httpResponseInputs().toList()) {
            StringBuilder s = new StringBuilder();
            s.append("\\begin{figure}[H]\n");
            s.append("\t\\centering\n");
            s.append("\t\\begin{lstlisting}[style=EBNF]\n");
            s.append(argument.get()[0]).append('\n');
            s.append("\t\\end{lstlisting}\n");
            s.append("\t\\caption{Payload del caso di test TC" + i + "}\n");
            s.append("\\label{fig:BatchPayloadParser:tests:response:" + i + "}\n");
            s.append("\\end{figure}\n");
            System.out.println(s.toString());
            i += 1;
        }
    }
}
