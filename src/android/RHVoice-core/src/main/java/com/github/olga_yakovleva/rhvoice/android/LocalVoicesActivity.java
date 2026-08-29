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
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Ekran glosow wlasnych.
 *
 * Jest OSOBNA aktywnoscia (wzorowana na {@link SettingsActivity}), a nie
 * fragmentem wstrzykiwanym w cudza aktywnosc: pozycja menu jest wspolna dla
 * wszystkich ekranow z belka, a nie kazda taka aktywnosc ma kontener i cykl
 * zycia, ktorych potrzebuje ten fragment.
 */
public final class LocalVoicesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle state) {
        EdgeToEdge.enable(this);
        super.onCreate(state);
        setTitle(R.string.local_voices);
        setContentView(R.layout.frame);
        if (state == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frame, new LocalVoicesFragment(), "local_voices")
                    .commit();
        }
    }

    public static void show(Context context) {
        context.startActivity(new Intent(context, LocalVoicesActivity.class));
    }
}
