package de.herbers.bbmeping;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * Regel-Ablage: eine Standard-Regel (gilt fuer alle nicht gesondert
 * behandelten Absender) und beliebig viele personenbezogene Regeln, die die
 * Standard-Regel fuer einen bestimmten Kontakt ersetzen (Mathias: "abhaengig
 * von der Person und dem Modus"). Jede Regel traegt selbst, in welchen
 * Telefon-Modi sie ueberhaupt gilt.
 *
 * Speicherung als einfache SharedPreferences-Werte (kein JSON/keine
 * Bibliothek, wie im ganzen Projekt ueblich) - der Kontakt-Schluessel ist
 * ContactsContract's LOOKUP_KEY, stabil ueber Synchronisationen hinweg.
 */
final class Rules {

    private static final String PREFS = "bbmeping_rules";
    private static final String K_DEFAULT = "default_pattern";
    private static final String K_CONTACT_KEYS = "contact_keys"; // -getrennt
    private static final String K_LAST_AUTO_SOUND = "last_auto_sound";

    private Rules() {}

    private static SharedPreferences p(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static AlertPattern defaultPattern(Context ctx) {
        String s = p(ctx).getString(K_DEFAULT, null);
        return s == null ? new AlertPattern() : AlertPattern.decode(s);
    }

    static void setDefaultPattern(Context ctx, AlertPattern pattern) {
        p(ctx).edit().putString(K_DEFAULT, pattern.encode()).apply();
    }

    /** Der Ton, den BBM Enterprise selbst fuer eine echte Prioritaets-
     *  Nachricht meldet (siehe SoundLocator), zuletzt gemerkt von
     *  NotificationCapture. Dient nur dem "Testen"-Knopf als realistische
     *  Vorschau, wenn (noch) kein eigener Ton gewaehlt wurde - so muss BBM
     *  nie selbst Toene beilegen, sondern nur einmal beobachtet werden. */
    static String lastKnownAutoSound(Context ctx) {
        return p(ctx).getString(K_LAST_AUTO_SOUND, null);
    }

    static void setLastKnownAutoSound(Context ctx, String uriString) {
        p(ctx).edit().putString(K_LAST_AUTO_SOUND, uriString).apply();
    }

    static final class ContactRule {
        final String lookupKey;
        final String displayName;
        final AlertPattern pattern;
        ContactRule(String lookupKey, String displayName, AlertPattern pattern) {
            this.lookupKey = lookupKey; this.displayName = displayName; this.pattern = pattern;
        }
    }

    static List<ContactRule> contactRules(Context ctx) {
        List<ContactRule> out = new ArrayList<>();
        for (String key : keys(ctx)) {
            SharedPreferences sp = p(ctx);
            String name = sp.getString("contact_name_" + key, key);
            String enc = sp.getString("contact_pattern_" + key, null);
            out.add(new ContactRule(key, name, enc == null ? new AlertPattern() : AlertPattern.decode(enc)));
        }
        return out;
    }

    static void setContactRule(Context ctx, String lookupKey, String displayName, AlertPattern pattern) {
        List<String> ks = keys(ctx);
        if (!ks.contains(lookupKey)) { ks.add(lookupKey); saveKeys(ctx, ks); }
        p(ctx).edit()
                .putString("contact_name_" + lookupKey, displayName)
                .putString("contact_pattern_" + lookupKey, pattern.encode())
                .apply();
    }

    static void removeContactRule(Context ctx, String lookupKey) {
        List<String> ks = keys(ctx);
        ks.remove(lookupKey);
        saveKeys(ctx, ks);
        p(ctx).edit().remove("contact_name_" + lookupKey).remove("contact_pattern_" + lookupKey).apply();
    }

    private static List<String> keys(Context ctx) {
        String raw = p(ctx).getString(K_CONTACT_KEYS, "");
        List<String> out = new ArrayList<>();
        if (!raw.isEmpty()) for (String k : raw.split("")) if (!k.isEmpty()) out.add(k);
        return out;
    }

    private static void saveKeys(Context ctx, List<String> ks) {
        p(ctx).edit().putString(K_CONTACT_KEYS, String.join("", ks)).apply();
    }

    /** Welches Muster fuer diesen Absender im aktuellen Modus gilt - eine
     *  personenbezogene Regel geht vor der Standard-Regel, aber nur wenn sie
     *  fuer den aktuellen Modus ueberhaupt aktiv ist; sonst zaehlt die
     *  Standard-Regel (auch nur, wenn sie fuer diesen Modus aktiv ist).
     *  null = in diesem Modus soll fuer diesen Absender gar nichts ausgeloest
     *  werden. */
    static AlertPattern resolve(Context ctx, String contactLookupKey, Mode mode) {
        if (contactLookupKey != null) {
            for (ContactRule r : contactRules(ctx)) {
                if (r.lookupKey.equals(contactLookupKey)) {
                    return r.pattern.modes.contains(mode) ? r.pattern : null;
                }
            }
        }
        AlertPattern def = defaultPattern(ctx);
        return def.modes.contains(mode) ? def : null;
    }
}
