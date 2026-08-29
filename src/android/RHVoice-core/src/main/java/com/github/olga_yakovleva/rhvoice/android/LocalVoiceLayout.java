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

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Rozpoznaje uklad paczki glosu w archiwum ZIP.
 *
 * Obslugiwane sa dwa realne warianty:
 * - paczka androidowa: {@code voice.info} lezy w korzeniu archiwum (prefiks {@code ""}),
 * - dodatek NVDA ({@code .nvda-addon}): glos siedzi w podkatalogu, najczesciej
 *   {@code data/} (prefiks {@code "data/"}).
 *
 * Klasa nie rozpakowuje niczego - tylko odpowiada, GDZIE w archiwum jest korzen
 * glosu, albo ze to nie jest paczka glosu. Jest czysta Java, wiec w pelni
 * testowalna bez urzadzenia.
 */
public final class LocalVoiceLayout {

    /** Nazwa pliku opisu glosu; jego polozenie wyznacza korzen paczki. */
    public static final String INFO_FILE_NAME = "voice.info";

    private final String prefix;

    private LocalVoiceLayout(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Przeglada archiwum i zwraca rozpoznany uklad.
     *
     * Wybierany jest wpis {@code voice.info} o NAJKROTSZEJ sciezce, bo paczka
     * moze zawierac zagniezdzone kopie (np. przykladowe dane), a korzeniem jest
     * ten najwyzej.
     *
     * @param in strumien archiwum ZIP; metoda go NIE zamyka.
     */
    public static LocalVoiceLayout detect(InputStream in) throws IOException {
        String best = null;
        ZipInputStream zip = new ZipInputStream(in);
        ZipEntry entry;
        while ((entry = zip.getNextEntry()) != null) {
            if (entry.isDirectory())
                continue;
            String name = normalizeSeparators(entry.getName());
            if (!isVoiceInfoPath(name))
                continue;
            if (best == null || name.length() < best.length())
                best = name;
        }
        if (best == null)
            return new LocalVoiceLayout(null);
        return new LocalVoiceLayout(best.substring(0, best.length() - INFO_FILE_NAME.length()));
    }

    /**
     * Ujednolica separatory katalogow. Niektore archiwizery zapisuja separator
     * windowsowy, mimo ze format ZIP wymaga ukosnika - traktujemy oba tak samo,
     * zeby nie odrzucac poprawnych merytorycznie paczek.
     */
    static String normalizeSeparators(String name) {
        return name.replace('\\', '/');
    }

    /** Czy sciezka wskazuje wprost na plik {@code voice.info} (a nie np. {@code myvoice.info}). */
    private static boolean isVoiceInfoPath(String name) {
        if (name.equals(INFO_FILE_NAME))
            return true;
        return name.endsWith("/" + INFO_FILE_NAME);
    }

    /** Czy archiwum zawiera paczke glosu. */
    public boolean isVoicePackage() {
        return prefix != null;
    }

    /**
     * Prefiks sciezki, ktory nalezy sciac przy rozpakowywaniu: {@code ""} dla
     * paczki androidowej, {@code "data/"} dla dodatku NVDA.
     *
     * @throws IllegalStateException gdy archiwum nie jest paczka glosu.
     */
    public String getPrefix() {
        if (prefix == null)
            throw new IllegalStateException("archive is not a voice package");
        return prefix;
    }
}
