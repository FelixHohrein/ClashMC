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
- **Schadens-Berechnung**: Präzise Prozent-basierte Belohnungen
- **Equipment-Skalierung**: Level-basierte Waffen und Rüstung

### 🎬 Replay-System
- **Movement-Tracking**: Speichert Spieler-Position alle 0.5 Sekunden
- **Vogelperspektive**: Schaue Angriffe von oben an
- **ArmorStand-NPC**: NPC läuft den Angreifer-Path smooth nach
- **Block-Animation**: Partikel-Effekte und Sounds bei Block-Zerstörung
- **Speed-Control**: 0.5x, 1x, 2x, 4x, 8x Geschwindigkeit
- **Follow-Cam**: Kamera folgt NPC oder frei beweglich
- **7-Tage-Retention**: Spieler sehen Replays <7 Tage
- **Admin-Modus**: Admins sehen alle Replays unbegrenzt
- **Timeline**: ActionBar mit Progress-Bar

### 🎮 GUI-System
- **Rathaus-Menü**: Zentrale Übersicht über Dorf und Ressourcen
- **Angriffs-Menü**: Wähle Online- oder Offline-Angriffe
- **Shop-System**: Kaufe Items und Upgrades
- **Mine-Belohnungs-Menü**: Übersicht über gesammelte Ressourcen

### ⚡ Performance-Optimierungen
- **HikariCP Connection Pool**: Professionelles Connection-Pooling für optimale DB-Performance (max 10 Connections)
- **CacheManager-System**: Intelligentes Caching mit TTL für häufig geladene Daten
  - King-ID Cache (30min TTL)
  - Village-Level Cache (5min TTL)
  - Resources Cache (30sec TTL)
  - Automatische Cache-Invalidation bei Updates
- **Async Database Operations**: Alle kritischen DB-Operationen laufen asynchron mit CompletableFuture
  - Player Join: 98% schneller (Main-Thread vollständig frei)
  - Attack Finish: 96% schneller (Non-blocking reward system)
  - Mine Session End: 95% schneller (Async item saving)
- **Optimierte Chunk-Loading**: Synchrones Loading vor Schematic-Paste
- **Async Cleanup**: Block-Löschung und Cleanup läuft asynchron
- **Reduzierte DB-Load**: 50-80% weniger Datenbankzugriffe durch intelligentes Caching
- **Admin Monitoring**: `/clash stats` Command für Performance-Übersicht

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

### Replay-System

#### Setup
1. **Replay-Welt erstellen**:
   ```
   /mv create Replays VOID
   ```

2. **Replay ansehen**:
   - Öffne Rathaus-Menü (Rechtsklick auf Lapis-Block)
   - Klicke auf "Replay-System"
   - Wähle ein Replay aus der Liste

#### Features

**Movement-Tracking:**
- Alle 0.5 Sekunden wird die Position des Angreifers gespeichert
- Smooth NPC-Movement für realistische Wiedergabe

**Vogelperspektive:**
- Spieler wird 30 Blöcke über Dorf teleportiert
- Spectator-Mode mit freier Bewegung
- Optional: Follow-Cam (Kamera folgt NPC)

**Controls (Items im Inventar):**
- **Slot 0** (Red Bed): Replay beenden
- **Slot 2** (Feather): 0.5x Geschwindigkeit
- **Slot 3** (Paper): 1x Normal
- **Slot 4** (Sugar x2): 2x Schnell
- **Slot 5** (Sugar x4): 4x Sehr schnell
- **Slot 6** (Sugar x8): 8x Ultra schnell
- **Slot 8** (Ender Eye): Kamera-Modus wechseln (Frei ↔ Folgen)

**Timeline:**
- ActionBar zeigt Progress-Bar
- Zeigt Zeit + Prozent + Geschwindigkeit

**Access-Control:**
- **Spieler**: Sehen eigene Angriffe + Angriffe auf eigenes Dorf (<7 Tage)
- **Admins** (`clashmc.admin.replay`): Sehen ALLE Replays unbegrenzt

