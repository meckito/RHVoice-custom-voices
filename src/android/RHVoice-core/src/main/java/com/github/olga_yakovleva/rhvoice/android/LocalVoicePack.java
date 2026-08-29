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

/**
 * Glos zainstalowany z pliku przez uzytkownika.
 *
 * DECYZJA PROJEKTOWA: ta klasa NIE dziedziczy po {@link DataPack}.
 * {@code DataPack} jest scisle spleciony z {@code TtsResource}, {@code dataUrl},
 * {@code dataMd5}, WorkManagerem i wersjonowaniem z katalogu JSON - dziedziczenie
 * wymusiloby atrapy tych pol dla glosu, ktory nie ma zadnego zrodla w sieci.
 * Wspolny jest tylko kontrakt "daj sciezke do katalogu glosu", wiec jest to
 * osobny, prosty typ - dzieki temu daje sie w calosci testowac na JVM.
 *
 * Format wpisu w rejestrze {@code local-voices.properties}:
 * {@code <id> = <name>|<language>|<enabled>}
 */
public final class LocalVoicePack {

    /** Separator pol w wartosci rejestru. */
    private static final char FIELD_SEPARATOR = '|';

    private final String id;
    private final String name;
    private final String language;
    private final boolean enabled;

    public LocalVoicePack(String id, String name, String language, boolean enabled) {
        this.id = requireField(id, "id");
        this.name = requireCleanField(name, "name");
        this.language = requireCleanField(language, "language");
        this.enabled = enabled;
    }

    /** Tworzy glos wlasnie zaimportowany - domyslnie wlaczony. */
    public static LocalVoicePack fromInfo(LocalVoiceInfo info) {
        return new LocalVoicePack(info.getId(), info.getName(), info.getLanguage(), true);
    }

    /**
     * Odtwarza glos z linii rejestru.
     *
     * @throws IllegalArgumentException gdy wartosc nie ma wymaganych pol.
     */
    public static LocalVoicePack fromRegistryValue(String id, String value) {
        if (value == null)
            throw new IllegalArgumentException("registry value is null for id " + id);
        String[] fields = value.split("\\" + FIELD_SEPARATOR, -1);
        if (fields.length < 2)
            throw new IllegalArgumentException("registry value has too few fields: " + value);
        // Brak lub nieczytelna flaga = glos WLACZONY. Wylaczyc glos moze tylko
        // jednoznaczne "false" - zepsuty rejestr nie ma po cichu ukrywac glosu.
        boolean enabled = !(fields.length >= 3 && "false".equalsIgnoreCase(fields[2].trim()));
        return new LocalVoicePack(id, fields[0], fields[1], enabled);
    }

    /** Linia wartosci do zapisania w rejestrze. */
    public String toRegistryValue() {
        return name + FIELD_SEPARATOR + language + FIELD_SEPARATOR + enabled;
    }

    private static String requireField(String value, String what) {
        String trimmed = (value == null) ? "" : value.trim();
        if (trimmed.isEmpty())
            throw new IllegalArgumentException("local voice: empty " + what);
        return trimmed;
    }

    /** Pole, ktore trafi do rejestru, nie moze zawierac separatora. */
    private static String requireCleanField(String value, String what) {
        String trimmed = requireField(value, what);
        if (trimmed.indexOf(FIELD_SEPARATOR) >= 0)
            throw new IllegalArgumentException("local voice: " + what + " must not contain '"
                    + FIELD_SEPARATOR + "': " + trimmed);
        return trimmed;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /** Nazwa jezyka tak, jak podaje ja {@code voice.info} (np. "Polish"). */
    public String getLanguage() {
        return language;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Kopia tego glosu z inna flaga wlaczenia. */
    public LocalVoicePack withEnabled(boolean newEnabled) {
        return new LocalVoicePack(id, name, language, newEnabled);
    }

    /** Katalog danych tego glosu wewnatrz katalogu glosow lokalnych. */
    public File getDir(File rootDir) {
        return new File(rootDir, id);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (!(other instanceof LocalVoicePack))
            return false;
        LocalVoicePack that = (LocalVoicePack) other;
        return enabled == that.enabled
                && id.equals(that.id)
                && name.equals(that.name)
                && language.equals(that.language);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + language.hashCode();
        result = 31 * result + (enabled ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "LocalVoicePack[" + id + " " + toRegistryValue() + "]";
    }
}
