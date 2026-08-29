/* Copyright (C) 2026  Tomek (fork RHVoice-custom-voices)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>. */

package com.github.olga_yakovleva.rhvoice.android;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

/**
 * Rejestr glosow zainstalowanych z pliku.
 *
 * Stan trzymany jest we WLASNYM pliku {@code local-voices.properties} w katalogu
 * glosow lokalnych, a nie w {@code SharedPreferences} - dzieki temu cala klasa
 * jest testowalna na JVM bez Androida (zaleznosc od {@code Context} jest schowana
 * za {@link LocalVoiceDirs}).
 *
 * Katalog glosow lokalnych jest CELOWO osobny od {@code getDir("data", 0)}:
 * {@code DataPack.cleanup()} usuwa z katalogu danych wszystko, co pasuje do nazwy
 * paczki, i nie moze przypadkiem skasowac glosu uzytkownika.
 */
public final class LocalVoiceStore {

    /** Nazwa pliku rejestru w katalogu glosow lokalnych. */
    public static final String REGISTRY_FILE_NAME = "local-voices.properties";

    /** Nazwa katalogu glosow lokalnych w prywatnej przestrzeni aplikacji. */
    public static final String DIR_NAME = "local-voices";

    private final LocalVoiceDirs dirs;

    public LocalVoiceStore(LocalVoiceDirs dirs) {
        this.dirs = dirs;
    }

    public File getRootDir() {
        return dirs.rootDir();
    }

    private File getRegistryFile() {
        return new File(dirs.rootDir(), REGISTRY_FILE_NAME);
    }

    /**
     * Wszystkie zarejestrowane glosy, posortowane po nazwie - kolejnosc na liscie
     * w UI ma byc stabilna, a {@link Properties} kolejnosci nie gwarantuje.
     *
     * Wpisy nieczytelne sa POMIJANE, a nie traktowane jako blad calego rejestru:
     * jeden zepsuty wiersz nie moze odciac uzytkownika od pozostalych glosow.
     */
    public List<LocalVoicePack> list() throws IOException {
        Properties props = load();
        List<LocalVoicePack> packs = new ArrayList<>();
        for (String id : props.stringPropertyNames()) {
            try {
                packs.add(LocalVoicePack.fromRegistryValue(id, props.getProperty(id)));
            } catch (IllegalArgumentException ignored) {
                // zepsuty wiersz rejestru - pomijamy
            }
        }
        Collections.sort(packs, new Comparator<LocalVoicePack>() {
            @Override
            public int compare(LocalVoicePack a, LocalVoicePack b) {
                return a.getName().compareToIgnoreCase(b.getName());
            }
        });
        return packs;
    }

    /** Glos o podanym identyfikatorze albo {@code null}, gdy go nie ma. */
    public LocalVoicePack get(String id) throws IOException {
        String value = load().getProperty(id);
        if (value == null)
            return null;
        try {
            return LocalVoicePack.fromRegistryValue(id, value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Dodaje glos; wpis o tym samym identyfikatorze jest nadpisywany. */
    public void add(LocalVoicePack pack) throws IOException {
        Properties props = load();
        props.setProperty(pack.getId(), pack.toRegistryValue());
        save(props);
    }

    /** Wlacza lub wylacza glos. Nieznany identyfikator jest ignorowany. */
    public void setEnabled(String id, boolean enabled) throws IOException {
        LocalVoicePack pack = get(id);
        if (pack == null)
            return;
        add(pack.withEnabled(enabled));
    }

    /**
     * Usuwa glos z rejestru ORAZ jego dane z dysku. Nieznany identyfikator jest
     * ignorowany.
     */
    public void remove(String id) throws IOException {
        Properties props = load();
        if (props.remove(id) == null)
            return;
        save(props);
        deleteRecursively(new File(dirs.rootDir(), id));
    }

    /**
     * Sciezki katalogow glosow, ktore maja trafic do silnika.
     *
     * Pomijane sa glosy wylaczone ORAZ te, ktorych dane zniknely z dysku (np. po
     * wyczyszczeniu danych aplikacji) - silnik nie moze dostac sciezki, ktora nie
     * istnieje.
     */
    public List<String> getEnabledPaths() throws IOException {
        File root = dirs.rootDir();
        List<String> paths = new ArrayList<>();
        for (LocalVoicePack pack : list()) {
            if (!pack.isEnabled())
                continue;
            File dir = pack.getDir(root);
            if (!new File(dir, LocalVoiceLayout.INFO_FILE_NAME).isFile())
                continue;
            paths.add(dir.getAbsolutePath());
        }
        return paths;
    }

    private Properties load() throws IOException {
        Properties props = new Properties();
        File file = getRegistryFile();
        if (!file.isFile())
            return props;
        InputStreamReader reader = new InputStreamReader(
                new BufferedInputStream(new FileInputStream(file)), "utf-8");
        try {
            props.load(reader);
        } catch (FileNotFoundException e) {
            return props;
        } finally {
            reader.close();
        }
        return props;
    }

    private void save(Properties props) throws IOException {
        File root = dirs.rootDir();
        if (!root.isDirectory() && !root.mkdirs())
            throw new IOException("cannot create local voice directory: " + root);
        Writer writer = new OutputStreamWriter(
                new BufferedOutputStream(new FileOutputStream(getRegistryFile())), "utf-8");
        try {
            props.store(writer, "RHVoice local voices");
        } finally {
            writer.close();
        }
    }

    /** Usuwa plik lub caly katalog z zawartoscia. */
    static void deleteRecursively(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children)
                deleteRecursively(child);
        }
        file.delete();
    }
}
