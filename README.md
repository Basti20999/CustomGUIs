# CustomGUIs

A lightweight Paper plugin that lets you create custom inventory GUIs entirely from a YAML config file — no coding required.

## Features

- Define unlimited inventory GUIs in `config.yml`
- Each GUI gets its own in-game command (e.g. `/help`, `/rules`)
- Per-click-type dispatch (left, right, shift-left, shift-right, middle)
- Run actions as the player **or** as the console
- `open:` action to chain GUIs together (navigation menus)
- `border:` and `fill-empty:` shortcuts — no more pasting 16 filler entries
- Per-item permissions (item is hidden if the player lacks the node)
- Built-in placeholders: `%player%`, `%player_uuid%`, `%world%`, `%x%`, `%y%`, `%z%`
- Sounds on open and on click
- Item glow / enchantment glint
- `&` color codes and `#RRGGBB` hex colors in titles, item names, and lore
- Configurable inventory size (9 – 54 slots)
- Read-only mode to prevent players from taking items
- Hot-reload without restarting the server (`/cgu reload`)
- Tab completion on `/cgu`

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

| Command                       | Description                                | Permission                |
|-------------------------------|--------------------------------------------|---------------------------|
| `/<command>`                  | Opens the GUI with that command            | *(none by default)*       |
| `/cgu reload`                 | Reloads `config.yml`                       | `customguis.admin.reload` |
| `/cgu list`                   | Lists all loaded GUIs                      | `customguis.admin.list`   |
| `/cgu open <id> [player]`     | Opens a GUI for yourself or another player | `customguis.admin.open`   |
| `/cgu help`                   | Shows the admin help                       | *(none)*                  |

> **Note:** After a reload, newly added GUI commands become available immediately.
> Commands that were *removed* from the config remain registered until the next server restart (Bukkit limitation), but they will no longer open any inventory.

## Configuration

The config file lives at `plugins/CustomGUIs/config.yml`.

### Full GUI entry reference

```yaml
guis:
  <gui-id>:
    command: <command>            # In-game command (without /)
    title: "<title>"              # Inventory title — supports & and #RRGGBB colors and placeholders
    size: <9|18|27|36|45|54>      # Number of slots
    readonly: <true|false>        # Default: true. Prevents players from moving items.
    close-on-click: <true|false>  # Default: same as `readonly`
    open-sound: <SOUND_NAME>      # Optional. Bukkit Sound enum name.

    border:                       # Optional. Fills perimeter slots not in `items`.
      material: GRAY_STAINED_GLASS_PANE
      name: "&r"

    fill-empty:                   # Optional. Fills any still-empty slots.
      material: BLACK_STAINED_GLASS_PANE
      name: "&r"

    items:
      <slot>:
        material: <MATERIAL>
        name: "<name>"
        lore:
          - "<line 1>"
          - "<line 2>"
        amount: <1-64>            # Default 1
        glow: <true|false>        # Default false
        permission: <node>        # Optional. Item hidden if missing.
        click-sound: <SOUND_NAME> # Optional.

        # ANY-CLICK action (omit if using on-click):
        command: <command>
        console-command: <command>
        open: <gui-id>
        message: "<text>"

        # OR per-click-type dispatch:
        on-click:
          left:        { command: shop }
          right:       { console-command: "give %player% diamond 1" }
          shift-left:  { open: another-gui }
          middle:      { message: "&aHi %player%!" }
```

### Action types

| Field             | Effect                                          |
|-------------------|-------------------------------------------------|
| `command`         | Runs the command as the clicking player         |
| `console-command` | Runs the command as the server console          |
| `open`            | Opens another GUI by its id                     |
| `message`         | Sends a chat message to the clicking player     |

Multiple action fields can coexist on the same item — they all fire on click.

### Placeholders

Resolved in `title`, item `name`, item `lore`, `message`, `command`, and `console-command`:

`%player%` `%player_uuid%` `%world%` `%x%` `%y%` `%z%`

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
    title: "&8&lMain Menu &7- &f%player%"
    size: 27
    open-sound: BLOCK_CHEST_OPEN

    fill-empty:
      material: BLACK_STAINED_GLASS_PANE
      name: "&r"

    items:
      13:
        material: NETHER_STAR
        name: "#FFD700&lServer Shop"
        lore:
          - ""
          - "&7Left-click to open the shop."
          - "&7Right-click for daily reward."
          - ""
        glow: true
        click-sound: UI_BUTTON_CLICK
        on-click:
          left:
            open: shop-menu
          right:
            console-command: "give %player% diamond 1"
            message: "&aDaily diamond claimed!"
```

## Permissions

| Permission                  | Description                  | Default |
|-----------------------------|------------------------------|---------|
| `customguis.admin`          | Parent for all admin nodes   | op      |
| `customguis.admin.reload`   | Allows `/cgu reload`         | op      |
| `customguis.admin.list`     | Allows `/cgu list`           | op      |
| `customguis.admin.open`     | Allows `/cgu open`           | op      |

GUI commands themselves do not have a built-in permission node. To restrict who can open a GUI, set per-item `permission:` on its entry items, or restrict the command in your permissions plugin.

## Building from Source

```bash
git clone https://github.com/basti20999/customguis.git
cd customguis
mvn clean package
```

The compiled JAR will be in `target/`.

## License

This project is provided as-is for free use and modification.
