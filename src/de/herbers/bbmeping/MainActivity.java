package de.herbers.bbmeping;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Einstellungen: Standard-Muster (gilt fuer jeden nicht gesondert
 * behandelten Absender), personenbezogene Muster, und - fuer beide - welche
 * Telefon-Modi ueberhaupt ausloesen sollen. Kein Layout-XML, wie im ganzen
 * Projekt ueblich - alles direkt in Java gebaut.
 */
public class MainActivity extends Activity {

    private static final int REQ_CONTACTS = 401;
    private static final int REQ_PICK_CONTACT = 402;
    private static final int REQ_PICK_SOUND = 403;

    private LinearLayout root;
    private ScrollView scroll;
    private int pendingScrollY = -1;

    /** null = Standard-Muster wird bearbeitet; sonst der LOOKUP_KEY des
     *  gerade bearbeiteten Kontakts. */
    private String editingContactKey;
    private AlertPattern editBuffer;
    /** Nur fuer einen neu hinzugefuegten Kontakt, bis er gespeichert wird. */
    private String pendingNewContactName;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#1C1C1E"));
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int d = dp(1);
        root.setPadding(16 * d, 16 * d, 16 * d, 16 * d);
        scroll.addView(root);
        setContentView(scroll);
        rebuild();
    }

    @Override
    protected void onResume() {
        super.onResume();
        rebuild();
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] results) {
        rebuild();
    }

    @Override
    protected void onActivityResult(int req, int result, Intent data) {
        super.onActivityResult(req, result, data);
        if (result != RESULT_OK || data == null) return;
        if (req == REQ_PICK_CONTACT) {
            resolvePickedContact(data.getData());
        } else if (req == REQ_PICK_SOUND && editBuffer != null) {
            Uri uri = data.getData();
            if (uri != null) {
                try {
                    getContentResolver().takePersistableUriPermission(uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Throwable ignored) {}
                editBuffer.soundUri = uri.toString();
                rebuild();
            }
        }
    }

    /** Zeigt eine Auswahl der Toene, die die installierte BBM-Enterprise-App
     *  selbst mitbringt (siehe InstalledSounds) - direkt gegen deren eigene
     *  Ressourcen aufgeloest, ohne je eine Datei zu kopieren. */
    private void showBbmSoundPicker() {
        List<InstalledSounds.NamedSound> sounds = InstalledSounds.resolve(getPackageManager());
        if (sounds.isEmpty()) {
            new android.app.AlertDialog.Builder(this)
                    .setMessage(R.string.bbm_sound_none_found)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }
        String[] labels = new String[sounds.size()];
        for (int i = 0; i < sounds.size(); i++) {
            labels[i] = InstalledSounds.friendlyName(this, sounds.get(i).rawName);
        }
        new android.app.AlertDialog.Builder(this)
                .setTitle(R.string.bbm_sound_picker_title)
                .setItems(labels, (dlg, which) -> {
                    editBuffer.soundUri = sounds.get(which).uri.toString();
                    rebuild();
                })
                .show();
    }

    private String soundDescription() {
        String uri = editBuffer.soundUri;
        if (uri == null) return getString(R.string.sound_auto_desc);
        if (InstalledSounds.isBbmOwnUri(uri)) {
            String rawName = uri.substring(uri.lastIndexOf('/') + 1);
            // Der Ressourcen-Name steckt nicht in der Uri (nur die numerische
            // Id) - fuer die Anzeige stattdessen einfach den zuletzt gewaehlten
            // Roh-Namen aus InstalledSounds erneut auflösen und vergleichen.
            for (InstalledSounds.NamedSound s : InstalledSounds.resolve(getPackageManager())) {
                if (s.uri.toString().equals(uri)) {
                    return getString(R.string.sound_bbm_prefix, InstalledSounds.friendlyName(this, s.rawName));
                }
            }
            return getString(R.string.sound_bbm_prefix, rawName);
        }
        return getString(R.string.sound_custom_prefix, uri);
    }

    /** Ton fuer den "Testen"-Knopf: der eingestellte eigene/BBM-eigene Ton,
     *  sonst der zuletzt bei einer echten Prioritaets-Nachricht beobachtete
     *  Ton (siehe NotificationCapture/Rules), sonst - nur als allerletzter
     *  Behelf, wenn beides fehlt - ein System-Platzhalter. */
    private Uri testSoundUri() {
        Uri sound = editBuffer.soundUriParsed();
        if (sound != null) return sound;
        String last = Rules.lastKnownAutoSound(this);
        if (last != null) {
            try { return Uri.parse(last); } catch (Throwable ignored) {}
        }
        return android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION);
    }

    private void resolvePickedContact(Uri contactUri) {
        if (contactUri == null) return;
        try (android.database.Cursor c = getContentResolver().query(contactUri,
                new String[]{ContactsContract.Contacts.LOOKUP_KEY, ContactsContract.Contacts.DISPLAY_NAME},
                null, null, null)) {
            if (c != null && c.moveToFirst()) {
                editingContactKey = c.getString(0);
                pendingNewContactName = c.getString(1);
                editBuffer = new AlertPattern();
                rebuild();
            }
        } catch (Throwable ignored) {}
    }

    // ---------- Aufbau ----------

    private void rebuild() {
        if (scroll != null) pendingScrollY = scroll.getScrollY();
        root.removeAllViews();
        int d = dp(1);

        TextView title = new TextView(this);
        title.setText(R.string.app_name);
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setPadding(0, 0, 0, 4 * d);
        root.addView(title);

        TextView desc = new TextView(this);
        desc.setText(R.string.main_description);
        desc.setTextColor(Color.parseColor("#CCCCCC"));
        desc.setTextSize(13);
        desc.setPadding(0, 0, 0, 16 * d);
        root.addView(desc);

        if (!isListenerEnabled()) root.addView(notifPermissionHint(d));

        if (editBuffer != null) {
            buildPatternEditor(d);
        } else {
            buildOverview(d);
        }

        if (pendingScrollY >= 0) {
            final int y = pendingScrollY;
            pendingScrollY = -1;
            scroll.post(() -> scroll.scrollTo(0, y));
        }
    }


    private boolean isListenerEnabled() {
        // Ab API 31 die offizielle Methode - direktes Auslesen von
        // "enabled_notification_listeners" liefert auf neueren Android-
        // Versionen fuer normale (nicht-privilegierte) Apps oft nur noch
        // einen veralteten/leeren Stand.
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            android.app.NotificationManager nm =
                    (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            return nm != null && nm.isNotificationListenerAccessGranted(
                    new ComponentName(this, NotificationCapture.class));
        }
        String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return flat != null && flat.contains(getPackageName());
    }

    private View notifPermissionHint(int d) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#3A2E1E"));
        bg.setCornerRadius(10 * d);
        box.setBackground(bg);
        box.setPadding(14 * d, 12 * d, 14 * d, 12 * d);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = 16 * d;
        box.setLayoutParams(lp);
        TextView t = new TextView(this);
        t.setText(R.string.notif_perm_hint);
        t.setTextColor(Color.parseColor("#FFD9A0"));
        t.setTextSize(13);
        t.setPadding(0, 0, 0, 8 * d);
        box.addView(t);
        Button btn = new Button(this);
        btn.setText(R.string.notif_perm_button);
        btn.setOnClickListener(v -> startActivity(new Intent(
                "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")));
        box.addView(btn);
        return box;
    }

    // ---------- Übersicht ----------

    private void buildOverview(int d) {
        section(root, getString(R.string.section_default_pattern), d);
        TextView sub = new TextView(this);
        sub.setText(summarize(Rules.defaultPattern(this)));
        sub.setTextColor(Color.parseColor("#8899AA"));
        sub.setTextSize(12);
        sub.setPadding(0, 0, 0, 8 * d);
        root.addView(sub);
        Button editDefault = new Button(this);
        editDefault.setText(R.string.button_edit_default);
        editDefault.setOnClickListener(v -> {
            editingContactKey = null;
            editBuffer = Rules.defaultPattern(this);
            rebuild();
        });
        root.addView(editDefault);

        section(root, getString(R.string.section_contact_patterns), d);
        List<Rules.ContactRule> rules = Rules.contactRules(this);
        if (rules.isEmpty()) {
            TextView none = new TextView(this);
            none.setText(R.string.contacts_empty_hint);
            none.setTextColor(Color.parseColor("#8899AA"));
            none.setTextSize(13);
            none.setPadding(0, 0, 0, 8 * d);
            root.addView(none);
        }
        for (Rules.ContactRule r : rules) root.addView(contactRow(r, d));

        Button addContact = new Button(this);
        addContact.setText(R.string.button_add_contact);
        addContact.setOnClickListener(v -> {
            if (!ContactMatcher.hasPermission(this)) {
                requestPermissions(new String[]{android.Manifest.permission.READ_CONTACTS}, REQ_CONTACTS);
                return;
            }
            startActivityForResult(new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI),
                    REQ_PICK_CONTACT);
        });
        root.addView(addContact);

        if (!ContactMatcher.hasPermission(this)) {
            TextView permHint = new TextView(this);
            permHint.setText(R.string.contacts_perm_hint);
            permHint.setTextColor(Color.parseColor("#8899AA"));
            permHint.setTextSize(12);
            permHint.setPadding(0, 8 * d, 0, 0);
            root.addView(permHint);
        }
    }

    private View contactRow(Rules.ContactRule r, int d) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 6 * d, 0, 6 * d);
        TextView name = new TextView(this);
        name.setText(r.displayName + "\n" + summarize(r.pattern));
        name.setTextColor(Color.WHITE);
        name.setTextSize(13);
        name.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(name);
        TextView edit = new TextView(this);
        edit.setText(R.string.button_edit);
        edit.setTextColor(Color.parseColor("#2E9BE6"));
        edit.setPadding(12 * d, 0, 12 * d, 0);
        edit.setOnClickListener(v -> {
            editingContactKey = r.lookupKey;
            editBuffer = r.pattern.copy();
            rebuild();
        });
        row.addView(edit);
        TextView remove = new TextView(this);
        remove.setText("✕");
        remove.setTextColor(Color.parseColor("#FFB0B0"));
        remove.setOnClickListener(v -> { Rules.removeContactRule(this, r.lookupKey); rebuild(); });
        row.addView(remove);
        return row;
    }

    private String summarize(AlertPattern p) {
        List<String> modes = new ArrayList<>();
        for (Mode m : Mode.values()) if (p.modes.contains(m)) modes.add(m.label(this));
        String volume = p.rampEnabled
                ? getString(R.string.pattern_volume_ramp, p.volumeStartPct, p.volumeMaxPct)
                : getString(R.string.pattern_volume_flat, p.volumeStartPct);
        String active = modes.isEmpty() ? getString(R.string.pattern_never_active)
                : getString(R.string.pattern_active_at, TextUtils.join("/", modes));
        return TextUtils.join(", ", new String[]{
                getString(R.string.pattern_tones_burst, p.tonesPerBurst),
                getString(R.string.pattern_repeated, p.burstCount),
                getString(R.string.pattern_pauses, p.toneGapMs, p.burstGapMs),
                volume
        }) + " · " + active;
    }

    // ---------- Muster-Editor (fuer Standard UND Kontakte gleich) ----------

    private void buildPatternEditor(int d) {
        String heading = editingContactKey == null ? getString(R.string.section_default_pattern)
                : (pendingNewContactName != null ? pendingNewContactName
                   : ContactMatcher.displayNameForLookupKey(this, editingContactKey));
        section(root, heading, d);

        root.addView(numberRow(getString(R.string.label_tones_per_burst), editBuffer.tonesPerBurst, 1, 20,
                v -> editBuffer.tonesPerBurst = v, d));
        root.addView(numberRow(getString(R.string.label_burst_count), editBuffer.burstCount, 1, 20,
                v -> editBuffer.burstCount = v, d));
        root.addView(numberRow(getString(R.string.label_tone_gap), editBuffer.toneGapMs, 50, 5000, 50,
                v -> editBuffer.toneGapMs = v, d));
        root.addView(numberRow(getString(R.string.label_burst_gap), editBuffer.burstGapMs, 200, 60000, 200,
                v -> editBuffer.burstGapMs = v, d));
        root.addView(numberRow(getString(R.string.label_volume_start), editBuffer.volumeStartPct, 1, 100,
                v -> editBuffer.volumeStartPct = v, d));

        CheckBox ramp = new CheckBox(this);
        ramp.setText(R.string.checkbox_ramp);
        ramp.setTextColor(Color.WHITE);
        ramp.setChecked(editBuffer.rampEnabled);
        ramp.setOnCheckedChangeListener((cb, on) -> { editBuffer.rampEnabled = on; rebuild(); });
        root.addView(ramp);
        if (editBuffer.rampEnabled) {
            root.addView(numberRow(getString(R.string.label_volume_max), editBuffer.volumeMaxPct, 1, 100,
                    v -> editBuffer.volumeMaxPct = v, d));
        }

        section(root, getString(R.string.section_modes), d);
        for (Mode m : Mode.values()) {
            CheckBox cb = new CheckBox(this);
            cb.setText("  " + m.label(this));
            cb.setTextColor(Color.WHITE);
            cb.setChecked(editBuffer.modes.contains(m));
            cb.setOnCheckedChangeListener((v, on) -> {
                if (on) editBuffer.modes.add(m); else editBuffer.modes.remove(m);
            });
            root.addView(cb);
        }

        section(root, getString(R.string.section_sound), d);
        TextView soundInfo = new TextView(this);
        soundInfo.setText(soundDescription());
        soundInfo.setTextColor(Color.parseColor("#8899AA"));
        soundInfo.setTextSize(12);
        soundInfo.setPadding(0, 0, 0, 8 * d);
        root.addView(soundInfo);
        // Zwei Zeilen statt einer (drei Knoepfe nebeneinander passen auf
        // einem Telefon nicht mehr nebeneinander auf den Bildschirm).
        LinearLayout soundRow = new LinearLayout(this);
        soundRow.setOrientation(LinearLayout.HORIZONTAL);
        Button pickBbmSound = new Button(this);
        pickBbmSound.setText(R.string.button_pick_bbm_sound);
        pickBbmSound.setOnClickListener(v -> showBbmSoundPicker());
        soundRow.addView(pickBbmSound, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Button pickSound = new Button(this);
        pickSound.setText(R.string.button_pick_sound);
        pickSound.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("audio/*");
            i.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(i, REQ_PICK_SOUND);
        });
        soundRow.addView(pickSound, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        root.addView(soundRow);
        if (editBuffer.soundUri != null) {
            Button reset = new Button(this);
            reset.setText(R.string.button_reset_sound);
            reset.setOnClickListener(v -> { editBuffer.soundUri = null; rebuild(); });
            root.addView(reset);
        }

        Button test = new Button(this);
        test.setText(R.string.button_test);
        test.setOnClickListener(v -> {
            Uri sound = testSoundUri();
            AlertPlayer.play(this, "test", sound, editBuffer);
        });
        root.addView(test);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setPadding(0, 16 * d, 0, 0);
        Button save = new Button(this);
        save.setText(R.string.button_save);
        save.setOnClickListener(v -> {
            AlertPlayer.stop();
            if (editingContactKey == null) {
                Rules.setDefaultPattern(this, editBuffer);
            } else {
                String name = pendingNewContactName != null ? pendingNewContactName
                        : ContactMatcher.displayNameForLookupKey(this, editingContactKey);
                Rules.setContactRule(this, editingContactKey, name, editBuffer);
            }
            closeEditor();
        });
        buttons.addView(save);
        Button cancel = new Button(this);
        cancel.setText(R.string.button_cancel);
        cancel.setOnClickListener(v -> { AlertPlayer.stop(); closeEditor(); });
        buttons.addView(cancel);
        root.addView(buttons);
    }

    private void closeEditor() {
        editBuffer = null;
        editingContactKey = null;
        pendingNewContactName = null;
        rebuild();
    }

    // ---------- Kleine Bauhelfer ----------

    private void section(LinearLayout root, String label, int d) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextColor(Color.parseColor("#2E9BE6"));
        t.setTextSize(15);
        t.setPadding(0, 18 * d, 0, 6 * d);
        root.addView(t);
    }

    private interface IntSink { void set(int v); }

    private View numberRow(String label, int value, int min, int max, IntSink sink, int d) {
        return numberRow(label, value, min, max, 1, sink, d);
    }

    private View numberRow(String label, int value, int min, int max, int step, IntSink sink, int d) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 4 * d, 0, 4 * d);
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextColor(Color.WHITE);
        t.setTextSize(13);
        t.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(t);

        TextView valView = new TextView(this);
        valView.setTextColor(Color.parseColor("#2E9BE6"));
        valView.setTextSize(14);
        valView.setPadding(10 * d, 0, 10 * d, 0);
        valView.setText(String.valueOf(value));

        row.setTag(value);
        Button minus = new Button(this);
        minus.setText("−");
        minus.setOnClickListener(v -> {
            int cur = row.getTag() instanceof Integer ? (Integer) row.getTag() : value;
            int nv = Math.max(min, cur - step);
            valView.setText(String.valueOf(nv));
            sink.set(nv);
            row.setTag(nv);
        });
        Button plus = new Button(this);
        plus.setText("+");
        plus.setOnClickListener(v -> {
            int cur = row.getTag() instanceof Integer ? (Integer) row.getTag() : value;
            int nv = Math.min(max, cur + step);
            valView.setText(String.valueOf(nv));
            sink.set(nv);
            row.setTag(nv);
        });

        row.addView(minus);
        row.addView(valView);
        row.addView(plus);
        return row;
    }
}
