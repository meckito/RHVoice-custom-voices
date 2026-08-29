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

import java.io.File;
import java.io.IOException;

/**
 * Bariera bezpieczenstwa przy rozpakowywaniu archiwow (Zip Slip).
 *
 * Nazwa wpisu w ZIP-ie pochodzi z pliku, ktory dal uzytkownik, wiec moze zawierac
 * {@code ../} albo sciezke absolutna i probowac nadpisac plik poza katalogiem
 * docelowym. Ta klasa jest JEDYNYM miejscem, przez ktore wolno zamieniac nazwe
 * wpisu na plik na dysku.
 */
public final class SafeZipPath {

    private SafeZipPath() {
    }

    /**
     * Zamienia nazwe wpisu archiwum na plik wewnatrz {@code targetDir}.
     *
     * @param targetDir katalog docelowy rozpakowania.
     * @param entryName nazwa wpisu z archiwum (moze byc wroga).
     * @return plik gwarantowanie lezacy WEWNATRZ {@code targetDir}.
     * @throws IOException gdy wpis wychodzi poza katalog docelowy albo wskazuje
     *                     na sam katalog docelowy.
     */
    public static File resolve(File targetDir, String entryName) throws IOException {
        if (entryName == null)
            throw new IOException("zip entry has no name");
        String name = LocalVoiceLayout.normalizeSeparators(entryName).trim();
        if (name.isEmpty())
            throw new IOException("zip entry has an empty name");
        if (name.startsWith("/"))
            throw new IOException("zip entry uses an absolute path: " + entryName);

        // Porownanie na sciezkach kanonicznych: rozwija .. oraz symlinki, wiec
        // wykrywa tez ucieczke ukryta w srodku sciezki.
        String root = targetDir.getCanonicalPath();
        File target = new File(targetDir, name);
        String resolved = target.getCanonicalPath();

        // Separator w prefiksie jest KONIECZNY: bez niego katalog "rootEvil"
        // przeszedlby jako rzekomo lezacy w "root".
        if (!resolved.startsWith(root + File.separator))
            throw new IOException("zip entry escapes the target directory: " + entryName);
        return target;
    }
}
