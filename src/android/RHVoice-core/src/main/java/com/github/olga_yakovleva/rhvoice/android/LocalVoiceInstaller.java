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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Caly przebieg instalacji glosu z pliku, bez ani jednej zaleznosci od Androida -
 * dzieki temu logika importu jest testowalna na JVM, a worker i UI sa tylko
 * cienka warstwa nad ta klasa.
 *
 * Gwarancja: instalacja jest ALBO calkowicie udana, ALBO nie zostawia sladu.
 * Rozpakowanie idzie do katalogu tymczasowego, a na miejsce docelowe wchodzi
 * dopiero na koniec przez {@link File#renameTo} - dlatego nieudany import nie
 * niszczy poprzedniej, dzialajacej wersji glosu.
 */
public final class LocalVoiceInstaller {

    /** Nazwa katalogu roboczego wewnatrz katalogu glosow lokalnych. */
    static final String TEMP_DIR_NAME = ".tmp";

    /** Pyta, czy jezyk o tej nazwie jest zainstalowany (np. "Polish"). */
    public interface LanguageAvailability {
        boolean isLanguageInstalled(String languageName);
    }

    /** Paczka jest poprawna, ale brakuje danych jezykowych, bez ktorych glos nie zagra. */
    public static final class MissingLanguageException extends IOException {
        private final String language;

        MissingLanguageException(String language) {
            super("language not installed: " + language);
            this.language = language;
        }

        public String getLanguage() {
            return language;
        }
    }

    private final LocalVoiceStore store;
    private final LanguageAvailability languages;

    public LocalVoiceInstaller(LocalVoiceStore store, LanguageAvailability languages) {
        this.store = store;
        this.languages = languages;
    }

    /**
     * Instaluje glos z archiwum.
     *
     * @param in strumien pliku wskazanego przez uzytkownika; metoda go NIE zamyka.
     * @return zarejestrowany glos.
     * @throws MissingLanguageException gdy nie ma zainstalowanego jezyka glosu.
     * @throws IOException gdy plik nie jest paczka glosu, zawiera wroga sciezke
     *                     albo zapis sie nie udal.
     */
    public LocalVoicePack install(InputStream in) throws IOException {
        File root = store.getRootDir();
        if (!root.isDirectory() && !root.mkdirs())
            throw new IOException("cannot create local voice directory: " + root);
        File tempRoot = new File(root, TEMP_DIR_NAME);
        // Sprzatanie po ewentualnej wczesniejszej awarii (np. ubity proces).
        LocalVoiceStore.deleteRecursively(tempRoot);
        if (!tempRoot.mkdirs())
            throw new IOException("cannot create temporary directory: " + tempRoot);

        File archive = new File(tempRoot, "package.zip");
        File unpacked = new File(tempRoot, "unpacked");
        try {
            // Kopia na dysk, bo archiwum trzeba przejsc DWA razy: raz po uklad,
            // raz po zawartosc. Strumienia z SAF nie da sie cofnac.
            copyToFile(in, archive);

            LocalVoiceLayout layout = detectLayout(archive);
            LocalVoiceInfo info = readInfo(archive, layout);
            if (!languages.isLanguageInstalled(info.getLanguage()))
                throw new MissingLanguageException(info.getLanguage());

            if (!unpacked.mkdirs())
                throw new IOException("cannot create temporary directory: " + unpacked);
            InputStream data = openArchive(archive);
            try {
                LocalVoiceExtractor.extract(data, layout, unpacked);
            } finally {
                data.close();
            }

            LocalVoicePack pack = LocalVoicePack.fromInfo(info);
            // Ponowny import nie moze po cichu wlaczyc glosu, ktory uzytkownik
            // swiadomie wylaczyl - zachowujemy jego decyzje.
            LocalVoicePack previous = store.get(pack.getId());
            if (previous != null)
                pack = pack.withEnabled(previous.isEnabled());

            File target = pack.getDir(root);
            // Podmiana na samym koncu: az do tej chwili stara wersja glosu dziala.
            LocalVoiceStore.deleteRecursively(target);
            if (!unpacked.renameTo(target))
                throw new IOException("cannot move the voice into place: " + target);
            store.add(pack);
            return pack;
        } finally {
            // R5: katalog roboczy znika ZAWSZE, takze po bledzie i po wyjatku.
            LocalVoiceStore.deleteRecursively(tempRoot);
        }
    }

    private static InputStream openArchive(File file) throws IOException {
        return new BufferedInputStream(new FileInputStream(file));
    }

    private static LocalVoiceLayout detectLayout(File archive) throws IOException {
        InputStream in = openArchive(archive);
        try {
            LocalVoiceLayout layout = LocalVoiceLayout.detect(in);
            if (!layout.isVoicePackage())
                throw new IOException("the file is not a voice package");
            return layout;
        } finally {
            in.close();
        }
    }

    /** Czyta voice.info wprost z archiwum, jeszcze przed rozpakowaniem czegokolwiek. */
    private static LocalVoiceInfo readInfo(File archive, LocalVoiceLayout layout) throws IOException {
        String infoPath = layout.getPrefix() + LocalVoiceLayout.INFO_FILE_NAME;
        InputStream in = openArchive(archive);
        try {
            java.util.zip.ZipInputStream zip = new java.util.zip.ZipInputStream(in);
            java.util.zip.ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory())
                    continue;
                if (LocalVoiceLayout.normalizeSeparators(entry.getName()).equals(infoPath))
                    return LocalVoiceInfo.parse(zip);
            }
        } finally {
            in.close();
        }
        throw new IOException("the package has no " + LocalVoiceLayout.INFO_FILE_NAME);
    }

    private static void copyToFile(InputStream in, File target) throws IOException {
        OutputStream out = new BufferedOutputStream(new FileOutputStream(target));
        try {
            byte[] buf = new byte[8192];
            int read;
            while ((read = in.read(buf)) != -1) {
                if (read > 0)
                    out.write(buf, 0, read);
            }
            out.flush();
        } finally {
            out.close();
        }
    }

    /**
     * Pozostalosci w katalogu roboczym. Sluzy testom do dowodzenia, ze import
     * nie zostawia smieci; w aplikacji nieuzywane.
     */
    List<String> listTempLeftovers() {
        List<String> names = new ArrayList<>();
        File[] children = new File(store.getRootDir(), TEMP_DIR_NAME).listFiles();
        if (children != null) {
            for (File child : children)
                names.add(child.getName());
        }
        return names;
    }
}
