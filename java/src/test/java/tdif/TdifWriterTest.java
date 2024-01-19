package tdif;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TdifWriterTest {

    private final StringWriter sw = new StringWriter();
    private final TdifWriter w = new TdifWriter(sw);

    @Test
    void writeHeader() throws IOException {
        w.writeHeader("foo", "bar");
        assertThat(sw).hasToString("\"foo\",\"bar\"\n");
    }

    @Test
    void writeNullHeader() {
        assertThatThrownBy(() -> w.writeHeader("foo", null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Header must not contain null");
    }

    @Test
    void writeEmptyHeader() {
        assertThatThrownBy(w::writeHeader)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Header must not be empty");
    }

    @Test
    void writeEmptyFieldHeader() {
        assertThatThrownBy(() -> w.writeHeader("foo", ""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Header must not contain empty string");
    }

    @Test
    void writeHeaderTwice() {
        assertThatThrownBy(() -> w.writeHeader("foo").writeHeader("bar"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Header already written");
    }

    @Test
    void writeRecordBeforeHeader() {
        assertThatThrownBy(() -> w.writeRecord("foo"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Header not written");
    }

    @Test
    void writeRecord() throws IOException {
        w.writeHeader("h1", "h2", "h3", "h4")
            .writeRecord("foo", "", 42, null);

        assertThat(sw).hasToString("\"h1\",\"h2\",\"h3\",\"h4\"\n\"foo\",\"\",42,\\N\n");
    }

    @Test
    void writeMultilineRecord() throws IOException {
        w.writeHeader("h1", "h2")
            .writeRecord("foo", "foo \"is\" bar");

        assertThat(sw).hasToString("\"h1\",\"h2\"\n\"foo\",\"foo \\\"is\\\" bar\"\n");
    }

    @Test
    void writeRecordWithWrongFieldCount() {
        assertThatThrownBy(() -> w.writeHeader("header1", "header2").writeRecord("foo"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Expected 2 fields, got 1");
    }

    @Test
    void writeDuplicateHeader() {
        assertThatThrownBy(() -> w.writeHeader("foo", "FOO"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Duplicate header: foo");
    }

    @Test
    void writeComment() throws IOException {
        w.writeComment("foo");
        assertThat(sw).hasToString("#foo\n");
    }

    @Test
    void writeCommentWithNullInput() {
        assertThatThrownBy(() -> w.writeComment(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("comment must not be null");
    }

    @ParameterizedTest
    @ValueSource(strings = { "foo\n", "foo\r", "foo\r\n" })
    void writeCommentWithIllegalInput(final String str) {
        assertThatThrownBy(() -> w.writeComment(str))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("comment must not contain line breaks");
    }

    @Test
    void file(@TempDir final Path tmpDir) throws IOException {
        final Path file = tmpDir.resolve("foo.tdif");
        try (var fw = new TdifWriter(file)) {
            fw.writeComment("1st comment")
                .writeHeader("h1")
                .writeComment("2nd comment")
                .writeRecord("foo")
                .writeComment("3rd comment");
        }

        assertThat(file).hasContent("""
            #1st comment
            "h1"
            #2nd comment
            "foo"
            #3rd comment
            """);
    }

}
