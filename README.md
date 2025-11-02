# 🏰 ClashMC - Clash of Clans in Minecraft

![Version](https://img.shields.io/badge/version-0.0.1--SNAPSHOT-blue)
![Minecraft](https://img.shields.io/badge/minecraft-1.21.5-green)
![Java](https://img.shields.io/badge/java-21-orange)
![License](https://img.shields.io/badge/license-Custom-red)

Ein umfangreiches Minecraft-Plugin, das Clash of Clans-Mechaniken in Minecraft implementiert. Spieler können eigene Dörfer bauen und upgraden, Ressourcen sammeln, andere Spieler angreifen und in Minen Rohstoffe abbauen.

## 📋 Inhaltsverzeichnis

- [Features](#-features)
- [Installation](#-installation)
- [Welten-Setup](#-welten-setup)
- [Datenbank-Konfiguration](#-datenbank-konfiguration)
- [Spielmechaniken](#-spielmechaniken)
  - [Dorf-System](#dorf-system)
  - [Ressourcen-System](#ressourcen-system)
  - [Mine-System](#mine-system)
  - [Angriffs-System](#angriffs-system)
- [Commands](#-commands)
- [Permissions](#-permissions)
- [Datenbank-Struktur](#-datenbank-struktur)
- [Entwicklung](#-entwicklung)
- [Credits](#-credits)

---

## ✨ Features

### 🏘️ Dorf-System
- **Eigene Dörfer**: Jeder Spieler erhält ein eigenes Dorf in einem 5x5 Chunk Grid
- **Level-basierte Upgrades**: Dörfer können aufgerüstet werden mit progressiv steigenden Kosten
- **WorldEdit-Integration**: Dörfer werden als Schematics gespeichert und geladen
- **Automatische Verwaltung**: Grid-System mit automatischer Chunk-Zuweisung

### 💰 Zwei Währungssysteme
- **Clash Coins**: Hauptwährung für Upgrades und Shop
  - Verdient durch Ressourcensammler (zeitbasiert)
  - Verdient durch Mine-Sessions
  - Verdient durch Angriffe auf andere Spieler
- **King Coins**: Premium-Währung
  - Nur bei perfekten Angriffen (≥90% Zerstörung)
  - Seltene Belohnung für skilled Gameplay

### ⛏️ Mine-System
- **10-Minuten-Sessions**: Zeitbegrenzte Mining-Sessions
- **Instanzierte Minen**: Jeder Spieler bekommt seine eigene Mine
- **Level-abhängige Erze**: Höheres Dorflevel = bessere Erze
- **Spitzhacken-Upgrades**: Verbessere deine Spitzhacke dauerhaft
- **Booster-System**: 
  - Ressourcen-Multiplikator (2.2x - 10x je nach Erz)
  - Temporärer Spitzhacken-Level-Boost (+20 Level)

### ⚔️ Angriffs-System
- **Online-Angriffe**: Beide Spieler kämpfen gleichzeitig
- **Offline-Angriffe**: Greife Spieler an, die nicht online sind
- **Level-Matching**: Angriffe nur gegen Spieler mit ähnlichem Level (±20)
- **3-Minuten-Timer**: Schnelles, actionreiches Gameplay
- **Replay-System**: Alle Angriffe werden als JSON gespeichert
- **Schadens-Berechnung**: Präzise Prozent-basierte Belohnungen
- **Equipment-Skalierung**: Level-basierte Waffen und Rüstung

### 🎮 GUI-System
- **Rathaus-Menü**: Zentrale Übersicht über Dorf und Ressourcen
- **Angriffs-Menü**: Wähle Online- oder Offline-Angriffe
- **Shop-System**: Kaufe Items und Upgrades
- **Mine-Belohnungs-Menü**: Übersicht über gesammelte Ressourcen

---

## 📦 Installation

### Voraussetzungen

- **Minecraft Server**: Paper 1.21.5 oder höher
- **Java**: Version 21 oder höher
- **MySQL**: Version 8.0 oder höher
- **Plugins**:
  - WorldEdit (7.3.13 oder höher)
  - Multiverse-Core

### Installations-Schritte

1. **Plugin herunterladen**
   ```bash
   # Projekt klonen und bauen
   git clone git@github.com:FelixHohrein/ClashMC.git
   cd ClashMC
   mvn clean install
   ```

2. **Plugin installieren**
   ```bash
   # ClashMC.jar ins plugins-Verzeichnis kopieren
   cp target/ClashMC.jar /pfad/zum/server/plugins/
   ```

3. **Abhängigkeiten installieren**
   - WorldEdit herunterladen und ins plugins-Verzeichnis legen
   - Multiverse-Core herunterladen und ins plugins-Verzeichnis legen

4. **Server starten** (erstes Mal)
   - Server starten, damit Konfigurationsdateien erstellt werden
   - Server wieder stoppen für Konfiguration

5. **Datenbank konfigurieren**
   - Siehe [Datenbank-Konfiguration](#-datenbank-konfiguration)

6. **Welten erstellen**
   - Siehe [Welten-Setup](#-welten-setup)

7. **Server neu starten**

---

## 🌍 Welten-Setup

ClashMC benötigt **drei separate Welten**:

### 1. Clash-Welt (Hauptwelt für Dörfer)
```bash
# Mit Multiverse erstellen
/mv create Clash NORMAL
```
**Eigenschaften**: Keine Mobs, kein PVP, kein Tag/Nacht-Zyklus

### 2. Attacks-Welt (für Angriffe)
```bash
/mv create Attacks FLAT
```
**Eigenschaften**: PVP aktiviert, keine Mobs, zeitlicher Stillstand

### 3. Mine-Welt (für Ressourcenabbau)
```bash
/mv create mine NORMAL
```
**Eigenschaften**: Keine Mobs, keine Block-Drops außer Erze

**Hinweis**: Die Welt-Namen müssen exakt `Clash`, `Attacks` und `mine` lauten!

---

## 🗄️ Datenbank-Konfiguration

### MySQL-Datenbank erstellen

```sql
CREATE DATABASE clashmc CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'clashmc'@'localhost' IDENTIFIED BY 'dein_sicheres_passwort';
GRANT ALL PRIVILEGES ON clashmc.* TO 'clashmc'@'localhost';
FLUSH PRIVILEGES;
```

### database.yml konfigurieren

Die Datei wird automatisch erstellt unter `plugins/ClashMC/database.yml`:

```yaml
host: localhost
port: 3306
database: clashmc
username: clashmc
password: dein_sicheres_passwort
```

**Wichtig**: Die Migrations werden automatisch beim ersten Start ausgeführt!

---

## 🎮 Spielmechaniken

### Dorf-System

#### Dorf-Erstellung
- Beim ersten Join erhält jeder Spieler automatisch ein Dorf
- Dörfer werden in einem Grid-System (5x5 Chunks pro Dorf) verwaltet
- Startet bei Level 1

#### Dorf-Upgrade
- Kosten-Formel: `1000 + 500 × Level + 30 × Level^1.5` Clash Coins
- Beispiel-Kosten:
  - Level 2: ~1,585 Coins
  - Level 5: ~3,835 Coins
  - Level 10: ~9,950 Coins
  - Level 20: ~24,683 Coins

#### Schematic-System
- Dörfer werden als WorldEdit-Schematics gespeichert
- Beim Upgrade: altes Dorf wird gelöscht, neues wird platziert
- Schematics müssen im entsprechenden Ordner abgelegt werden

---

### Ressourcen-System

#### Ressourcensammler
- **Aktivierung**: Rechtsklick auf LAPIS_BLOCK
- **Sammelrate**: 5% Basis + 1% pro Dorflevel
- **Maximum**: 12 Stunden Offline-Zeit werden berücksichtigt
- **Minimum**: 1 Minute muss vergangen sein
- **Formel**: `(Zeit_in_Sekunden × Effizienz)`

**Beispiel-Berechnung**:
- Dorflevel 10 = 15% Effizienz (0.15)
- 1 Stunde Offline = 3600 Sekunden
- Ertrag: 3600 × 0.15 = 540 Clash Coins

#### Transaktionen
- Alle Ressourcen-Änderungen werden in Echtzeit in der DB gespeichert
- Balance-Checks verhindern negative Kontostände
- Feedback-Messages an Spieler

---

### Mine-System

#### Session-Ablauf
1. Spieler startet Mine-Session über Command oder GUI
2. Instanzierte Mine wird generiert (eigene für jeden Spieler)
3. 10-Minuten-Timer startet mit ActionBar-Anzeige
4. Spieler erhält Spitzhacke basierend auf seinem Level
5. Gesammelte Ressourcen werden automatisch gespeichert
6. Am Ende: Teleport zurück, Inventar wird geleert

#### Ressourcen-Typen

| Material | Display | Wert | Typ |
|----------|---------|------|-----|
| COBBLESTONE | Stein | 1 | Baumaterial |
| OAK_PLANKS | Holz | 2 | Baumaterial |
| OAK_FENCE | Holzzaun | 2 | Baumaterial |
| RAIL | Schienen | 3 | Baumaterial |
| RAW_COPPER | Kupfer | 5 | Baumaterial |
| COAL | Kohle | - | Wertvoll |
| RAW_IRON | Eisen | - | Wertvoll |
| RAW_GOLD | Gold | - | Wertvoll |
| DIAMOND | Diamant | - | Wertvoll |
| EMERALD | Smaragd | - | Wertvoll |

#### Erz-Verteilung
- Basiert auf Formel: `villageLevel / maxLevel`
- Höheres Level = mehr seltene Erze
- Dynamisches Replacement von STONE-Blöcken

#### Booster

**Resource Multiplier**:
- Kohle: 3.0x
- Eisen: 2.7x
- Gold: 2.5x
- Diamant: 2.2x
- Smaragd: 10.0x

**Pickaxe Level Plus**: +20 Level temporär

---

### Angriffs-System

#### Angriffs-Typen

**Online-Angriff**:
- Beide Spieler müssen registriert sein
- Beide erhalten Level-basiertes Equipment
- Echtzeit-PvP
- Höhere Belohnungen

**Offline-Angriff**:
- Verteidiger ist nicht online
- Defensive Strukturen (geplant: Golems, Traps)
- Nur registrierte Nicht-Online-Spieler als Ziele

#### Angriffs-Ablauf

1. **Ziel-Auswahl**: Level-Matching ±20
2. **Instance-Erstellung**: Verteidiger-Dorf wird geladen
3. **Scan**: Alle zerstörbaren Blöcke werden gezählt
4. **Timer Start**: 3 Minuten
5. **ActionBar**: Zeigt verbleibende Zeit und aktuellen Schaden%
6. **Block-Tracking**: Jeder zerstörte Block wird gespeichert
7. **Auswertung**: Schaden-Berechnung und Belohnungen
8. **Replay-Speicherung**: JSON-Export aller zerstörten Blöcke
9. **Cleanup**: Teleport, Inventory-Clear, Schematic-Löschung

#### Schaden-Berechnung
```
Schaden% = (Zerstörte relevante Blöcke / Gesamt zerstörbare Blöcke) × 100
```

**Nicht zerstörbar**: AIR, BARRIER, BEDROCK, GRASS_BLOCK, TALL_GRASS, SHORT_GRASS, POPPY, DANDELION, COARSE_DIRT, DIRT_PATH, ROOTED_DIRT

#### Belohnungen

- **Clash Coins**: `Schaden% × 10`
  - Beispiel: 62.5% Schaden = 625 Coins
- **King Coins**: `1` wenn Schaden ≥ 90%, sonst `0`
- **Ressourcen-Transfer**:
  - Angreifer: +berechnete Coins
  - Verteidiger: -berechnete Coins / 2

#### Equipment-Skalierung
- Basiert auf: `villageLevel / maxLevel` (0.0 - 1.0)
- AttackerLoadouts: Offensiv-fokussiert
- DefenderLoadouts: Defensiv-fokussiert
- Automatische Slot-Zuweisung (Rüstung, Schwert, Tools)

#### Replay-System
Jeder Angriff wird gespeichert als:
```json
[
  {
    "material": "STONE_BRICKS",
    "x": 10,
    "y": 5,
    "z": 8,
    "timestamp": 1699012345678
  },
  ...
]
```

---

## 📝 Commands

### Öffentliche Commands

| Command | Beschreibung |
|---------|-------------|
| `/clash` | Haupt-Command (zeigt Hilfe) |
| `/clash info` | Plugin-Informationen |
| `/clash top` | Rangliste (Top-Spieler) |

### Admin-Commands

**Permission erforderlich**: `clashmc.admin`

| Command | Beschreibung |
|---------|-------------|
| `/clash reset <spieler>` | Setzt Spielerdaten zurück |
| `/clash saveschematic <level>` | Speichert aktuelles Dorf als Schematic |
| `/clash upgrade <spieler>` | Forciert Dorf-Upgrade |
| `/clash addcoins <spieler> <clash\|king> <menge>` | Fügt Coins hinzu |
| `/clash savemine` | Speichert Mine-Schematic |
| `/clash mine <spieler>` | Startet Mine-Session für Spieler |

---

## 🔐 Permissions

| Permission | Beschreibung | Standard |
|-----------|-------------|----------|
| `clashmc.admin` | Admin-Befehle aktivieren | OP |

---

## 🗄️ Datenbank-Struktur

### Tabellen-Übersicht

#### `kgmg_players`
```sql
- id (INT, AUTO_INCREMENT, PRIMARY KEY) -- king_id
- uuid (VARCHAR, UNIQUE)
- language (VARCHAR)
- created_at (TIMESTAMP)
- last_seen (TIMESTAMP)
```

#### `kgmg_villages`
```sql
- king_id (INT, PRIMARY KEY, FK)
- level (INT, DEFAULT 1)
- last_attacked (TIMESTAMP, NULL)
```

#### `kgmg_player_resources`
```sql
- king_id (INT, PRIMARY KEY, FK)
- clash_coins (BIGINT, DEFAULT 0)
- king_coins (BIGINT, DEFAULT 0)
- last_collector_use (BIGINT) -- Millisekunden
```

#### `kgmg_mine_data`
```sql
- king_id (INT, FK)
- material_type (VARCHAR)
- amount (INT)
- pickaxe_level (INT)
```

#### `kgmg_attacks`
```sql
- attack_id (INT, AUTO_INCREMENT, PRIMARY KEY)
- attacker_id (INT, FK)
- defender_id (INT, FK)
- is_online (BOOLEAN)
- damage_percent (DOUBLE)
- clash_coins_looted (BIGINT)
- king_coins_looted (BIGINT)
- attack_time (TIMESTAMP)
- replay (TEXT) -- JSON
```

#### `kgmg_attack_optin`
```sql
- king_id (INT, PRIMARY KEY, FK)
- opt_in_online_attacks (BOOLEAN)
```

---

## 🛠️ Entwicklung

### Build-Anleitung

```bash
# Projekt klonen
git clone git@github.com:FelixHohrein/ClashMC.git
cd ClashMC

# Mit Maven bauen
mvn clean install

# JAR-Datei findet sich dann in:
# target/ClashMC.jar
```

### Technologie-Stack

- **Java**: 21
- **Build-Tool**: Maven
- **Server**: Paper 1.21.5
- **Datenbank**: MySQL 8.0
- **Bibliotheken**:
  - WorldEdit (Schematic-Verwaltung)
  - Lombok (Boilerplate-Reduktion)
  - Gson (JSON-Serialisierung)
  - MySQL Connector (Datenbank)

### Projektstruktur

```
ClashMC/
├── src/main/java/de/payne/clashmc/
│   ├── ClashMC.java              # Hauptklasse
│   ├── attacks/                  # Angriffs-System
│   │   ├── AttackManager.java
│   │   ├── AttackInstance.java
│   │   ├── AttackInstanceManager.java
│   │   └── equipment/            # Equipment-Skalierung
│   ├── commands/                 # Command-Handler
│   ├── database/                 # Datenbank-Layer
│   │   ├── DatabaseManager.java
│   │   ├── modules/              # DB-Module pro Tabelle
│   │   └── migrations/           # SQL-Migrations
│   ├── economy/                  # Währungs-System
│   ├── files/                    # Config-Handler
│   ├── gui/                      # Inventory-GUIs
│   ├── handlers/                 # Data-Handler
│   ├── listeners/                # Event-Listener
│   ├── maphandling/              # Welt-/Schematic-Verwaltung
│   ├── mine/                     # Mine-System
│   ├── scoreboard/               # Scoreboard (geplant)
│   └── utils/                    # Utility-Klassen
├── src/main/resources/
│   └── plugin.yml                # Plugin-Konfiguration
├── pom.xml                       # Maven-Konfiguration
└── README.md                     # Diese Datei
```

### Code-Conventions

- **Sprache**: Deutsche Kommentare, englische Variablen
- **Lombok**: Verwendet für Getter/Setter/Constructors
- **Logging**: LogUtil für konsistente Logs
- **Async**: Blocking DB-Calls (könnte optimiert werden)
- **Error-Handling**: Try-Catch mit SQLException

---

## 📈 Roadmap

### Geplante Features

- [ ] **Replay-Viewer**: Angriffe visuell wiedergeben
- [ ] **Clans/Guilds**: Spieler-Gruppierungen
- [ ] **Defensive Strukturen**: Türme, Fallen, Golems für Offline-Schutz
- [ ] **Achievements**: Erfolgs-System
- [ ] **Scoreboard**: Live-Statistiken
- [ ] **Shop-Erweiterung**: Mehr Items und Booster
- [ ] **Event-System**: Zeitlich begrenzte Events
- [ ] **Async DB**: Performance-Optimierung
- [ ] **Config-System**: Mehr Anpassungsmöglichkeiten

---

## 🐛 Bekannte Issues

- Mine-Chunk-Loading kann manchmal verzögert sein
- Replay-System speichert, aber Viewer fehlt noch
- Defender-Ausrüstung wird auch bei Online-Angriffen als Attacker-Equipment vergeben (Zeile 83 in AttackManager.java)
- Keine Config-Optionen für Timing/Kosten (hardcoded)

---

## 🤝 Contributing

Contributions sind willkommen! 

1. Fork das Projekt
2. Erstelle einen Feature-Branch (`git checkout -b feature/AmazingFeature`)
3. Commit deine Änderungen (`git commit -m 'Add some AmazingFeature'`)
4. Push zum Branch (`git push origin feature/AmazingFeature`)
5. Öffne einen Pull Request

---

## 📄 License

Dieses Projekt ist unter einer Custom License. Kontaktiere den Autor für Details.

---

## 👨‍💻 Credits

**Entwickler**: Felix Payne

**Inspiration**: Clash of Clans by Supercell

---

## 📞 Kontakt

Bei Fragen oder Problemen:
- GitHub Issues: [ClashMC Issues](https://github.com/FelixHohrein/ClashMC/issues)
- Repository: [ClashMC](https://github.com/FelixHohrein/ClashMC)

---

<div align="center">

**Viel Spaß beim Clashen! ⚔️**

Made with ❤️ for the Minecraft Community

</div>

