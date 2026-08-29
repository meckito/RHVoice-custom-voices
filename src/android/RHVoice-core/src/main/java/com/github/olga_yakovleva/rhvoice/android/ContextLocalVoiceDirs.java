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

import android.content.Context;

import java.io.File;

/**
 * Androidowa implementacja {@link LocalVoiceDirs}.
 *
 * Jest osobna klasa (a nie metoda w {@link LocalVoiceStore}) po to, zeby sam
 * rejestr NIE zalezal od {@code Context} i dal sie testowac na czystej JVM.
 * Cala zaleznosc od Androida w warstwie glosow lokalnych siedzi wlasnie tutaj.
 *
 * Katalog {@code local-voices} jest CELOWO inny niz {@code getDir("data", 0)}:
 * {@code DataPack.cleanup()} przeczesuje katalog danych i usuwa wszystko pasujace
 * do nazwy paczki, wiec glosy uzytkownika trzymamy poza jego zasiegiem.
 */
public final class ContextLocalVoiceDirs implements LocalVoiceDirs {

    private final Context context;

    public ContextLocalVoiceDirs(Context context) {
        this.context = context.getApplicationContext();
    }

    /** Skrot: rejestr glosow lokalnych dla podanego kontekstu. */
    public static LocalVoiceStore newStore(Context context) {
        return new LocalVoiceStore(new ContextLocalVoiceDirs(context));
    }

    @Override
    public File rootDir() {
        return context.getDir(LocalVoiceStore.DIR_NAME, Context.MODE_PRIVATE);
    }
}
