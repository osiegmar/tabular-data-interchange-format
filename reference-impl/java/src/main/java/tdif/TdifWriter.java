package tdif;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class TdifWriter implements Closeable {

    private static final char CR = '\r';
    private static final char LF = '\n';
    private static final char FIELD_ENCLOSURE = '"';
    private static final char FIELD_SEPARATOR = ',';
    private static final char COMMENT_START = '#';
    private static final String NULL = "\\N";
    private static final String OS_LS = System.lineSeparator();

    private final Writer writer;
    private int fieldsPerRecord;

    public TdifWriter(final Writer writer) {
        this.writer = Objects.requireNonNull(writer, "writer must not be null");
    }

    public TdifWriter(final Path file) throws IOException {
        writer = Files.newBufferedWriter(Objects.requireNonNull(file, "file must not be null"));
    }

    public TdifWriter writeHeader(final String... fields) throws IOException {
        Objects.requireNonNull(fields, "Header must not be null");
        if (fields.length == 0) {
            throw new IllegalArgumentException("Header must not be empty");
        }
        if (fieldsPerRecord > 0) {
            throw new IllegalStateException("Header already written");
        }
        validateHeaderFields(fields);

        writeFields(fields);
        fieldsPerRecord = fields.length;
        return this;
    }

    private static void validateHeaderFields(final String[] header) {
        for (int i = 0; i < header.length; i++) {
            if (header[i] == null) {
                throw new NullPointerException("Header must not contain null");
            }
            if (header[i].isEmpty()) {
                throw new IllegalArgumentException("Header must not contain empty string");
            }
            for (int j = i + 1; j < header.length; j++) {
                if (header[i].equalsIgnoreCase(header[j])) {
                    throw new IllegalArgumentException("Duplicate header: " + header[i]);
                }
            }
        }
    }

    private void writeFields(final Object[] fields) throws IOException {
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                writer.write(FIELD_SEPARATOR);
            }

            writeField(fields[i]);
        }

        writer.write(OS_LS);
    }

    private void writeField(final Object val) throws IOException {
        if (val == null) {
            writer.write(NULL);
            return;
        }

        writer.write(FIELD_ENCLOSURE);

        if (val instanceof Number || val instanceof Boolean) {
            writer.write(val.toString());
        } else {
            writeEscapedString(val.toString());
        }

        writer.write(FIELD_ENCLOSURE);
    }

    private void writeEscapedString(final String str) throws IOException {
        for (int i = 0; i < str.length(); i++) {
            final char ch = str.charAt(i);
            if (ch == FIELD_ENCLOSURE) {
                writer.write(FIELD_ENCLOSURE);
            }
            writer.write(ch);
        }
    }

    public TdifWriter writeRecord(final Object... fields) throws IOException {
        Objects.requireNonNull(fields, "fields must not be null");

        if (fieldsPerRecord == 0) {
            throw new IllegalStateException("Header not written");
        }

        if (fields.length != fieldsPerRecord) {
            throw new IllegalArgumentException("Expected %d fields, got %d".formatted(fieldsPerRecord, fields.length));
        }

        writeFields(fields);
        return this;
    }

    public TdifWriter writeComment(final String comment) throws IOException {
        Objects.requireNonNull(comment, "comment must not be null");
        if (comment.indexOf(CR) != -1 || comment.indexOf(LF) != -1) {
            throw new IllegalArgumentException("comment must not contain line breaks");
        }
        writer.append(COMMENT_START).append(comment).append(OS_LS);
        return this;
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

}