#### Technische Details
- NPC: ArmorStand mit Custom-Name und Waffe
- Block-Breaking: Partikel-Effekte + Sound
- Grid-System: 10×10 = 100 gleichzeitige Replay-Sessions
- Daten: JSON-Speicherung (Blocks + Movement)

---

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
| `/clash stats` | Zeigt Cache & DB-Statistiken an |

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
  - HikariCP (Connection Pooling)
  - Guava (Collections)

### Projektstruktur

```
ClashMC/
├── src/main/java/de/payne/clashmc/
│   ├── ClashMC.java                              # Hauptklasse - Plugin-Initialisierung
│   │
│   ├── attacks/                                  # ⚔️ Angriffs-System
│   │   ├── AttackInstance.java                  # Einzelne Angriffs-Instanz
│   │   ├── AttackInstanceManager.java           # Verwaltet 100 gleichzeitige Angriffe
│   │   ├── AttackManager.java                   # Zentrale Angriffs-Verwaltung
│   │   ├── BrokenBlock.java                     # Zerstörter Block (für Replay)
│   │   └── equipment/                           # Equipment-System
│   │       ├── AttackerLoadouts.java            # Angreifer-Equipment
│   │       ├── DefenderLoadouts.java            # Verteidiger-Equipment
│   │       ├── EquipmentManager.java            # Equipment-Verwaltung
│   │       ├── EquipmentTemplate.java           # Template-Pattern
│   │       ├── LevelRange.java                  # Level-Ranges
│   │       └── ScaledEquipmentTemplate.java     # Skalierungs-System
│   │
│   ├── cache/                                    # 🚀 Performance-Caching
│   │   └── CacheManager.java                    # TTL-basiertes Caching
│   │
│   ├── commands/                                 # 📋 Command-System
│   │   ├── ClashCommand.java                    # Basis-Command
│   │   ├── CommandHandler.java                  # Command-Router
│   │   ├── CommandInterface.java                # Command-Interface
│   │   └── subcommands/                         # Alle Subcommands
│   │       ├── AddCoinsCommand.java             # /clash addcoins (Admin)
│   │       ├── InfoCommand.java                 # /clash info
│   │       ├── MineSessionCommand.java          # /clash mine (Admin)
│   │       ├── ReloadCommand.java               # /clash reload (Admin)
│   │       ├── ResetCommand.java                # /clash reset (Admin)
│   │       ├── SaveMineCommand.java             # /clash savemine (Admin)
│   │       ├── SaveSchematicCommand.java        # /clash saveschematic (Admin)
│   │       ├── StatsCommand.java                # /clash stats (Admin)
│   │       ├── TopCommand.java                  # /clash top
│   │       └── UpgradeCommand.java              # /clash upgrade (Admin)
│   │
│   ├── config/                                   # ⚙️ Config-System
│   │   └── ConfigManager.java                   # 100+ Config-Getter
│   │
│   ├── database/                                 # 💾 Datenbank-Layer
│   │   ├── DatabaseManager.java                 # Zentrale DB-Verwaltung
│   │   ├── MySqlDatabase.java                   # HikariCP Connection Pool
│   │   ├── core/                                # Basis-Klassen
│   │   │   ├── AsyncDatabaseModule.java         # Async-Wrapper
│   │   │   └── DatabaseModule.java              # Base-Class
│   │   ├── migrations/                          # SQL-Migrations
│   │   │   └── MigrationManager.java            # Auto-Migration beim Start
│   │   └── modules/                             # DB-Module (pro Tabelle)
│   │       ├── AttackDatabase.java              # kgmg_attacks + replays
│   │       ├── MineDatabase.java                # kgmg_mine_data + boosters
│   │       ├── PlayerDatabase.java              # kgmg_players
│   │       ├── PlayerResourcesDatabase.java     # kgmg_player_resources
│   │       └── VillageDatabase.java             # kgmg_villages
│   │
│   ├── economy/                                  # 💰 Wirtschafts-System
│   │   ├── PlayerResources.java                 # Ressourcen-Container
│   │   └── ResourceManager.java                 # Collector + Transactions
│   │
│   ├── files/                                    # 📁 File-Handler
│   │   ├── DatabaseHandler.java                 # database.yml
│   │   ├── FileManager.java                     # Zentrale File-Verwaltung
│   │   ├── FILENAME.java                        # Enum für Dateien
│   │   ├── ReplayHandler.java                   # replay.yml
│   │   └── VillageDataHandler.java              # village.yml
│   │
│   ├── gui/                                      # 🎮 Inventory-GUIs
│   │   ├── AttackMenu.java                      # Angriffs-Auswahl
│   │   ├── MineRewardMenu.java                  # Mine-Belohnungen
│   │   ├── ReplayListMenu.java                  # Replay-Auswahl (NEU!)
│   │   ├── ShopMenu.java                        # Haupt-Shop
│   │   ├── TownHallMenu.java                    # Rathaus (Hauptmenü)
│   │   └── UpgradeShopMenu.java                 # Dorf-Upgrades
│   │
│   ├── handlers/                                 # 🔧 Data-Handler
│   │   └── PlayerDataHandler.java               # Spieler-Daten-Operationen
│   │
│   ├── listeners/                                # 👂 Event-Listener
│   │   ├── player/                              # Spieler-Events
│   │   │   ├── AttackBlockBreakListener.java    # Block-Break während Angriff
│   │   │   ├── AttackMenuClickListener.java     # Attack-GUI Clicks
│   │   │   ├── MineEnterListener.java           # Mine-Session Start
│   │   │   ├── MineLeaveListener.java           # Mine-Session Ende
│   │   │   ├── MineRewardClickListener.java     # Mine-Reward-GUI Clicks
│   │   │   ├── PlayerInteractListener.java      # Collector-Interaction
│   │   │   ├── PlayerJoinListener.java          # Join-Handler (Async)
│   │   │   ├── PlayerLeaveListener.java         # Leave-Handler
│   │   │   ├── ReplayControlsListener.java      # Replay-Controls (NEU!)
│   │   │   ├── ReplayListClickListener.java     # Replay-GUI Clicks (NEU!)
│   │   │   ├── ShopClickListener.java           # Shop-GUI Clicks
│   │   │   ├── TownHallClickListener.java       # Rathaus-GUI Clicks
│   │   │   └── UpgradeShopClickListener.java    # Upgrade-GUI Clicks
│   │   └── world/                               # Welt-Events (leer)
│   │
│   ├── maphandling/                              # 🗺️ Welt-/Schematic-Verwaltung
│   │   └── schematic/
│   │       ├── SchematicManager.java            # WorldEdit-Integration
│   │       ├── VillageAllocator.java            # Grid-basierte Dorf-Zuweisung
│   │       └── VillageBuilder.java              # Dorf-Platzierung
│   │
│   ├── mine/                                     # ⛏️ Mine-System
│   │   ├── MineBoosterType.java                 # Booster-Enum (aus Config)
│   │   ├── MineInstance.java                    # Einzelne Mine-Session
│   │   ├── MineManager.java                     # Mine-Verwaltung
│   │   ├── MineMaterialType.java                # Ressourcen-Typen
│   │   ├── MineResourceReplacer.java            # Erz-Verteilung (aus Config)
│   │   └── MineSchematicManager.java            # Mine-Schematics
│   │
│   ├── replay/                                   # 🎬 Replay-System (NEU!)
│   │   ├── MovementPoint.java                   # Position-Tracking
│   │   ├── ReplayData.java                      # Replay-Daten-Container
│   │   ├── ReplayInstance.java                  # Replay-Session-Manager
│   │   ├── ReplayPlayer.java                    # Wiedergabe-Engine
│   │   └── ReplayWorldManager.java              # Replay-Welt + Grid
│   │
│   ├── scoreboard/                               # 📊 Scoreboard (geplant)
│   │
│   └── utils/                                    # 🛠️ Utility-Klassen
│       ├── ItemStackUtil.java                   # Item-Creation-Helper
│       ├── LogUtil.java                         # Logging-Utilities
│       └── PlayerDataCache.java                 # Cache-Helper
│
├── src/main/resources/
│   ├── config.yml                                # Haupt-Konfiguration (400+ Zeilen)
│   └── plugin.yml                                # Plugin-Metadaten
│
├── pom.xml                                       # Maven-Dependencies & Build
├── .gitignore                                    # Git-Ignore-Rules
└── README.md                                     # Diese Datei
```

