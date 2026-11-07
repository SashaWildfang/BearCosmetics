# BearCosmetics

A cosmetics system for the **Bear Nation** network. Players browse, buy and equip particle **trails** and **auras** from an in-game menu. Ownership is stored in MySQL, so it carries across servers.

![Paper](https://img.shields.io/badge/Paper-1.21-blue) ![Java](https://img.shields.io/badge/Java-21-orange) ![Build](https://img.shields.io/badge/build-Gradle-02303A)

## Features

- **Cosmetics menu** (`/cosmetics`) with category pages and a purchase confirmation screen
- **Trails** and **auras**: particle effects that follow the player
- **MySQL storage** for ownership and equipped cosmetics
- **In-memory cache** of the cosmetic catalogue for fast menu loads
- A menu item that opens the cosmetics menu

## Commands

| Command | Description | Permission | Default |
|---|---|---|---|
| `/cosmetics` | Open the cosmetics menu | `bearcosmetics.cmd.cosmetics` | everyone |

## Configuration

`plugins/BearCosmetics/config.yml` is created on first start. Fill in your database details:

```yaml
database:
  host: localhost
  port: 3306
  name: bearnation
  user: ""
  password: ""
```

## Requirements

- Paper 1.21+
- Java 21
- A MySQL / MariaDB database

## Building

```bash
gradle build
```

The plugin jar goes to `build/libs/`.

## Project layout

```
src/main/java/com/bearnation/bearCosmetics/
├── BearCosmetics.java   # Plugin entry point
├── cache/               # Cosmetic catalogue cache
├── commands/            # /cosmetics
├── cosmetics/           # Trail and aura renderers
├── database/            # MySQL connection handling
├── gui/                 # Menus
├── listeners/           # Join, GUI and item listeners
└── utils/               # Item and GUI builders
```

---

Part of the Bear Nation plugin suite.
