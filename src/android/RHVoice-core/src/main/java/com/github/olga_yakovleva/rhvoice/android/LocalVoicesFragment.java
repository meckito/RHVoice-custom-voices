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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.divider.MaterialDividerItemDecoration;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Ekran "Glosy wlasne": import glosu z pliku oraz wlaczanie, wylaczanie i
 * usuwanie zaimportowanych glosow.
 *
 * DOSTEPNOSC (uzytkownicy czytnikow ekranu sa dla tej funkcji glowna grupa):
 * wynik importu i kazda zmiana stanu ida do TRWALEGO pola tekstowego z
 * accessibilityLiveRegion, a nie do Toasta - czytnik oglasza je od razu, a
 * uzytkownik moze do nich wrocic palcem.
 */
public final class LocalVoicesFragment extends ToolbarFragment
        implements LocalVoiceListAdapter.Listener {

    private static final String TAG = "RHVoice.LocalVoices";

    private LocalVoiceListAdapter adapter;
    private TextView status;
    private LocalVoiceStore store;
    private ActivityResultLauncher<String[]> picker;

    /** Odbiera wynik importu z workera. */
    private final BroadcastReceiver importReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String missingLanguage = intent.getStringExtra(LocalVoiceImportWorker.EXTRA_MISSING_LANGUAGE);
            String error = intent.getStringExtra(LocalVoiceImportWorker.EXTRA_ERROR);
            String name = intent.getStringExtra(LocalVoiceImportWorker.EXTRA_VOICE_NAME);
            if (missingLanguage != null)
                announce(getString(R.string.local_voice_error_missing_language, missingLanguage));
            else if (error != null)
                announce(getString(R.string.local_voice_error_failed, error));
            else if (name != null)
                announce(getString(R.string.local_voice_installed, name));
            refresh();
        }
    };

    @Override
    public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        // Typ */* jest konieczny: .nvda-addon nie ma zarejestrowanego typu MIME,
        // wiec zawezenie do application/zip ukrylby te pliki w wybieraczu.
        picker = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri == null)
                return;
            onFileChosen(uri);
        });
    }

    @Override
    public void onViewReady(@NonNull View view, @Nullable Bundle savedInstanceState) {
        View root = replaceFrame(view, R.layout.local_voices);
        if (root == null)
            return;
        store = ContextLocalVoiceDirs.newStore(requireContext());
        status = root.findViewById(R.id.local_voice_status);

        MaterialButton importButton = root.findViewById(R.id.local_voice_import);
        importButton.setOnClickListener(v -> picker.launch(
                new String[]{"application/zip", "application/octet-stream", "*/*"}));

        RecyclerView list = root.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(requireActivity()));
        list.addItemDecoration(new MaterialDividerItemDecoration(
                requireContext(), MaterialDividerItemDecoration.VERTICAL));
        adapter = new LocalVoiceListAdapter(requireContext(), this);
        list.setAdapter(adapter);
        InsetUtil.setInsets(list, (left, top, right, bottom) -> list.setPadding(left, 0, right, bottom));

        toolbar.setSubtitle(R.string.local_voices);
        refresh();
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(LocalVoiceImportWorker.ACTION_IMPORT_DONE);
        androidx.core.content.ContextCompat.registerReceiver(requireContext(), importReceiver, filter,
                androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED);
        refresh();
    }

    @Override
    public void onStop() {
        requireContext().unregisterReceiver(importReceiver);
        super.onStop();
    }

    private void onFileChosen(Uri uri) {
        announce(getString(R.string.local_voice_installing));
        LocalVoiceImportWorker.enqueue(requireContext(), uri);
    }

    /**
     * Import GOTOWEJ kopii paczki, ktora aktywnosc zrobila z pliku wskazanego
     * poza aplikacja (menedzer plikow, udostepnianie).
     */
    public void importLocalCopy(java.io.File file) {
        announce(getString(R.string.local_voice_installing));
        LocalVoiceImportWorker.enqueueFile(requireContext(), file);
    }

    /** Pokazuje blad, ktory wystapil jeszcze przed zleceniem importu. */
    public void showImportError(String message) {
        announce(message);
    }

    /** Pokazuje komunikat i pozwala czytnikowi ekranu go oglosic. */
    private void announce(String message) {
        if (status == null)
            return;
        status.setText(message);
        status.setVisibility(View.VISIBLE);
        status.announceForAccessibility(message);
    }

    private void refresh() {
        if (adapter == null)
            return;
        List<LocalVoicePack> packs;
        try {
            packs = store.list();
        } catch (IOException e) {
            Log.e(TAG, "Cannot read the local voice registry", e);
            packs = Collections.emptyList();
        }
        adapter.setItems(packs);
        if (packs.isEmpty())
            announce(getString(R.string.local_voice_none));
    }

    @Override
    public void onLocalVoiceEnabledChanged(LocalVoicePack pack, boolean enabled) {
        try {
            store.setEnabled(pack.getId(), enabled);
        } catch (IOException e) {
            Log.e(TAG, "Cannot update the local voice registry", e);
            announce(getString(R.string.local_voice_error_failed, String.valueOf(e.getMessage())));
            return;
        }
        notifyEngine();
        announce(getString(enabled ? R.string.local_voice_enabled : R.string.local_voice_disabled,
                pack.getName()));
        refresh();
    }

    @Override
    public void onLocalVoiceRemoveRequested(LocalVoicePack pack) {
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage(getString(R.string.local_voice_remove_question, pack.getName()))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.remove, (dialog, which) -> remove(pack))
                .show();
    }

    private void remove(LocalVoicePack pack) {
        try {
            store.remove(pack.getId());
        } catch (IOException e) {
            Log.e(TAG, "Cannot remove the local voice", e);
            announce(getString(R.string.local_voice_error_failed, String.valueOf(e.getMessage())));
            return;
        }
        notifyEngine();
        announce(getString(R.string.local_voice_removed, pack.getName()));
        refresh();
    }

    /**
     * Usluga TTS trzyma sciezki glosow w pamieci i przelicza je na to
     * rozgloszenie - bez niego zmiana zadziala dopiero po restarcie uslugi.
     */
    private void notifyEngine() {
        requireContext().sendBroadcast(new Intent(RHVoiceService.ACTION_CHECK_DATA));
    }
}