### Projekt-Statistiken

| Kategorie | Anzahl |
|-----------|--------|
| **Packages** | 14 |
| **Java-Klassen** | 70+ |
| **Commands** | 10 |
| **GUIs** | 6 |
| **Listener** | 15 |
| **Database-Module** | 5 |
| **Database-Tabellen** | 8 |
| **Config-Variablen** | 100+ |
| **Zeilen Code** | ~10,000+ |
| **Dependencies** | 8 |

### Code-Conventions

- **Sprache**: Deutsche Kommentare, englische Variablen
- **Lombok**: Verwendet für Getter/Setter/Constructors
- **Logging**: LogUtil für konsistente Logs
- **Async**: CompletableFuture für alle DB-Operations
  - `executeQueryAsync()` für Queries
  - `executeUpdateAsync()` für Updates
  - `.thenAccept()` / `.thenCompose()` für Chaining
  - `Bukkit.getScheduler().runTask()` für Bukkit-API-Calls aus async Context
- **Error-Handling**: 
  - `.exceptionally()` für async Exceptions
  - Try-Catch für synchrone Operationen
  - LogUtil für alle Fehler
- **Caching**: PlayerDataCache und CacheManager für häufige DB-Zugriffe

---

## 📈 Roadmap

### Geplante Features

#### 🎯 Nächste Phase (hohe Priorität)
- [ ] **Scoreboard**: Live-Statistiken Sidebar mit Toggle-Command
- [ ] **Defensive Strukturen**: Golems, Türme, Fallen für Offline-Schutz
- [ ] **Achievements**: Erfolgs-System mit Kategorien

