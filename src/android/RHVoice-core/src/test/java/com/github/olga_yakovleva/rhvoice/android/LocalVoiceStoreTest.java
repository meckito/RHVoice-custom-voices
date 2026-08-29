package com.github.olga_yakovleva.rhvoice.android;

import static com.google.common.truth.Truth.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Testy rejestru glosow lokalnych. Zaleznosc od Androida jest odizolowana
 * interfejsem LocalVoiceDirs, wiec caly CRUD sprawdzamy na katalogu tymczasowym.
 */
public class LocalVoiceStoreTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private File root;
    private LocalVoiceStore store;

    @Before
    public void setUp() throws IOException {
        root = folder.newFolder("local-voices");
        store = new LocalVoiceStore(() -> root);
    }

    private LocalVoicePack kazek() {
        return new LocalVoicePack("kazek_beta", "Kazek-Beta", "Polish", true);
    }

    private LocalVoicePack alicja() {
        return new LocalVoicePack("alicja", "Alicja", "Polish", true);
    }

    /** Tworzy katalog glosu z plikiem, zeby mierzyc usuwanie danych z dysku. */
    private File installDirFor(LocalVoicePack pack) throws IOException {
        File dir = pack.getDir(root);
        new File(dir, "16000").mkdirs();
        Files.write(new File(dir, "voice.info").toPath(),
                "name=x\n".getBytes(StandardCharsets.UTF_8));
        Files.write(new File(dir, "16000/voice.data").toPath(),
                new byte[]{1, 2, 3});
        return dir;
    }

    @Test
    public void listIsEmptyWhenNothingWasImported() throws IOException {
        assertThat(store.list()).isEmpty();
    }

    @Test
    public void addedVoiceAppearsOnTheList() throws IOException {
        store.add(kazek());
        List<LocalVoicePack> list = store.list();
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getId()).isEqualTo("kazek_beta");
    }

    @Test
    public void registryFileSurvivesReload() throws IOException {
        store.add(kazek());
        LocalVoiceStore reopened = new LocalVoiceStore(() -> root);
        assertThat(reopened.list()).containsExactly(kazek());
    }

    @Test
    public void addingSameIdTwiceOverwritesInsteadOfDuplicating() throws IOException {
        store.add(kazek());
        store.add(new LocalVoicePack("kazek_beta", "Kazek-Beta", "Russian", false));
        List<LocalVoicePack> list = store.list();
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getLanguage()).isEqualTo("Russian");
        assertThat(list.get(0).isEnabled()).isFalse();
    }

    @Test
    public void listIsSortedByNameSoTheUiOrderIsStable() throws IOException {
        store.add(kazek());
        store.add(alicja());
        assertThat(store.list().get(0).getName()).isEqualTo("Alicja");
        assertThat(store.list().get(1).getName()).isEqualTo("Kazek-Beta");
    }

    @Test
    public void getReturnsVoiceById() throws IOException {
        store.add(kazek());
        assertThat(store.get("kazek_beta")).isEqualTo(kazek());
    }

    @Test
    public void getReturnsNullForUnknownId() throws IOException {
        assertThat(store.get("nie_ma")).isNull();
    }

    @Test
    public void setEnabledPersistsTheFlag() throws IOException {
        store.add(kazek());
        store.setEnabled("kazek_beta", false);
        assertThat(new LocalVoiceStore(() -> root).get("kazek_beta").isEnabled()).isFalse();
    }

    @Test
    public void enabledPathsSkipDisabledVoices() throws IOException {
        store.add(kazek());
        store.add(alicja());
        installDirFor(kazek());
        installDirFor(alicja());
        store.setEnabled("alicja", false);
        List<String> paths = store.getEnabledPaths();
        assertThat(paths).containsExactly(kazek().getDir(root).getAbsolutePath());
    }

    @Test
    public void enabledPathsSkipVoicesWhoseDataIsMissingFromDisk() throws IOException {
        // Wpis w rejestrze bez katalogu na dysku (np. po czyszczeniu danych apki)
        // NIE moze trafic do silnika - dostalby sciezke, ktora nie istnieje.
        store.add(kazek());
        assertThat(store.getEnabledPaths()).isEmpty();
    }

    @Test
    public void removeDropsTheEntry() throws IOException {
        store.add(kazek());
        store.remove("kazek_beta");
        assertThat(store.list()).isEmpty();
        assertThat(new LocalVoiceStore(() -> root).list()).isEmpty();
    }

    @Test
    public void removeDeletesVoiceDataFromDisk() throws IOException {
        store.add(kazek());
        File dir = installDirFor(kazek());
        assertThat(dir.exists()).isTrue();
        store.remove("kazek_beta");
        assertThat(dir.exists()).isFalse();
    }

    @Test
    public void removeOfUnknownIdIsHarmless() throws IOException {
        store.remove("nie_ma");
        assertThat(store.list()).isEmpty();
    }

    @Test
    public void removeLeavesOtherVoicesUntouched() throws IOException {
        store.add(kazek());
        store.add(alicja());
        File keep = installDirFor(alicja());
        installDirFor(kazek());
        store.remove("kazek_beta");
        assertThat(store.list()).containsExactly(alicja());
        assertThat(keep.exists()).isTrue();
    }

    @Test
    public void createsRegistryDirectoryOnDemand() throws IOException {
        File missing = new File(folder.getRoot(), "jeszcze/nie/ma");
        LocalVoiceStore lazyStore = new LocalVoiceStore(() -> missing);
        lazyStore.add(kazek());
        assertThat(new File(missing, LocalVoiceStore.REGISTRY_FILE_NAME).isFile()).isTrue();
    }

    @Test
    public void ignoresCorruptedRegistryLinesInsteadOfFailing() throws IOException {
        store.add(kazek());
        File registry = new File(root, LocalVoiceStore.REGISTRY_FILE_NAME);
        String content = new String(Files.readAllBytes(registry.toPath()), StandardCharsets.UTF_8);
        Files.write(registry.toPath(), (content + "zepsuty_wpis=bezpol\n").getBytes(StandardCharsets.UTF_8));
        // Dobry wpis musi przezyc obok zepsutego.
        assertThat(new LocalVoiceStore(() -> root).list()).containsExactly(kazek());
    }

    @Test
    public void readsRegistryWrittenWithNonAsciiNames() throws IOException {
        store.add(new LocalVoicePack("zazolc", "Zaz\u017c\u0142\u0107", "Polish", true));
        assertThat(new LocalVoiceStore(() -> root).get("zazolc").getName())
                .isEqualTo("Zaz\u017c\u0142\u0107");
    }
}
