package com.github.olga_yakovleva.rhvoice.android;

import static com.google.common.truth.Truth.assertThat;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Test integracyjny na PRAWDZIWYM dodatku NVDA z glosem, nie na archiwum
 * zbudowanym w pamieci. Dowodzi, ze cala sciezka (rozpoznanie ukladu, parsowanie
 * voice.info, rozpakowanie z normalizacja) dziala na pliku, ktory realnie
 * dostarcza uzytkownik.
 *
 * Plik NIE jest trzymany w repozytorium (9,7 MB). Test jest POMIJANY, gdy nie
 * podano jego sciezki:
 *
 * ./gradlew :RHVoice-core:testStableDebugUnitTest --configure-on-demand \
 *   -Dkazek.addon=/home/tomecki/rhvoice_Android_custom_voices/RHVoice-voice-Polish-kazek-beta.nvda-addon
 */
public class RealAddonIntegrationTest {

    /** Nazwa property z sciezka do pliku dodatku. */
    private static final String PROPERTY = "kazek.addon";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private File addon;

    @Before
    public void requireAddonFile() {
        String path = System.getProperty(PROPERTY);
        Assume.assumeTrue("property -D" + PROPERTY + " not set", path != null && !path.isEmpty());
        addon = new File(path);
        Assume.assumeTrue("addon file not found: " + path, addon.isFile());
    }

    private static InputStream open(File file) throws IOException {
        return new BufferedInputStream(new FileInputStream(file));
    }

    private LocalVoiceLayout layout() throws IOException {
        InputStream in = open(addon);
        try {
            return LocalVoiceLayout.detect(in);
        } finally {
            in.close();
        }
    }

    @Test
    public void recognizesNvdaAddonLayout() throws IOException {
        LocalVoiceLayout layout = layout();
        assertThat(layout.isVoicePackage()).isTrue();
        assertThat(layout.getPrefix()).isEqualTo("data/");
    }

    @Test
    public void extractsVoiceAndReadsItsRealMetadata() throws IOException {
        File dir = folder.newFolder("kazek");
        LocalVoiceLayout layout = layout();
        List<String> extracted;
        InputStream in = open(addon);
        try {
            extracted = LocalVoiceExtractor.extract(in, layout, dir);
        } finally {
            in.close();
        }

        // Plikow w glosie jest duzo - jesli byloby ich kilka, znaczy ze sciezka
        // rozpakowania zgubila poddrzewo.
        assertThat(extracted.size()).isGreaterThan(30);

        assertThat(new File(dir, "voice.info").isFile()).isTrue();
        assertThat(new File(dir, "voice.params").isFile()).isTrue();
        assertThat(new File(dir, "16000/voice.data").isFile()).isTrue();
        assertThat(new File(dir, "24000/voice.data").isFile()).isTrue();

        InputStream info = open(new File(dir, "voice.info"));
        try {
            LocalVoiceInfo parsed = LocalVoiceInfo.parse(info);
            assertThat(parsed.getName()).isEqualTo("Kazek-Beta");
            assertThat(parsed.getLanguage()).isEqualTo("Polish");
            assertThat(parsed.getFormat()).isEqualTo(4);
            assertThat(parsed.getId()).isEqualTo("kazek_beta");
        } finally {
            info.close();
        }
    }

    @Test
    public void doesNotExtractAddonMetadataFilesAlongsideTheVoice() throws IOException {
        File dir = folder.newFolder("kazek");
        InputStream in = open(addon);
        try {
            LocalVoiceExtractor.extract(in, layout(), dir);
        } finally {
            in.close();
        }
        // manifest.ini i doc/ naleza do opakowania NVDA, nie do glosu.
        assertThat(new File(dir, "manifest.ini").exists()).isFalse();
        assertThat(new File(dir, "doc").exists()).isFalse();
    }
}
