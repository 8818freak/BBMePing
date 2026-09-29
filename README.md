# BBMe Ping

Lässt Prioritäts-Nachrichten ("PING") von [BBM Enterprise](https://www.bbm.com/)
auch bei stummgeschaltetem Telefon hörbar durchkommen — als frei einstellbares
Ton-Muster, nicht nur eine Vibration.

## Warum

BBM Enterprise selbst spielt bei einer Prioritäts-Nachricht keinen Ton mehr ab,
sobald das Telefon lautlos gestellt ist — genau dann, wenn es am wichtigsten
wäre. Ein bestehendes Tool ("BBMTweaks") behebt das nur durch Vibration, nie
durch Ton, und reagiert nicht auf einzelne Personen oder den genauen
Telefon-Modus.

## Was es tut

Eine `NotificationListenerService`-App (keine Root-Rechte, kein
Xposed/Hooking-Framework nötig) beobachtet Benachrichtigungen von
`com.bbm.enterprise`. Erkennt sie eine Prioritäts-Nachricht (Kanal-ID enthält
`"priority"`), löst sie ein Ton-Muster aus:

- **N Töne** im Abstand X ms, dann eine **längere Pause**, das Ganze
  **M mal wiederholt** — alles frei einstellbar.
- **Lautstärke** frei wählbar, optional über die Wiederholungen hinweg bis zu
  einem Maximum **ansteigend**.
- Getrennt einstellbar **je Telefon-Modus** (Normal/Vibration/Lautlos) — z. B.
  nur bei Lautlos aktiv, bei Vibration nichts zusätzliches.
- Optional **pro Kontakt** ein eigenes Muster, das die Standard-Einstellung für
  genau diese Person ersetzt (setzt Kontakte-Zugriff voraus — funktioniert,
  weil BBM-Kontakte im normalen Adressbuch des Telefons landen).

Der eigentliche Ton wird **nicht mitgeliefert**: die App liest zur Laufzeit
aus, welchen Ton der Prioritäts-Benachrichtigungskanal von BBM Enterprise
selbst hinterlegt hat (`NotificationChannel.getSound()`), und spielt genau
diese — bereits auf dem Gerät vorhandene — Datei ab. Alternativ lässt sich
in den Einstellungen eine eigene Sounddatei auswählen. So wird keine
Ton-Datei des ursprünglichen Entwicklers kopiert oder weiterverteilt.

Damit ein Ton auch bei lautlos zu hören ist, läuft die Wiedergabe über den
Wecker-Lautstärke-Stream (`AudioAttributes.USAGE_ALARM`) — dieselbe,
öffentlich dokumentierte Technik, über die auch Wecker-Apps bei lautlosem
Telefon noch klingeln. Keine Umgehung von Systemschutz, keine besonderen
Berechtigungen dafür nötig.

## Bauen

Kein Gradle — dieselbe rohe Android-SDK-Kommandozeilen-Toolchain wie
[EdgeTab](https://github.com/8818freak/EdgeTab) und
[Sucher](https://github.com/8818freak/Sucher). Ein eigener, unabhängiger
Signierschlüssel wird empfohlen (nicht denselben wie die beiden anderen Apps
verwenden, da inhaltlich und rechtlich unabhängig).

Gemeinsame Klassen (Benachrichtigungs-Kern u. a.) liegen im Git-Submodul
[`common/`](https://github.com/8818freak/herbers-android-common) — vor dem
Bauen einmal `git submodule update --init` ausführen; der Build kompiliert
`common/src` mit.

```sh
SDK=/path/to/android/sdk
BT="$SDK/build-tools/34.0.0"
AJAR="$SDK/platforms/android-34/android.jar"
KEYSTORE=/path/to/keystore.p12

rm -rf build && mkdir -p build/gen build/obj
"$BT/aapt2" compile --dir res -o build/res.zip
"$BT/aapt2" link -o build/base.apk -I "$AJAR" --manifest AndroidManifest.xml \
  --java build/gen -R build/res.zip --auto-add-overlay \
  --min-sdk-version 29 --target-sdk-version 34
# common/src mitkompilieren (2>/dev/null: fehlendes Submodul stoert nicht)
javac --release 11 -d build/obj -classpath "$AJAR" $(find src build/gen common/src -name '*.java' 2>/dev/null)
"$BT/d8" --min-api 29 --lib "$AJAR" --output build/ $(find build/obj -name '*.class')
cp build/base.apk build/unsigned.apk && (cd build && zip -qj unsigned.apk classes.dex)
"$BT/zipalign" -f -p 4 build/unsigned.apk build/aligned.apk
"$BT/apksigner" sign --ks "$KEYSTORE" --ks-type PKCS12 \
  --out build/BBMePing.apk build/aligned.apk
```

## Grenzen

- Setzt eine installierte, laufende BBM-Enterprise-App voraus, deren
  Prioritäts-Kanal weiterhin `"priority"` im Namen trägt (durch Dekompilieren
  der aktuellen Version bestätigt, kann sich mit einem künftigen BBM-Update
  ändern).
- Personenbezogene Zuordnung läuft über einen exakten Namensabgleich mit dem
  Adressbuch (Anzeigename der Benachrichtigung = Anzeigename des Kontakts) -
  kein Fuzzy-Matching, um nie die falsche Person zu treffen.

## Dokumentation / Documentation

- **Deutsch:** [Anleitung](docs/BBMePing-Anleitung.pdf) · [Werbung](docs/BBMePing-Werbung.pdf)
- **English:** [User guide](docs/BBMePing-Guide.pdf) · [Flyer](docs/BBMePing-Flyer.pdf)

## Lizenz

GNU General Public License v3.0 (oder später) — siehe `LICENSE`.
