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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

/**
 * Opis glosu wczytany z pliku {@code voice.info} paczki lokalnej.
 *
 * Format czytamy tak samo jak upstream w {@code DataPack.getVersion(File)}:
 * {@link Properties} z kodowaniem UTF-8. Rozni sie zachowanie przy bledzie -
 * upstream zwraca {@code null}, my rzucamy {@link IOException} z opisem, bo ten
 * plik pochodzi od uzytkownika i powod odrzucenia trzeba mu pokazac.
 */
public final class LocalVoiceInfo {

    /** Wymagane klucze; bez nich paczka nie jest uzywalnym glosem. */
    private static final String KEY_NAME = "name";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_FORMAT = "format";
    private static final String KEY_GENDER = "gender";
    private static final String KEY_REVISION = "revision";

    private final String name;
    private final String language;
    private final String gender;
    private final int format;
    private final int revision;

    private LocalVoiceInfo(String name, String language, String gender, int format, int revision) {
        this.name = name;
        this.language = language;
        this.gender = gender;
        this.format = format;
        this.revision = revision;
    }

    /**
     * Wczytuje i sprawdza {@code voice.info}.
     *
     * @param in strumien tresci pliku; metoda go NIE zamyka.
     * @throws IOException gdy brakuje wymaganego pola albo liczba jest niepoprawna.
     */
    public static LocalVoiceInfo parse(InputStream in) throws IOException {
        Properties props = new Properties();
        // InputStreamReader wymusza UTF-8; Properties.load(InputStream) czytalby latin-1.
        props.load(new InputStreamReader(new BufferedInputStream(in), "utf-8"));
        String name = requireNonBlank(props, KEY_NAME);
        String language = requireNonBlank(props, KEY_LANGUAGE);
        int format = requireInt(props, KEY_FORMAT, false);
        int revision = requireInt(props, KEY_REVISION, true);
        String gender = trimmed(props.getProperty(KEY_GENDER));
        return new LocalVoiceInfo(name, language, gender, format, revision);
    }

    private static String trimmed(String value) {
        return (value == null) ? "" : value.trim();
    }

    private static String requireNonBlank(Properties props, String key) throws IOException {
        String value = trimmed(props.getProperty(key));
        if (value.isEmpty())
            throw new IOException("voice.info: missing required field '" + key + "'");
        return value;
    }

    /**
     * @param optional gdy true, brak pola daje 0 zamiast bledu (upstream tak
     *                 traktuje {@code revision} nowych paczek).
     */
    private static int requireInt(Properties props, String key, boolean optional) throws IOException {
        String value = trimmed(props.getProperty(key));
        if (value.isEmpty()) {
            if (optional)
                return 0;
            throw new IOException("voice.info: missing required field '" + key + "'");
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IOException("voice.info: field '" + key + "' is not a number: " + value);
        }
    }

    public String getName() {
        return name;
    }

    public String getLanguage() {
        return language;
    }

    /** Plec lektora albo pusty napis, gdy paczka jej nie podaje. */
    public String getGender() {
        return gender;
    }

    public int getFormat() {
        return format;
    }

    public int getRevision() {
        return revision;
    }

    /**
     * Identyfikator glosu wyliczany DOKLADNIE tak jak w
     * {@code TtsResource.getId()}, zeby glos lokalny i pobrany z katalogu
     * trafialy w to samo miejsce w danych.
     */
    public String getId() {
        return name.toLowerCase().replace("-", "_");
    }
}