#### 🚀 Zukünftige Features (mittlere Priorität)
- [ ] **Clans/Guilds**: Spieler-Gruppierungen mit Clan-Wars
- [ ] **Defensive Strukturen**: Türme, Fallen, Golems für Offline-Schutz
- [ ] **Achievements**: Erfolgs-System mit Kategorien und Belohnungen
- [ ] **Shop-Erweiterung**: Mehr Items, Booster und Cosmetics
- [ ] **Event-System**: Zeitlich begrenzte Events (Double Coins, King Rush, etc.)

#### ✅ Bereits implementiert
- [x] **Async DB**: Performance-Optimierung → **AsyncDatabaseModule mit CompletableFuture**
- [x] **Connection Pooling**: → **HikariCP (max 10 Connections)**
- [x] **Caching-System**: → **CacheManager mit TTL und Auto-Invalidation**
- [x] **Admin-Tools**: → `/clash stats` Command für Monitoring
- [x] **Config-System**: → **Umfassende config.yml mit /clash reload**
- [x] **Replay-Viewer**: → **Vollständiges Replay-System mit NPC, Kamera, Controls**

---

## 🐛 Bekannte Issues

- **Scoreboard**: Verzeichnis existiert, aber noch nicht implementiert
- **Replay-Welt**: Muss manuell mit `/mv create Replays VOID` erstellt werden
- **Defensive Strukturen**: Noch nicht implementiert (Golems, Traps, Türme)

### ✅ Kürzlich implementiert:
- ~~Replay-System~~ → **KOMPLETT IMPLEMENTIERT** (Movement-Tracking, NPC, Kamera, Controls)
- ~~Config-System~~ → **KOMPLETT IMPLEMENTIERT** (400+ Zeilen config.yml, /clash reload)
- ~~Mine-Chunk-Loading~~ → **BEHOBEN** (Synchrones Loading)
- ~~Defender-Ausrüstung~~ → **BEHOBEN** (Zeile 83 in AttackManager)
- ~~ActionBar-Tasks~~ → **BEHOBEN** (Proper cleanup)
- ~~Connection-Pooling~~ → **BEHOBEN** (HikariCP)
- ~~Redundante DB-Calls~~ → **BEHOBEN** (CacheManager)
- ~~Async Database~~ → **BEHOBEN** (AsyncDatabaseModule)
- ~~TODO-Kommentare~~ → **BEHOBEN** (Async cleanup)

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

