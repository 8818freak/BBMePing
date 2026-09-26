# Changelog — BBMe Ping

Alle nennenswerten Änderungen, neueste zuerst.

## 0.5
- Fix: "Wiederholungen" ließ sich nicht auf 0 stellen, weil das Feld
  tatsächlich die Gesamtzahl der Tonfolgen zeigte/einstellte (Minimum 1).
  Zeigt/setzt jetzt wirklich nur die zusätzlichen Wiederholungen (0 = nur
  einmal abspielen).

## 0.4
- Fix: "Ton aus BBMe wählen…" fand die installierten BBM-Töne nur
  gelegentlich, je nach zufälliger impliziter Paket-Sichtbarkeit (Android
  11+ verbirgt fremde Pakete standardmäßig). Manifest deklariert
  com.bbm.enterprise jetzt explizit über `<queries>`, damit das
  zuverlässig funktioniert.

## 0.3
- Fix: die Lautstärke-Einstellungen (Start/Maximum) hatten keine hörbare
  Wirkung - MediaPlayer.setVolume() wurde vor prepare() aufgerufen und auf
  diesem Geraet dadurch stillschweigend ignoriert. Jetzt danach gesetzt.
- Neu: "Über BBMe Ping"-Seite in der App (Version, Lizenz, aufklappbares
  Änderungsprotokoll) - wie bei EdgeTab.

## 0.2
- Neu: Ton direkt aus der installierten BBM-Enterprise-App auswählbar
  ("Ton aus BBMe wählen…") - gegen deren eigene Ressourcen aufgeloest
  (android.resource://-URI), ohne je eine Datei zu kopieren.
- Fix: der "Testen"-Knopf spielte bisher, wenn kein eigener Ton gewaehlt war,
  einen unbeteiligten System-Standardton ab statt eines echten BBM-Tons;
  er nutzt jetzt den zuletzt bei einer echten Prioritaets-Nachricht
  tatsaechlich ermittelten Ton, falls vorhanden.
- Fix: Pause zwischen einzelnen Toenen wartete bisher auf einen festen Timer
  statt auf das tatsaechliche Ende des vorherigen Tons - konnte bei laengeren
  Toenen zu Ueberlappung fuehren.
- Begriff "Stoß"/"Stöße" in der deutschen Oberflaeche durch "Tonfolge"/
  "Tonfolgen" ersetzt.

## 0.1
- Erste Version: Prioritäts-Benachrichtigungen von BBM Enterprise erkennen,
  frei einstellbares Ton-/Wiederholungsmuster (Töne je Tonfolge, Pausen,
  Wiederholungen, Lautstärke mit optionalem Anstieg bis zu einem Maximum),
  je Telefon-Modus (Normal/Vibration/Lautlos) einzeln aktivierbar,
  personenbezogene Muster über Adressbuch-Abgleich, automatische
  Ton-Ermittlung aus dem Benachrichtigungskanal von BBM Enterprise selbst
  (keine mitgelieferte Ton-Datei), Wiedergabe über den Wecker-Lautstärke-Stream
  damit auch bei lautlos hörbar.
