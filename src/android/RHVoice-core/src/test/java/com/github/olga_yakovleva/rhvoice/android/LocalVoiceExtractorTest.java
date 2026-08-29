package com.github.olga_yakovleva.rhvoice.android;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Testy rozpakowania paczki glosu z normalizacja ukladu. */
public class LocalVoiceExtractorTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    /** Buduje ZIP-a w pamieci: kolejne pary nazwa, tresc. */
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

    private static final String INFO =
            "name=Kazek-Beta\nlanguage=Polish\ngender=male\nformat=4\nrevision=0\n";

    /** Uklad dodatku NVDA: glos w data/, obok niego rzeczy nienalezace do glosu. */
    private static byte[] nvdaAddon() throws IOException {
        return zip(
                "manifest.ini", "[addon]\nname=RHVoice-voice-Polish-Kazek\n",
                "doc/en/readme.html", "<html/>",
                "data/voice.info", INFO,
                "data/16000/mgc.pdf", "MGC",
                "langdata/g2p.fst", "FST");
    }

    private List<String> extract(byte[] archive, File dir) throws IOException {
        LocalVoiceLayout layout = LocalVoiceLayout.detect(new ByteArrayInputStream(archive));
        return LocalVoiceExtractor.extract(new ByteArrayInputStream(archive), layout, dir);
    }

    /** Wszystkie pliki w katalogu, sciezkami relatywnymi, posortowane. */
    private static List<String> filesIn(File dir) throws IOException {
        List<String> found = new ArrayList<>();
        collect(dir, dir, found);
        Collections.sort(found);
        return found;
    }

    private static void collect(File root, File current, List<String> found) throws IOException {
        File[] children = current.listFiles();
        if (children == null)
            return;
        for (File child : children) {
            if (child.isDirectory())
                collect(root, child, found);
            else
                found.add(root.toPath().relativize(child.toPath()).toString().replace('\\', '/'));
        }
    }

    @Test
    public void extractsOnlyVoiceSubtreeAndStripsPrefix() throws IOException {
        File dir = folder.newFolder("out");
        extract(nvdaAddon(), dir);
        assertThat(filesIn(dir)).containsExactly("voice.info", "16000/mgc.pdf");
    }

    @Test
    public void putsVoiceInfoAtTheRootOfTargetDirectory() throws IOException {
        File dir = folder.newFolder("out");
        extract(nvdaAddon(), dir);
        File info = new File(dir, "voice.info");
        assertThat(info.isFile()).isTrue();
        assertThat(new String(Files.readAllBytes(info.toPath()), StandardCharsets.UTF_8))
                .isEqualTo(INFO);
    }

    @Test
    public void returnsListOfExtractedRelativePaths() throws IOException {
        File dir = folder.newFolder("out");
        List<String> extracted = extract(nvdaAddon(), dir);
        assertThat(extracted).containsExactly("voice.info", "16000/mgc.pdf");
    }

    @Test
    public void extractsAndroidStylePackageWithVoiceAtRoot() throws IOException {
        File dir = folder.newFolder("out");
        byte[] archive = zip(
                "voice.info", INFO,
                "16000/mgc.pdf", "MGC",
                "voice.params", "params");
        extract(archive, dir);
        assertThat(filesIn(dir)).containsExactly("voice.info", "16000/mgc.pdf", "voice.params");
    }

    @Test
    public void createsMissingIntermediateDirectories() throws IOException {
        File dir = new File(folder.getRoot(), "nie/ma/takiego");
        byte[] archive = zip("voice.info", INFO, "24000/deep/nested/file.bin", "X");
        extract(archive, dir);
        assertThat(new File(dir, "24000/deep/nested/file.bin").isFile()).isTrue();
    }

    @Test
    public void copiesFileContentByteForByte() throws IOException {
        File dir = folder.newFolder("out");
        StringBuilder big = new StringBuilder();
        for (int i = 0; i < 20000; i++)
            big.append((char) ('a' + (i % 26)));
        String payload = big.toString();
        extract(zip("voice.info", INFO, "16000/voice.data", payload), dir);
        assertThat(new String(Files.readAllBytes(new File(dir, "16000/voice.data").toPath()),
                StandardCharsets.UTF_8)).isEqualTo(payload);
    }

    @Test
    public void skipsEntriesThatEscapeTargetDirectory() throws IOException {
        File dir = folder.newFolder("out");
        byte[] archive = zip(
                "data/voice.info", INFO,
                "data/../../../evil.txt", "PWNED");
        try {
            extract(archive, dir);
            fail("expected IOException for a Zip Slip entry");
        } catch (IOException expected) {
            // paczka z wroga sciezka musi byc odrzucona w calosci
        }
        assertThat(new File(folder.getRoot().getParentFile(), "evil.txt").exists()).isFalse();
    }

    @Test
    public void rejectsArchiveThatIsNotAVoicePackage() throws IOException {
        File dir = folder.newFolder("out");
        byte[] archive = zip("manifest.ini", "x", "data/language.info", "y");
        try {
            extract(archive, dir);
            fail("expected IOException for an archive without voice.info");
        } catch (IOException expected) {
            // tak ma byc
        }
    }

    @Test
    public void ignoresDirectoryEntriesOutsideThePrefix() throws IOException {
        File dir = folder.newFolder("out");
        byte[] archive = zip(
                "doc/", "",
                "data/voice.info", INFO);
        extract(archive, dir);
        assertThat(filesIn(dir)).containsExactly("voice.info");
        assertThat(new File(dir, "doc").exists()).isFalse();
    }
}
