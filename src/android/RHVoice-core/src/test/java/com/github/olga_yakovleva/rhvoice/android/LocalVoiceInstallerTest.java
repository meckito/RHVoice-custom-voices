package com.github.olga_yakovleva.rhvoice.android;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Testy calego przebiegu instalacji glosu z pliku: sprawdzenie paczki,
 * wymaganego jezyka, rozpakowanie do katalogu tymczasowego, atomowe wstawienie
 * na miejsce docelowe i zapis do rejestru.
 */
public class LocalVoiceInstallerTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private File root;
    private LocalVoiceStore store;
    private LocalVoiceInstaller installer;

    /** Jezyki "zainstalowane" w tescie. */
    private List<String> languages = Collections.singletonList("Polish");

    @Before
    public void setUp() throws IOException {
        root = folder.newFolder("local-voices");
        store = new LocalVoiceStore(() -> root);
        installer = new LocalVoiceInstaller(store, name -> languages.contains(name));
    }

    private static byte[] zip(String... nameThenContent) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        ZipOutputStream out = new ZipOutputStream(buf);
        for (int i = 0; i < nameThenContent.length; i += 2) {
            out.putNextEntry(new ZipEntry(nameThenContent[i]));
            out.write(nameThenContent[i + 1].getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
        out.close();
        return buf.toByteArray();
    }

    private static String info(String name, String language) {
        return "name=" + name + "\nlanguage=" + language + "\ngender=male\nformat=4\nrevision=0\n";
    }

    private static byte[] addon(String name, String language) throws IOException {
        return zip(
                "manifest.ini", "[addon]\n",
                "data/voice.info", info(name, language),
                "data/16000/voice.data", "DATA");
    }

    private LocalVoicePack install(byte[] archive) throws IOException {
        return installer.install(new ByteArrayInputStream(archive));
    }

    @Test
    public void installsVoiceFromNvdaAddon() throws IOException {
        LocalVoicePack pack = install(addon("Kazek-Beta", "Polish"));
        assertThat(pack.getId()).isEqualTo("kazek_beta");
        assertThat(pack.getName()).isEqualTo("Kazek-Beta");
        assertThat(pack.getLanguage()).isEqualTo("Polish");
    }

    @Test
    public void placesVoiceDataInItsOwnDirectoryWithInfoAtRoot() throws IOException {
        LocalVoicePack pack = install(addon("Kazek-Beta", "Polish"));
        File dir = pack.getDir(root);
        assertThat(new File(dir, "voice.info").isFile()).isTrue();
        assertThat(new File(dir, "16000/voice.data").isFile()).isTrue();
    }

    @Test
    public void registersInstalledVoice() throws IOException {
        install(addon("Kazek-Beta", "Polish"));
        assertThat(store.get("kazek_beta")).isNotNull();
        assertThat(store.get("kazek_beta").isEnabled()).isTrue();
    }

    @Test
    public void installedVoicePathIsHandedToTheEngine() throws IOException {
        LocalVoicePack pack = install(addon("Kazek-Beta", "Polish"));
        assertThat(store.getEnabledPaths())
                .containsExactly(pack.getDir(root).getAbsolutePath());
    }

    @Test
    public void refusesVoiceWhoseLanguageIsNotInstalled() throws IOException {
        try {
            install(addon("Anna", "Russian"));
            fail("expected LocalVoiceInstaller.MissingLanguageException");
        } catch (LocalVoiceInstaller.MissingLanguageException e) {
            assertThat(e.getLanguage()).isEqualTo("Russian");
        }
        assertThat(store.list()).isEmpty();
    }

    @Test
    public void leavesNothingBehindWhenLanguageIsMissing() throws IOException {
        try {
            install(addon("Anna", "Russian"));
            fail("expected MissingLanguageException");
        } catch (LocalVoiceInstaller.MissingLanguageException expected) {
            // sprawdzamy skutki ubocznie
        }
        assertThat(new File(root, "anna").exists()).isFalse();
        assertThat(installer.listTempLeftovers()).isEmpty();
    }

    @Test
    public void refusesArchiveThatIsNotAVoicePackage() throws IOException {
        try {
            install(zip("manifest.ini", "x", "data/language.info", "y"));
            fail("expected IOException");
        } catch (IOException expected) {
            // tak ma byc
        }
        assertThat(store.list()).isEmpty();
    }

    @Test
    public void reinstallReplacesPreviousDataInsteadOfMerging() throws IOException {
        install(zip(
                "data/voice.info", info("Kazek-Beta", "Polish"),
                "data/16000/voice.data", "STARE",
                "data/stary_plik.bin", "X"));
        LocalVoicePack pack = install(zip(
                "data/voice.info", info("Kazek-Beta", "Polish"),
                "data/16000/voice.data", "NOWE"));
        File dir = pack.getDir(root);
        assertThat(new File(dir, "stary_plik.bin").exists()).isFalse();
        assertThat(new String(java.nio.file.Files.readAllBytes(
                new File(dir, "16000/voice.data").toPath()), StandardCharsets.UTF_8))
                .isEqualTo("NOWE");
        assertThat(store.list()).hasSize(1);
    }

    @Test
    public void reinstallKeepsVoiceDisabledIfUserDisabledItBefore() throws IOException {
        install(addon("Kazek-Beta", "Polish"));
        store.setEnabled("kazek_beta", false);
        install(addon("Kazek-Beta", "Polish"));
        // Ponowny import nie moze po cichu wlaczyc glosu, ktory uzytkownik wylaczyl.
        assertThat(store.get("kazek_beta").isEnabled()).isFalse();
    }

    @Test
    public void rejectsMaliciousPathsAndInstallsNothing() throws IOException {
        try {
            install(zip(
                    "data/voice.info", info("Kazek-Beta", "Polish"),
                    "data/../../../evil.txt", "PWNED"));
            fail("expected IOException");
        } catch (IOException expected) {
            // tak ma byc
        }
        assertThat(store.list()).isEmpty();
        assertThat(new File(root, "kazek_beta").exists()).isFalse();
        assertThat(installer.listTempLeftovers()).isEmpty();
    }

    @Test
    public void previousVoiceSurvivesFailedReinstall() throws IOException {
        install(addon("Kazek-Beta", "Polish"));
        try {
            install(zip(
                    "data/voice.info", info("Kazek-Beta", "Polish"),
                    "data/../../../evil.txt", "PWNED"));
            fail("expected IOException");
        } catch (IOException expected) {
            // stary glos musi zostac nietkniety
        }
        assertThat(new File(root, "kazek_beta/voice.info").isFile()).isTrue();
        assertThat(store.get("kazek_beta")).isNotNull();
    }

    @Test
    public void cleansUpTemporaryDirectoryAfterSuccess() throws IOException {
        install(addon("Kazek-Beta", "Polish"));
        assertThat(installer.listTempLeftovers()).isEmpty();
    }

    @Test
    public void installsSecondVoiceAlongsideTheFirst() throws IOException {
        languages = Arrays.asList("Polish", "Russian");
        install(addon("Kazek-Beta", "Polish"));
        install(addon("Anna", "Russian"));
        assertThat(store.list()).hasSize(2);
        assertThat(store.getEnabledPaths()).hasSize(2);
    }
}
