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
 * Zrodlo katalogu, w ktorym mieszkaja glosy lokalne.
 *
 * Istnieje po to, zeby {@link LocalVoiceStore} nie zalezal od
 * {@code android.content.Context} i dal sie testowac na czystej JVM z katalogiem
 * tymczasowym. W aplikacji implementacja zwraca
 * {@code context.getDir("local-voices", 0)}.
 */
public interface LocalVoiceDirs {

    /** Katalog glosow lokalnych; moze jeszcze nie istniec. */
    File rootDir();
}
