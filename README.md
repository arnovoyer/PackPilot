# PackPilot (Android, Kotlin + Compose)

## Status
MVP-Grundversion ist implementiert mit diesen Funktionen:
- Packlisten erstellen, umbenennen, löschen und öffnen
- Items hinzufügen
- Items abhaken (gepackt / ungepackt)
- Swipe auf Item markiert es als gepackt
- Gepackte Items werden automatisch nach unten sortiert
- Bei 100% gepackten Items erscheint ein Fullscreen-Erfolgsscreen
- Reset-Button zum Zurücksetzen einer Liste
- Lokale Speicherung mit Room-Datenbank

## Projektstruktur
- app/src/main/java/com/packapp/data: Room-Entities, DAO, Datenbank, Repository
- app/src/main/java/com/packapp/ui: ViewModel und Compose-Screens
- app/src/main/java/com/packapp/ui/theme: Theme-Dateien

## Nächste Schritte zum Starten
1. Projekt in Android Studio öffnen.
2. Falls kein Gradle Wrapper vorhanden ist, im Terminal des Projekts Wrapper erzeugen:
   gradle wrapper
3. Danach Projekt synchronisieren und App auf Emulator oder Gerät starten.

## Hinweise
- Aktuell ist Deutsch als Primärsprache im UI gesetzt.
- Daten werden lokal gespeichert (kein Cloud-Sync im MVP).