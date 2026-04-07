# CustomGUIs

A lightweight Paper plugin that lets you create custom inventory GUIs entirely from a YAML config file — no coding required.

## Features

- Define unlimited inventory GUIs in `config.yml`
- Each GUI gets its own in-game command (e.g. `/help`, `/rules`)
- Supports `&` color codes and `#RRGGBB` hex colors in titles, item names, and lore
- Configurable inventory size (9 – 54 slots)
- Read-only mode to prevent players from taking items
- Per-item click commands — execute any command when a player clicks an item
- Hot-reload without restarting the server (`/cgu reload`)

## Requirements

| Dependency | Version  |
|------------|----------|
| Paper      | 1.21.x   |
| Java       | 21+      |

## Installation

1. Build the plugin with Maven (`mvn clean package`) or download the latest JAR.
2. Place `CustomGUIs.jar` in your server's `plugins/` folder.
3. Start or reload the server — `plugins/CustomGUIs/config.yml` will be generated.
4. Edit `config.yml` to define your GUIs (see [Configuration](#configuration)).
5. Run `/cgu reload` in-game or restart the server to apply changes.

## Commands

| Command      | Description                     | Permission        |
|--------------|---------------------------------|-------------------|
| `/<command>` | Opens the GUI with that command | *(none by default)* |
| `/cgu reload`| Reloads `config.yml`            | `customguis.admin` |

> **Note:** After a reload, newly added GUI commands become available immediately.
> Commands that were *removed* from the config remain registered until the next server restart (Bukkit limitation), but they will no longer open any inventory.

## Configuration

The config file lives at `plugins/CustomGUIs/config.yml`.

### Full GUI entry reference

```yaml
guis:
  <gui-id>:
    command: <command>       # In-game command (without /)
    title: "<title>"         # Inventory title — supports & and #RRGGBB colors
    size: <9|18|27|36|45|54> # Number of slots (must be a multiple of 9)
    readonly: <true|false>   # Prevent players from moving items (default: true)
    items:
      <slot>:                # Slot index (0-based, top-left = 0)
        material: <MATERIAL> # Minecraft material name (e.g. DIAMOND, PAPER)
        name: "<name>"       # Item display name — supports color codes
        lore:                # Optional list of lore lines
          - "<line 1>"
          - "<line 2>"
        command: <command>   # Optional — command run by the player on click (without /)
```

### Color codes

| Format      | Example            | Effect                          |
|-------------|--------------------|---------------------------------|
| `&a`        | `&aGreen text`     | Standard Minecraft color code   |
| `&l`        | `&l&aBold green`   | Bold, italic, underline, etc.   |
| `#RRGGBB`   | `#FF5733Red text`  | Full hex RGB color (1.16+)      |

### Example

```yaml
guis:
  my-menu:
    command: menu
    title: "&8&lMain Menu"
    size: 27
    readonly: true
    items:
      13:
        material: NETHER_STAR
        name: "#FFD700&lServer Shop"
        lore:
          - ""
          - "&7Click to open the shop."
          - ""
        command: shop
```

Players can then type `/menu` to open this GUI.

## Permissions

| Permission        | Description                        | Default |
|-------------------|------------------------------------|---------|
| `customguis.admin`| Allows use of `/cgu reload`        | op      |

GUI commands do not have a built-in permission node. Restrict them using your server's permissions plugin if needed.

## Building from Source

```bash
git clone https://github.com/basti20999/customguis.git
cd customguis
mvn clean package
```

The compiled JAR will be in `target/`.

## License

This project is provided as-is for free use and modification.
