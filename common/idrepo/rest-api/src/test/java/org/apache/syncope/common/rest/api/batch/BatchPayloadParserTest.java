/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.common.rest.api.batch;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class BatchPayloadParserTest {
    private static final boolean SKIP_TEST = true;
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

    // The closing boundary does not match with the expected one
    // (they differ by the last letter) expecting IllegalArgumentException
    private static final BatchPayloadBuilder BOUND_03 = BatchPayloadBuilder.builder()
            .boundary("boundary")
            .delimiter()
            .line()
            .line("request")
            .line("body")
            .line()
            .line("--boundar--");

    // The closing boundary does not match with the expected one
    // (they differ by the first letter), expecting IllegalArgumentException
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

    @ParameterizedTest
    @MethodSource("inputs")
    public void test(
            final BatchPayloadBuilder builder,
            final Optional<Integer> expectedNumOfParts,
            final Optional<Class<Exception>> expectedException) throws IOException {
        if (expectedNumOfParts.isEmpty() && expectedException.isEmpty()) {
            throw new IllegalArgumentException("no expected value");
        }
        MediaType mediaType = mediaType(builder.getBoundary());
        if (expectedNumOfParts.isPresent()) {
            List<BatchRequestItem> actualParts = BatchPayloadParser.parse(
                    new ByteArrayInputStream(builder.create()),
                    mediaType,
                    new BatchRequestItem());
            Assertions.assertEquals(expectedNumOfParts.get(), actualParts.size());
            List<String> expectedParts = builder.getParts();
            for (int i = 0; i < actualParts.size(); i++) {
                Assertions.assertEquals(expectedParts.get(i), actualParts.get(i).getContent());
            }
        } else {
            Assertions.assertThrows(
                    expectedException.get(),
                    () -> BatchPayloadParser.parse(
                            new ByteArrayInputStream(builder.create()),
                            mediaType,
                            new BatchRequestItem()));
        }
    }

    @Test
    public void testEmptyStream() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> BatchPayloadParser.parse(
                        InputStream.nullInputStream(),
                        mediaType("bb"),
                        new BatchRequestItem()));
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
                () -> BatchPayloadParser.parse(
                        new ByteArrayInputStream(PART_07.create()),
                        null,
                        new BatchRequestItem()));
    }

    @Test
    public void testInvalidMediaType() {
        Assertions.assertThrows(
                NullPointerException.class,
                () -> BatchPayloadParser.parse(
                        new ByteArrayInputStream(PART_07.create()),
                        MediaType.TEXT_PLAIN_TYPE,
                        new BatchRequestItem()));
    }

    @Test
    public void testNullTemplate() {
        Assertions.assertThrows(
                NullPointerException.class,
                () -> BatchPayloadParser.parse(
                        new ByteArrayInputStream(PART_07.create()),
                        mediaType(PART_07.getBoundary()),
                        null));
    }

    private static Stream<Arguments> httpRequestInputs() {
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
                        ),
                        false
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
                                .create()),
                        false
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
                                .create()),
                        false
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
                                .create()),
                        false
                ),
                // Missing request-line
                Arguments.of(
                        BatchPayloadBuilder.builder()
                                .boundary("a-very-long-boundary-string-that-will-tests-the-limits-1234567890")
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
                        ),
                        false
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
                                .create()),
                        false
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
                                .create()),
                        false
                ),
                Arguments.of(
                        BatchPayloadBuilder.builder().boundary("mixed_CASE_Boundary_123")
                                .delimiter()
                                .line()
                                .line("Content-Type: application/http")
                                .line()
                                .line("POST /users HTTP/1.1")
                                .line("X-Domain: Master, Other") // Comma-separated values
                                // Duplicated key, its value should be appended to the ones above
                                .line("X-Domain: Third")
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
                        ),
                        false
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
                                .line("BadHeader Format") // Manca ':'
                                .line()
                                .text("body")
                                .closingDelimiter(),
                        List.of(BatchRequestItemBuilder.builder()
                                .method("POST").uri("/users")
                                .header("Accept", "application/json")
                                .line("BadHeader Format")
                                .line("")
                                .content("body")
                                .create()),
                        false
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
                                .create()),
                        false
                ),
                Arguments.of(
                        BatchPayloadBuilder.builder()
                                .boundary("bnd_13")
                                .delimiter()
                                .line()
                                .line("Content-Type:application/http")
                                .line()
                                .line("DELETE /users/1 HTTP/1.1")
                                .line()
                                .closingDelimiter(),
                        List.of(BatchRequestItemBuilder.builder()
                                .method("DELETE").uri("/users/1")
                                .content("")
                                .create()),
                        false
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
                        ),
                        false
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
                        ),
                        false
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
                        ),
                        false
                ),
                Arguments.of(
                        BatchPayloadBuilder.builder()
                                .boundary("mmm")
                                .delimiter()
                                .line()
                                .line("GET /resources?what=this HTTP/1.1")
                                .line()
                                .closingDelimiter(),
                        List.of(
                                BatchRequestItemBuilder.builder()
                                        .method(null)
                                        .uri(null)
                                        .query(null)
                                        .create()
                        ),
                        false
                ),
                 Arguments.of(
                         BatchPayloadBuilder.builder()
                                 .boundary("bnd_9")
                                 .delimiter()
                                 .line()
                                 .line("Content-Type: application/http")
                                 .line("Content-Transfer-Encoding: binary")
                                 .line()
                                 .line("POST  /users HTTP/1.1")
                                 .line()
                                 .closingDelimiter(),
                         List.of(BatchRequestItemBuilder.builder()
                                 .method(null)
                                 .uri(null)
                                 .create()),
                         SKIP_TEST
                 ),
                // A query string should end before '#'
                 Arguments.of(
                         BatchPayloadBuilder.builder()
                                 .boundary("#boundary#")
                                 .delimiter()
                                 .line()
                                 .line("Content-Type: application/http")
                                 .line("Content-Transfer-Encoding: binary")
                                 .line()
                                 .line("DELETE /anyObjects? HTTP/1.1")
                                 .delimiter()
                                 .line()
                                 .line("DELETE /users?name=giuseppe&lastname=verdi#ignoreMe HTTP/1.1")
                                 .closingDelimiter(),
                         List.of(
                                 BatchRequestItemBuilder.builder()
                                         .method("DELETE")
                                         .uri("/anyObjects")
                                         .create(),
                                 BatchRequestItemBuilder.builder()
                                         .method("DELETE")
                                         .uri("/users")
                                         .query("name=giuseppe&lastname=verdi")
                                         .create()
                         ),
                         SKIP_TEST
                 ),
                // This test should pass because the second '?' should be treated as an integral part of the query,
                // but it doesn't because the parser doesn't handle the query string correctly.
                 Arguments.of(
                         BatchPayloadBuilder.builder()
                                 .boundary("multiple?question?marks")
                                 .delimiter()
                                 .line()
                                 .line("Content-Type: application/http")
                                 .line("Content-Transfer-Encoding: binary")
                                 .line()
                                 .line("PUT /resources?k1=v1?k2=v2 HTTP/1.1")
                                 .line()
                                 .closingDelimiter(),
                         List.of(
                                 BatchRequestItemBuilder.builder()
                                         .method("PUT")
                                         .uri("/resources")
                                         .query("k1=v1?k2=v2")
                                         .content("")
                                         .create()
                         ),
                         SKIP_TEST
                 ),
                 Arguments.of(
                         BatchPayloadBuilder.builder()
                                 .boundary("simple boundary")
                                 .delimiter()
                                 .line()
                                 .line("Content-Type: application/http")
                                 .line("Content-Transfer-Encoding: binary")
                                 .line()
                                 .line("POST HTTP/1.1") // Missing URI
                                 .closingDelimiter(),
                         List.of(BatchRequestItemBuilder.builder()
                                 .method(null)
                                 .uri(null)
                                 .line("POST HTTP/1.1")
                                 .create()),
                         SKIP_TEST
                 )
        );
    }

    @ParameterizedTest
    @MethodSource("httpRequestInputs")
    public void testHttpRequests(
            final BatchPayloadBuilder builder,
            final List<BatchRequestItem> expectedBatch) throws IOException {
        Assumptions.assumeFalse(SKIP_TEST);

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
                                .boundary("simple-boundary-1")
                                .delimiter()
                                .line()
                                .line("Content-Type: application/http")
                                .line("Content-Transfer-Encoding: binary")
                                .line()
                                .line("HTTP/1.1 200 OK")
                                .line("Content-Type:application json")
                                .line()
                                .text("{\"status\":\"success\"}")
                                .closingDelimiter(),
                        List.of(BatchResponseItemBuilder.builder()
                                .status(200)
                                .header("Content-Type", "application json")
                                .content("{\"status\":\"success\"}")
                                .create()),
                        false),
                Arguments.of(
                        BatchPayloadBuilder.builder()
                                .boundary("Bnd_Mixed_CaSe_99")
                                .delimiter()
                                .line()
                                .line("Content-Transfer-Encoding:\t\tbinary")
                                .line()
                                .line("HTTP/1.1 201 Created")
                                .line("Location:   /users/1   ")
                                .line()
                                .text("body")
                                .closingDelimiter(),
                        List.of(BatchResponseItemBuilder.builder()
                                .status(201)
                                .header("Location", "/users/1")
                                .content("body")
                                .create()),
                        false),
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
                                .create()),
                        false),
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
                                 .create()),
                         SKIP_TEST),
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
                                .create()),
                        false),
                Arguments.of(
                        BatchPayloadBuilder.builder()
                                .boundary("another-boundary-======")
                                .delimiter()
                                .line()
                                .line("Content-Type: application/http")
                                .line("Content-Transfer-Encoding: binary")
                                .line()
                                .line("HTTP/1.1 12K")
                                .line("body")
                                .closingDelimiter(),
                        List.of(
                                BatchResponseItemBuilder.builder()
                                        .content("body\r\n")
                                        .create()),
                        false),
                Arguments.of(
                        BatchPayloadBuilder.builder()
                                .boundary("another-boundary-======")
                                .delimiter()
                                .line()
                                .line("Content-Type: application/http")
                                .line("Content-Transfer-Encoding: binary")
                                .line()
                                .line("HTTP/1.1  200")
                                .text("body")
                                .closingDelimiter(),
                        List.of(
                                BatchResponseItemBuilder.builder()
                                        .content("body")
                                        .create()),
                        false),
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
                                .line("X-Tags:       tagA,       tagB")
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
                                        .create()),
                        false),
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
                                 .create()),
                         SKIP_TEST),
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
                                         .create()),
                         SKIP_TEST)
        );
    }

    @ParameterizedTest
    @MethodSource("httpResponseInputs")
    public void testHttpResponses(
            final BatchPayloadBuilder builder,
            final List<BatchResponseItem> expectedBatch) throws IOException {
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

    private MediaType mediaType(final String boundary) {
        MediaType t = mock(MediaType.class);
        when(t.getParameters()).thenReturn(Map.of("boundary", boundary));
        return t;
    }

    private void assertEquals(final BatchItem expected, final BatchItem actual) {
        Assertions.assertEquals(expected.getHeaders(), actual.getHeaders());
        Assertions.assertEquals(expected.getContent(), actual.getContent());
    }

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
}
