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
import android.net.Uri;
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
 *
 * Ten ekran jest tez punktem wejscia dla "Otworz za pomoca": gdy uzytkownik
 * otworzy paczke glosu w menedzerze plikow, system moze uruchomic wlasnie tu
 * (filtry intencji w manifescie).
 */
public final class LocalVoicesActivity extends AppCompatActivity {

    /** Zapamietuje, ze plik z intencji zostal juz przekazany do importu. */
    private static final String STATE_INTENT_HANDLED = "intent_handled";

    private boolean intentHandled;

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
        } else {
            intentHandled = state.getBoolean(STATE_INTENT_HANDLED, false);
        }
        handleIncomingFile(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        // Nowa intencja to nowy plik - poprzednie oznaczenie przestaje obowiazywac.
        intentHandled = false;
        handleIncomingFile(intent);
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        // Bez tego obrot ekranu zaimportowalby ten sam plik po raz drugi.
        out.putBoolean(STATE_INTENT_HANDLED, intentHandled);
    }

    /**
     * Jesli aktywnosc dostala plik z zewnatrz (menedzer plikow, udostepnianie),
     * zleca jego import.
     */
    private void handleIncomingFile(Intent intent) {
        if (intentHandled || intent == null)
            return;
        Uri uri = extractUri(intent);
        if (uri == null)
            return;
        intentHandled = true;
        LocalVoicesFragment fragment = (LocalVoicesFragment)
                getSupportFragmentManager().findFragmentByTag("local_voices");
        if (fragment != null)
            fragment.importFile(uri);
        else
            LocalVoiceImportWorker.enqueue(this, uri);
    }

    private static Uri extractUri(Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_VIEW.equals(action))
            return intent.getData();
        if (Intent.ACTION_SEND.equals(action))
            return intent.getParcelableExtra(Intent.EXTRA_STREAM);
        return null;
    }

    public static void show(Context context) {
        context.startActivity(new Intent(context, LocalVoicesActivity.class));
    }
}
