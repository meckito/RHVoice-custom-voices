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
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.IOException;
import java.io.InputStream;

/**
 * Import glosu z pliku w tle.
 *
 * Uzywa {@code androidx.work} tak jak reszta aplikacji ({@code DataWorker},
 * {@code DataSyncWorker}) - nie wprowadzamy drugiego mechanizmu wspolbieznosci.
 * Dzieki temu import przezywa obrot ekranu i zamkniecie ekranu przez uzytkownika.
 *
 * Cala logika siedzi w {@link LocalVoiceInstaller}; tutaj jest tylko przeklad
 * z androidowego {@link Uri} i rozgloszenie wyniku.
 */
public final class LocalVoiceImportWorker extends Worker {

    private static final String TAG = "RHVoice.LocalVoiceImport";

    /** Klucz wejsciowy: adres pliku wybranego przez uzytkownika. */
    public static final String KEY_URI = "uri";

    /** Nazwa unikalnej pracy - dwa importy naraz nie maja sensu. */
    public static final String WORK_NAME = "com.github.olga_yakovleva.rhvoice.android.local_voice_import";

    /** Rozgloszenie wyniku importu do UI. */
    public static final String ACTION_IMPORT_DONE = "com.github.olga_yakovleva.rhvoice.android.action.local_voice_import_done";
    public static final String EXTRA_VOICE_NAME = "voice_name";
    /** Rodzaj bledu: brak jezyka. */
    public static final String EXTRA_MISSING_LANGUAGE = "missing_language";
    /** Rodzaj bledu: inny (tekst do pokazania). */
    public static final String EXTRA_ERROR = "error";

    public LocalVoiceImportWorker(Context context, WorkerParameters params) {
        super(context, params);
    }

    /** Zleca import wskazanego pliku. */
    public static void enqueue(Context context, Uri uri) {
        Data input = new Data.Builder().putString(KEY_URI, uri.toString()).build();
        WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                androidx.work.ExistingWorkPolicy.REPLACE,
                new OneTimeWorkRequest.Builder(LocalVoiceImportWorker.class)
                        .setInputData(input)
                        .build());
    }

    @Override
    public Result doWork() {
        final Context context = getApplicationContext();
        String uriString = getInputData().getString(KEY_URI);
        if (uriString == null) {
            Log.e(TAG, "No uri given");
            return Result.failure();
        }
        LocalVoiceStore store = ContextLocalVoiceDirs.newStore(context);
        // Jezyk uznajemy za dostepny, gdy silnik ma juz jego dane - o to samo
        // pyta reszta aplikacji przez LanguagePack.getPath.
        LocalVoiceInstaller installer = new LocalVoiceInstaller(store, name -> {
            DataManager dm = Repository.get().createDataManager();
            LanguagePack lp = dm.getLanguageByName(name);
            return lp != null && lp.getPath(context) != null;
        });
        try {
            InputStream in = context.getContentResolver().openInputStream(Uri.parse(uriString));
            if (in == null)
                throw new IOException("cannot open the selected file");
            LocalVoicePack pack;
            try {
                pack = installer.install(in);
            } finally {
                in.close();
            }
            // Silnik trzyma sciezki w pamieci - bez tego rozgloszenia nowy glos
            // pojawi sie dopiero po restarcie uslugi TTS.
            context.sendBroadcast(new Intent(RHVoiceService.ACTION_CHECK_DATA));
            context.sendBroadcast(new Intent(TextToSpeech.Engine.ACTION_TTS_DATA_INSTALLED));
            Intent done = new Intent(ACTION_IMPORT_DONE);
            done.setPackage(context.getPackageName());
            done.putExtra(EXTRA_VOICE_NAME, pack.getName());
            context.sendBroadcast(done);
            return Result.success();
        } catch (LocalVoiceInstaller.MissingLanguageException e) {
            report(context, EXTRA_MISSING_LANGUAGE, e.getLanguage());
            return Result.failure();
        } catch (IOException e) {
            Log.e(TAG, "Import failed", e);
            String message = (e.getMessage() == null) ? e.toString() : e.getMessage();
            report(context, EXTRA_ERROR, message);
            return Result.failure();
        }
    }

    private static void report(Context context, String extra, String value) {
        Intent done = new Intent(ACTION_IMPORT_DONE);
        done.setPackage(context.getPackageName());
        done.putExtra(extra, value);
        context.sendBroadcast(done);
    }
}
