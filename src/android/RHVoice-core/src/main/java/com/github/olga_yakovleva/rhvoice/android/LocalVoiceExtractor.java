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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Rozpakowuje paczke glosu do katalogu docelowego, NORMALIZUJAC uklad.
 *
 * Normalizacja polega na scieciu prefiksu rozpoznanego przez
 * {@link LocalVoiceLayout}, wiec niezaleznie od tego, czy paczka byla androidowa,
 * czy dodatkiem NVDA, w katalogu docelowym {@code voice.info} lezy w korzeniu -
 * dokladnie tak, jak silnik oczekuje katalogu glosu.
 *
 * Wszystko poza poddrzewem glosu (manifest.ini, doc/, langdata/) jest pomijane.
 *
 * NIE uzywamy {@code DataPack.copyBytes}: tam metoda jest {@code protected}, a
 * caly {@code DataPack} zalezny od {@code Context} i {@code android.util.Log},
 * co uniemozliwiloby testy na czystej JVM. Petla kopiujaca jest tu wlasna i
 * celowo trywialna.
 */
public final class LocalVoiceExtractor {

    private static final int BUFFER_SIZE = 8192;

    private LocalVoiceExtractor() {
    }

    /**
     * Rozpakowuje poddrzewo glosu.
     *
     * @param in       strumien archiwum ZIP; metoda go NIE zamyka.
     * @param layout   uklad rozpoznany wczesniej dla TEGO SAMEGO archiwum.
     * @param targetDir katalog docelowy; zostanie utworzony, gdy nie istnieje.
     * @return sciezki relatywne rozpakowanych plikow, w kolejnosci z archiwum.
     * @throws IOException gdy archiwum nie jest paczka glosu, gdy wpis probuje
     *                     wyjsc poza katalog docelowy albo przy bledzie zapisu.
     *                     Paczka z wroga sciezka jest odrzucana w CALOSCI.
     */
    public static List<String> extract(InputStream in, LocalVoiceLayout layout, File targetDir)
            throws IOException {
        if (!layout.isVoicePackage())
            throw new IOException("archive is not a voice package");
        String prefix = layout.getPrefix();
        if (!targetDir.isDirectory() && !targetDir.mkdirs())
            throw new IOException("cannot create target directory: " + targetDir);

        List<String> extracted = new ArrayList<>();
        ZipInputStream zip = new ZipInputStream(in);
        ZipEntry entry;
        while ((entry = zip.getNextEntry()) != null) {
            if (entry.isDirectory())
                continue;
            String name = LocalVoiceLayout.normalizeSeparators(entry.getName());
            // Sprawdzamy bezpieczenstwo KAZDEGO wpisu, takze tych spoza poddrzewa
            // glosu: wroga sciezka dyskwalifikuje cala paczke, a nie tylko jeden plik.
            SafeZipPath.resolve(targetDir, name);
            if (!name.startsWith(prefix))
                continue;
            String relative = name.substring(prefix.length());
            if (relative.isEmpty())
                continue;
            File target = SafeZipPath.resolve(targetDir, relative);
            File parent = target.getParentFile();
            if (parent != null && !parent.isDirectory() && !parent.mkdirs())
                throw new IOException("cannot create directory: " + parent);
            writeTo(zip, target);
            extracted.add(relative);
        }
        return extracted;
    }

    private static void writeTo(InputStream in, File target) throws IOException {
        OutputStream out = new BufferedOutputStream(new FileOutputStream(target));
        try {
            byte[] buf = new byte[BUFFER_SIZE];
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
}
