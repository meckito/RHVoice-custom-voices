package com.github.olga_yakovleva.rhvoice.android;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

/** Testy parsowania pliku voice.info z paczki glosu. */
public class LocalVoiceInfoTest {

    private static LocalVoiceInfo parse(String content) throws IOException {
        InputStream in = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        return LocalVoiceInfo.parse(in);
    }

    /** Rzeczywista tresc voice.info z badanego dodatku NVDA. */
    private static final String KAZEK_BETA =
            "name=Kazek-Beta\n"
            + "language=Polish\n"
            + "gender=male\n"
            + "format=4\n"
            + "revision=0\n";

    @Test
    public void parsesRealVoiceInfoFromExaminedAddon() throws IOException {
        LocalVoiceInfo info = parse(KAZEK_BETA);
        assertThat(info.getName()).isEqualTo("Kazek-Beta");
        assertThat(info.getLanguage()).isEqualTo("Polish");
        assertThat(info.getGender()).isEqualTo("male");
        assertThat(info.getFormat()).isEqualTo(4);
        assertThat(info.getRevision()).isEqualTo(0);
    }

    @Test
    public void derivesIdTheSameWayAsUpstreamTtsResource() throws IOException {
        // TtsResource.getId(): name.toLowerCase().replace("-", "_")
        assertThat(parse(KAZEK_BETA).getId()).isEqualTo("kazek_beta");
    }

    @Test
    public void toleratesWindowsLineEndingsAndSurroundingWhitespace() throws IOException {
        LocalVoiceInfo info = parse(
                "name = Kazek-Beta \r\n"
                + "language=Polish\r\n"
                + "format=4\r\n"
                + "revision=7\r\n");
        assertThat(info.getName()).isEqualTo("Kazek-Beta");
        assertThat(info.getRevision()).isEqualTo(7);
    }

    @Test
    public void readsNonAsciiNamesAsUtf8() throws IOException {
        LocalVoiceInfo info = parse(
                "name=Zaz\u017ca\u0142\u0107\n"
                + "language=Polish\n"
                + "format=4\n"
                + "revision=0\n");
        assertThat(info.getName()).isEqualTo("Zaz\u017ca\u0142\u0107");
    }

    @Test
    public void defaultsRevisionToZeroWhenAbsent() throws IOException {
        LocalVoiceInfo info = parse(
                "name=Kazek\nlanguage=Polish\nformat=4\n");
        assertThat(info.getRevision()).isEqualTo(0);
    }

    @Test
    public void genderIsEmptyWhenAbsentRatherThanNull() throws IOException {
        LocalVoiceInfo info = parse(
                "name=Kazek\nlanguage=Polish\nformat=4\n");
        assertThat(info.getGender()).isEmpty();
    }

    @Test
    public void rejectsMissingName() {
        assertRejected("language=Polish\nformat=4\n");
    }

    @Test
    public void rejectsMissingLanguage() {
        assertRejected("name=Kazek\nformat=4\n");
    }

    @Test
    public void rejectsMissingFormat() {
        assertRejected("name=Kazek\nlanguage=Polish\n");
    }

    @Test
    public void rejectsBlankName() {
        assertRejected("name=   \nlanguage=Polish\nformat=4\n");
    }

    @Test
    public void rejectsNonNumericFormat() {
        assertRejected("name=Kazek\nlanguage=Polish\nformat=cztery\n");
    }

    @Test
    public void rejectsNonNumericRevision() {
        assertRejected("name=Kazek\nlanguage=Polish\nformat=4\nrevision=x\n");
    }

    private static void assertRejected(String content) {
        try {
            LocalVoiceInfo info = parse(content);
            fail("expected IOException, got " + info.getName());
        } catch (IOException expected) {
            // tak ma byc
        }
    }
}
