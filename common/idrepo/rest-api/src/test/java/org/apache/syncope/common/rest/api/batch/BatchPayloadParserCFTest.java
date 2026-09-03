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
