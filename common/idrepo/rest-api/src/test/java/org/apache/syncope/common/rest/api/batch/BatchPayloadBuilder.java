package org.apache.syncope.common.rest.api.batch;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class BatchPayloadBuilder {
    private static final String EOL = "\n";
    private static final String CRLF = "\r\n";
    private static final String ALPHA = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String SPECIALS = "'()+_,-./:=?";
    private static final String BCHARSNOSPACE = ALPHA + DIGITS + SPECIALS;
    private static final String BCHARS = BCHARSNOSPACE + " ";
    private String boundary;
    private final StringBuilder buffer = new StringBuilder();
    private final StringBuilder partBuffer = new StringBuilder();
    private StringBuilder current;
    private final List<String> parts = new ArrayList<>();
    private final Random random;

    public static BatchPayloadBuilder builder() {
        return builder(123456789L);
    }

    public static BatchPayloadBuilder builder(final long seed) {
        return new BatchPayloadBuilder(seed);
    }

    private BatchPayloadBuilder(final long seed) {
        this.random = new Random(seed);
        current = buffer;
    }

    public byte[] create() {
        return create(StandardCharsets.US_ASCII);
    }

    public byte[] create(final Charset charset) {
        return buffer.toString().getBytes(charset);
    }

    public String getBoundary() {
        return boundary;
    }

    public List<String> getParts() {
        return parts;
    }

    public BatchPayloadBuilder boundary(final String boundary) {
        this.boundary = boundary;
        return this;
    }

    public BatchPayloadBuilder beginPart() {
        if (current == partBuffer) {
            throw new IllegalStateException();
        }
        partBuffer.setLength(0);
        current = partBuffer;
        return this;
    }

    public BatchPayloadBuilder endPart() {
        if (current == buffer) {
            throw new IllegalStateException();
        }
        parts.add(partBuffer.toString());
        buffer.append(partBuffer);
        current = buffer;
        return this;
    }

    public BatchPayloadBuilder delimiter() {
        return delimiter(0);
    }

    public BatchPayloadBuilder line() {
        return line(false);
    }

    public BatchPayloadBuilder line(final boolean unixEol) {
        return line("", unixEol);
    }

    public BatchPayloadBuilder line(final String line) {
        return line(line, false);
    }

    public BatchPayloadBuilder line(final String line, final boolean unixEol) {
        current.append(line);
        if (unixEol) {
            current.append("\n");
        } else {
            crlf(current);
        }
        return this;
    }

    public BatchPayloadBuilder text(final String text) {
        current.append(text);
        return this;
    }

    public BatchPayloadBuilder text(final int maxLength, final int maxNumOfLines) {
        for (int i = 0; i < random.nextInt(maxNumOfLines); i++) {
            choice(current, BCHARS, random.nextInt(maxLength));
            current.append(EOL);
        }
        return this;
    }

    public BatchPayloadBuilder delimiter(final int padding) {
        crlf(current);
        dashBoundary(current);
        transportPadding(current, padding);
        return this;
    }

    public BatchPayloadBuilder closingDelimiter() {
        return closingDelimiter(0);
    }

    public BatchPayloadBuilder closingDelimiter(final int padding) {
        crlf(current);
        dashBoundary(current);
        current.append("--");
        transportPadding(current, padding);
        return this;
    }

    public String toString() {
        return current.toString();
    }

    private void boundary(final StringBuilder s) {
        s.append(this.boundary);
    }

    private void dashBoundary(final StringBuilder s) {
        s.append("--");
        boundary(s);
    }

    private void transportPadding(final StringBuilder s, final int n) {
        String lws = " \t";
        choice(s, lws, n);
    }

    private void choice(final StringBuilder s, final String alphabet, final int times) {
        for (int i = 0; i < times; i++) {
            s.append(choice(alphabet));
        }
    }

    private String choice(final String alphabet) {
        return String.valueOf(alphabet.charAt(random.nextInt(alphabet.length())));
    }

    private void crlf(final StringBuilder s) {
        crlf(s, false);
    }

    private void crlf(final StringBuilder s, final boolean unixEol) {
        if (unixEol) {
            s.append(EOL);
        } else {
            s.append(CRLF);
        }
    }
}
