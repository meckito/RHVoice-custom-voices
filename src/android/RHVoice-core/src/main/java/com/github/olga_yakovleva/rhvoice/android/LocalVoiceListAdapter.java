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

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Lista glosow zainstalowanych z pliku.
 *
 * Celowo NIE rozszerzamy {@code VoiceListAdapter}: tamten jest sparametryzowany
 * {@code VoicePack} i dodanie drugiego typu elementu wymusiloby refaktor calego
 * adaptera - wiecej ryzyka niz zysku.
 *
 * DOSTEPNOSC: przelacznik i przycisk usuwania dostaja etykiety zawierajace NAZWE
 * glosu, bo na liscie kilku glosow samo "Usun" nie mowi czytelnikowi ekranu,
 * ktorego glosu dotyczy.
 */
public final class LocalVoiceListAdapter
        extends RecyclerView.Adapter<LocalVoiceListAdapter.LocalVoiceViewHolder> {

    /** Reakcje na dzialania uzytkownika; obsluguje je fragment. */
    public interface Listener {
        void onLocalVoiceEnabledChanged(LocalVoicePack pack, boolean enabled);

        void onLocalVoiceRemoveRequested(LocalVoicePack pack);
    }

    private final Context context;
    private final Listener listener;
    private final List<LocalVoicePack> items = new ArrayList<>();

    public LocalVoiceListAdapter(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public LocalVoiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new LocalVoiceViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.local_voice_list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull LocalVoiceViewHolder holder, int position) {
        final LocalVoicePack pack = items.get(position);
        holder.name.setText(pack.getName());
        holder.language.setText(context.getString(R.string.local_voice_language, pack.getLanguage()));

        // Listener trzeba odpiac przed setChecked, inaczej samo przypiecie
        // widoku do nowego elementu wyglada jak zmiana zrobiona przez uzytkownika.
        holder.enabled.setOnCheckedChangeListener(null);
        holder.enabled.setChecked(pack.isEnabled());
        holder.enabled.setContentDescription(pack.getName());
        holder.enabled.setOnCheckedChangeListener(
                (button, checked) -> listener.onLocalVoiceEnabledChanged(pack, checked));

        holder.remove.setContentDescription(
                context.getString(R.string.remove) + ": " + pack.getName());
        holder.remove.setOnClickListener(v -> listener.onLocalVoiceRemoveRequested(pack));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setItems(List<LocalVoicePack> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    public static final class LocalVoiceViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView language;
        final MaterialSwitch enabled;
        final MaterialButton remove;

        LocalVoiceViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.local_voice_name);
            language = itemView.findViewById(R.id.local_voice_language);
            enabled = itemView.findViewById(R.id.local_voice_enabled);
            remove = itemView.findViewById(R.id.local_voice_remove);
        }
    }
}
