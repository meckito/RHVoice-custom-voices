package com.github.olga_yakovleva.rhvoice.android;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * TESTY NEGATYWNE (Zadanie 21 planu).
 *
 * Cel nie jest kosmetyczny: dopoki nie widzimy, ze import UMIE ODMOWIC, dopoty
 * "udany import" niczego nie dowodzi - moglby po prostu zawsze konczyc sie
 * sukcesem. Kazdy przypadek sprawdza TRZY rzeczy naraz:
 * 1. blad jest zgloszony (a nie przemilczany),
 * 2. rejestr zostaje pusty,
 * 3. na dysku nie zostaje katalog-smiec.
 */
public class LocalVoiceInstallerNegativeTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private File root;
    private LocalVoiceStore store;
    private LocalVoiceInstaller installer;

    @Before
    public void setUp() throws IOException {
        root = folder.newFolder("local-voices");
        store = new LocalVoiceStore(() -> root);
        // Na tym "urzadzeniu" zainstalowany jest TYLKO polski.
        installer = new LocalVoiceInstaller(store,
                name -> Collections.singletonList("Polish").contains(name));
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

    private void install(byte[] data) throws IOException {
        installer.install(new ByteArrayInputStream(data));
    }

    /** Nic nie zostalo zapisane ani na dysku, ani w rejestrze. */
    private void assertNothingInstalled() throws IOException {
        assertThat(store.list()).isEmpty();
        assertThat(store.getEnabledPaths()).isEmpty();
        assertThat(installer.listTempLeftovers()).isEmpty();
        File[] children = root.listFiles();
        if (children != null) {
            for (File child : children) {
                // Dozwolony jest tylko plik rejestru; zaden katalog glosu.
                assertThat(child.isDirectory()).isFalse();
            }
        }
    }

    @Test
    public void refusesZipThatIsNotAVoicePackage() throws IOException {
        byte[] data = zip("notatki.txt", "to nie jest glos", "obrazek.png", "PNG");
        try {
            install(data);
            fail("expected IOException for an archive without voice.info");
        } catch (IOException expected) {
            assertThat(expected.getMessage()).contains("not a voice package");
        }
        assertNothingInstalled();
    }

    @Test
    public void refusesVoiceWhoseLanguageIsNotInstalledAndNamesThatLanguage()
            throws IOException {
        byte[] data = zip(
                "manifest.ini", "[addon]\n",
                "data/voice.info",
                "name=Anna-Test\nlanguage=Russian\ngender=female\nformat=4\nrevision=0\n",
                "data/16000/voice.data", "DATA");
        try {
            install(data);
            fail("expected MissingLanguageException");
        } catch (LocalVoiceInstaller.MissingLanguageException e) {
            // Nazwa jezyka jest potrzebna, by powiedziec uzytkownikowi CO doinstalowac.
            assertThat(e.getLanguage()).isEqualTo("Russian");
        }
        assertNothingInstalled();
    }

    @Test
    public void refusesFileThatIsNotAnArchiveAtAll() throws IOException {
        byte[] garbage = "to nie jest archiwum ZIP, tylko smieci".getBytes(StandardCharsets.UTF_8);
        try {
            install(garbage);
            fail("expected IOException for a non-archive file");
        } catch (IOException expected) {
            // dowolny blad wejscia-wyjscia jest tu poprawny
        }
        assertNothingInstalled();
    }

    @Test
    public void refusesEmptyFile() throws IOException {
        try {
            install(new byte[0]);
            fail("expected IOException for an empty file");
        } catch (IOException expected) {
            // tak ma byc
        }
        assertNothingInstalled();
    }

    @Test
    public void refusesPackageWithVoiceInfoMissingRequiredFields() throws IOException {
        byte[] data = zip("data/voice.info", "gender=male\nformat=4\n");
        try {
            install(data);
            fail("expected IOException for voice.info without name/language");
        } catch (IOException expected) {
            assertThat(expected.getMessage()).contains("voice.info");
        }
        assertNothingInstalled();
    }

    @Test
    public void refusesLanguagePackageInsteadOfVoicePackage() throws IOException {
        // Paczka JEZYKA ma language.info, nie voice.info - import glosu musi ja odrzucic.
        byte[] data = zip(
                "manifest.ini", "[addon]\n",
                "data/language.info", "name=Polish\nformat=4\nrevision=0\n",
                "data/g2p.fst", "FST");
        try {
            install(data);
            fail("expected IOException for a language package");
        } catch (IOException expected) {
            assertThat(expected.getMessage()).contains("not a voice package");
        }
        assertNothingInstalled();
    }
}
