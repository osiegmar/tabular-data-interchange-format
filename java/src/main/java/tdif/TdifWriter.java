package tdif;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class TdifWriter implements Closeable {

    private final Writer writer;
    private int fieldsPerRecord;

    public TdifWriter(final Writer writer) {
        this.writer = Objects.requireNonNull(writer, "writer must not be null");
    }

    public TdifWriter(final Path file) throws IOException {
        this(Files.newBufferedWriter(Objects.requireNonNull(file, "file must not be null")));
    }

    public TdifWriter writeHeader(final String... fields) throws IOException {
        if (fieldsPerRecord > 0) {
            throw new IllegalStateException("Header already written");
        }
        validateHeader(fields);

        writeInternal(fields);
        fieldsPerRecord = fields.length;
        return this;
    }

    private static void validateHeader(final String[] header) {
        requiresNonNullOrEmpty("Header", header);
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

    private static void requiresNonNullOrEmpty(final String name, final Object[] fields) {
        Objects.requireNonNull(fields, name + " must not be null");
        if (fields.length == 0) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
    }

    private void writeInternal(final Object[] fields) throws IOException {
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                writer.write(',');
            }

            writeField(fields[i]);
        }

        writer.write('\n');
    }

    private void writeField(final Object val) throws IOException {
        if (val == null) {
            writer.write("\\N");
        } else if (val instanceof Number || val instanceof Boolean) {
            writer.write(val.toString());
        } else {
            final String str = val.toString();
            writer.write('"');

            for (int i = 0; i < str.length(); i++) {
                final char ch = str.charAt(i);
                if (ch == '\\' | ch == '"') {
                    writer.write('\\');
                }
                writer.write(ch);
            }

            writer.write('"');
        }
    }

    public TdifWriter writeRecord(final Object... fields) throws IOException {
        requiresNonNullOrEmpty("Fields", fields);

        if (fieldsPerRecord == 0) {
            throw new IllegalStateException("Header not written");
        }

        if (fields.length != fieldsPerRecord) {
            throw new IllegalArgumentException("Expected " + fieldsPerRecord + " fields, got " + fields.length);
        }

        writeInternal(fields);
        return this;
    }

    public TdifWriter writeComment(final String comment) throws IOException {
        Objects.requireNonNull(comment, "comment must not be null");
        if (comment.indexOf('\r') != -1 || comment.indexOf('\n') != -1) {
            throw new IllegalArgumentException("comment must not contain line breaks");
        }
        writer.append('#').append(comment).append('\n');
        return this;
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

}
