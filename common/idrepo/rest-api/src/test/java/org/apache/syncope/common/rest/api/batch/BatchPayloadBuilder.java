package org.apache.syncope.common.rest.api.batch;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BatchPayloadBuilder {
    private static final String[] MIME_TYPES = {
            "text/plain", "text/html", "application/json", "application/xml",
            "image/jpeg", "image/png", "audio/mpeg", "video/mp4",
            "application/octet-stream", "multipart/mixed"
    };

    private static final String[] ENCODINGS = {
            "7bit", "8bit", "binary", "quoted-printable", "base64", "x-custom-encoding"
    };

    private static final String[] DISPOSITIONS = {
            "inline", "attachment", "form-data"
    };
    private static final String EOL = "\n";
    private static final String CRLF = "\r\n";
    private static final String ALPHA = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String ALNUM = ALPHA + DIGITS;
    private static final String SPECIALS = "'()+_,-./:=?";
    private static final String BCHARSNOSPACE = ALPHA + DIGITS + SPECIALS;
    private static final String BCHARS = BCHARSNOSPACE + " ";
    private String boundary;
    private StringBuilder buffer = new StringBuilder();
    private StringBuilder partBuffer = new StringBuilder();
    private StringBuilder current;
    private List<String> parts = new ArrayList<>();
    private Random random;

    public static BatchPayloadBuilder builder() {
        return builder(123456789L);
    }

    public static BatchPayloadBuilder builder(long seed) {
        return new BatchPayloadBuilder(seed);
    }

    private BatchPayloadBuilder(long seed) {
        this.random = new Random(seed);
        current = buffer;
    }

    public byte[] create() {
        return create(StandardCharsets.US_ASCII);
    }

    public byte[] create(Charset charset) {
        return buffer.toString().getBytes(charset);
    }

    public String getBoundary() {
        return boundary;
    }

    public List<String> getParts() {
        return parts;
    }

    public BatchPayloadBuilder boundary(String boundary) {
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
        buffer.append(partBuffer.toString());
        current = buffer;
        return this;
    }

    public BatchPayloadBuilder delimiter() {
        return delimiter(0);
    }

    public BatchPayloadBuilder line() {
        return line(false);
    }

    public BatchPayloadBuilder line(boolean unixEol) {
        return line("", unixEol);
    }

    public BatchPayloadBuilder line(String line) {
        return line(line, false);
    }

    public BatchPayloadBuilder line(String line, boolean unixEol) {
        current.append(line);
        if (unixEol) {
            current.append("\n");
        } else {
            crlf(current);
        }
        return this;
    }

    public BatchPayloadBuilder text(String text) {
        current.append(text);
        return this;
    }

    public BatchPayloadBuilder text(int maxLength, int maxNumOfLines) {
        for (int i = 0; i < random.nextInt(maxNumOfLines); i++) {
            choice(current, BCHARS, random.nextInt(maxLength));
            current.append(EOL);
        }
        return this;
    }

    public BatchPayloadBuilder preamble(int maxLength, int maxNumOfLines) {
        int lines = random.nextInt(maxNumOfLines);
        if (lines > 0) {
            for (int i = 0; i < lines; i++) {
                choice(current, BCHARS, random.nextInt(maxLength));
                crlf(current, true);
            }
            crlf(current);
        }
        return this;
    }

    public BatchPayloadBuilder epilogue(int maxLength, int maxNumOfLines) {
        int lines = random.nextInt(maxNumOfLines);
        if (lines > 0) {
            crlf(current);
            for (int i = 0; i < lines; i++) {
                choice(current, BCHARS, random.nextInt(maxLength));
                crlf(current, true);
            }
        }
        return this;
    }

    public BatchPayloadBuilder delimiter(int padding) {
        crlf(current);
        dashBoundary(current);
        transportPadding(current, padding);
        return this;
    }

    public BatchPayloadBuilder closingDelimiter() {
        return closingDelimiter(0);
    }

    public BatchPayloadBuilder closingDelimiter(int padding) {
        crlf(current);
        dashBoundary(current);
        current.append("--");
        transportPadding(current, padding);
        return this;
    }

    public String toString() {
        return current.toString();
    }

    private void boundary(StringBuilder s) {
        s.append(this.boundary);
    }

    private void dashBoundary(StringBuilder s) {
        s.append("--");
        boundary(s);
    }

    private void transportPadding(StringBuilder s, int n) {
        String lws = " \t";
        choice(s, lws, n);
    }

    private void choice(StringBuilder s, String alphabet, int times) {
        for (int i = 0; i < times; i++) {
            s.append(choice(alphabet));
        }
    }

    private String choice(String alphabet) {
        return String.valueOf(alphabet.charAt(random.nextInt(alphabet.length())));
    }

    private void crlf(StringBuilder s) {
        crlf(s, false);
    }

    private void crlf(StringBuilder s, boolean unixEol) {
        if (unixEol) {
            s.append(EOL);
        } else {
            s.append(CRLF);
        }
    }
}
