package com.github.olga_yakovleva.rhvoice.android;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Testy obrony przed Zip Slip: wpis archiwum NIE moze wyprowadzic pliku poza
 * katalog docelowy. To bariera bezpieczenstwa, wiec kazdy przypadek jest
 * osobnym testem, a nie petla - zeby po awarii bylo widac, KTORY wzorzec przeszedl.
 */
public class SafeZipPathTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private File resolve(String entryName) throws IOException {
        return SafeZipPath.resolve(folder.getRoot(), entryName);
    }

    private void assertRejected(String entryName) {
        try {
            File result = resolve(entryName);
            fail("expected IOException for '" + entryName + "', got " + result);
        } catch (IOException expected) {
            // tak ma byc
        }
    }

    @Test
    public void acceptsPlainNestedPath() throws IOException {
        File target = resolve("data/16000/mgc.pdf");
        assertThat(target.getCanonicalPath())
                .isEqualTo(new File(folder.getRoot(), "data/16000/mgc.pdf").getCanonicalPath());
    }

    @Test
    public void acceptsFileAtRoot() throws IOException {
        File target = resolve("voice.info");
        assertThat(target.getParentFile().getCanonicalPath())
                .isEqualTo(folder.getRoot().getCanonicalPath());
    }

    @Test
    public void acceptsHarmlessSingleDotSegment() throws IOException {
        File target = resolve("./data/voice.info");
        assertThat(target.getCanonicalPath())
                .isEqualTo(new File(folder.getRoot(), "data/voice.info").getCanonicalPath());
    }

    @Test
    public void rejectsParentTraversal() {
        assertRejected("../../../etc/passwd");
    }

    @Test
    public void rejectsAbsolutePath() {
        assertRejected("/etc/passwd");
    }

    @Test
    public void rejectsTraversalHiddenInsidePath() {
        assertRejected("data/../../x");
    }

    @Test
    public void rejectsTraversalWrittenWithBackslashes() {
        // Separatory windowsowe normalizujemy jak ukosniki (patrz LocalVoiceLayout),
        // wiec ta sciezka rowniez ucieka z katalogu i musi byc odrzucona.
        assertRejected("data\\..\\..\\x");
    }

    @Test
    public void acceptsBackslashAsDirectorySeparator() throws IOException {
        File target = resolve("data\\16000\\mgc.pdf");
        assertThat(target.getCanonicalPath())
                .isEqualTo(new File(folder.getRoot(), "data/16000/mgc.pdf").getCanonicalPath());
    }

    @Test
    public void rejectsEmptyName() {
        assertRejected("");
    }

    @Test
    public void rejectsNameThatResolvesToTheDirectoryItself() {
        assertRejected("./");
    }

    @Test
    public void rejectsSiblingDirectoryWithSharedPrefix() {
        // Klasyczny blad implementacji: startsWith bez separatora przepuszcza
        // katalog "rootEvil" obok "root".
        assertRejected("../" + folder.getRoot().getName() + "Evil/x");
    }
}
