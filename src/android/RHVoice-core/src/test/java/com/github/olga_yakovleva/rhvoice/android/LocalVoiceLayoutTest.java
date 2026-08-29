package com.github.olga_yakovleva.rhvoice.android;

import static com.google.common.truth.Truth.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.Test;

/**
 * Testy rozpoznawania ukladu paczki glosu w archiwum ZIP.
 *
 * Testy buduja archiwa W PAMIECI, wiec nie zaleza od plikow na dysku
 * ani od Androida - dzialaja jako zwykly test JVM.
 */
public class LocalVoiceLayoutTest {

    /** Buduje ZIP-a w pamieci z podanych nazw wpisow (wartosci sa nieistotne). */
    private static byte[] zipWith(String... entryNames) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(buf);
        for (String name : entryNames) {
            zip.putNextEntry(new ZipEntry(name));
            if (!name.endsWith("/"))
                zip.write(new byte[]{1, 2, 3});
            zip.closeEntry();
        }
        zip.close();
        return buf.toByteArray();
    }

    private static LocalVoiceLayout detect(byte[] zipBytes) throws IOException {
        return LocalVoiceLayout.detect(new ByteArrayInputStream(zipBytes));
    }

    @Test
    public void detectsVoiceAtArchiveRoot() throws IOException {
        LocalVoiceLayout layout = detect(zipWith(
                "voice.info",
                "16000/mgc.pdf",
                "lib/dur.pdf"));
        assertThat(layout.isVoicePackage()).isTrue();
        assertThat(layout.getPrefix()).isEqualTo("");
    }

    @Test
    public void detectsVoiceUnderDataDirectoryOfNvdaAddon() throws IOException {
        LocalVoiceLayout layout = detect(zipWith(
                "manifest.ini",
                "doc/en/readme.html",
                "data/voice.info",
                "data/16000/mgc.pdf"));
        assertThat(layout.isVoicePackage()).isTrue();
        assertThat(layout.getPrefix()).isEqualTo("data/");
    }

    @Test
    public void rejectsArchiveWithoutVoiceInfo() throws IOException {
        LocalVoiceLayout layout = detect(zipWith(
                "manifest.ini",
                "data/language.info",
                "data/g2p.fst"));
        assertThat(layout.isVoicePackage()).isFalse();
    }

    @Test
    public void picksShallowestVoiceInfoWhenSeveralExist() throws IOException {
        LocalVoiceLayout layout = detect(zipWith(
                "data/nested/other/voice.info",
                "data/voice.info",
                "data/16000/mgc.pdf"));
        assertThat(layout.getPrefix()).isEqualTo("data/");
    }

    @Test
    public void ignoresEntryWhoseNameOnlyEndsWithVoiceInfo() throws IOException {
        // "myvoice.info" nie jest plikiem "voice.info" - to inna nazwa.
        LocalVoiceLayout layout = detect(zipWith(
                "data/myvoice.info",
                "data/16000/mgc.pdf"));
        assertThat(layout.isVoicePackage()).isFalse();
    }

    @Test
    public void rejectsEmptyArchive() throws IOException {
        LocalVoiceLayout layout = detect(zipWith());
        assertThat(layout.isVoicePackage()).isFalse();
    }

    @Test
    public void treatsBackslashSeparatorsAsDirectorySeparators() throws IOException {
        // Niektore stare archiwizery zapisuja separator windowsowy.
        LocalVoiceLayout layout = detect(zipWith(
                "data\\voice.info",
                "data\\16000\\mgc.pdf"));
        assertThat(layout.isVoicePackage()).isTrue();
        assertThat(layout.getPrefix()).isEqualTo("data/");
    }
}
