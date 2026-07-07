# Mindustry Plugin

Integrated server platform with a built-in web interface, Telegram bot, custom event modules, and granular permissions with 30+ commands.

<img alt="Mindustry" src="https://img.shields.io/badge/Mindustry-v157-9cf"> <img alt="Java" src="https://img.shields.io/badge/Java-17-blue"> <img alt="Gradle" src="https://img.shields.io/badge/Gradle-8.14-green"> <img alt="License" src="https://img.shields.io/badge/License-GPLv3-red"> <img alt="JitPack" src="https://img.shields.io/badge/JitPack-available-brightgreen">

---

## Features

- **Telegram bot** - full bidirectional bridge: server monitoring, player management, map screenshots, in-game chat bridge
- **Web dashboard** - React SPA with an admin log panel: infinite scroll, clickable players, maps, context menu, search and filtering
- **Custom events** - loadable Java modules that modify gameplay rules per map
- **HTTP API** - REST API on localhost with SSE for external tools and automation
- **Anti-grief** - thorium reactor protection near cores, spawn zone blocking
- **Lightweight** - under 500 MB total with Mindustry including the dashboard, can run on a 1 GB RAM VDS
- **30+ commands** with flexible helper permissions
- **Achievements** - per-map achievement system with tier progression
- **Player rating** - based on playtime, kicks given and received
- **Voting system** - weighted kick and skip-map votes
- **SQLite persistence** - player stats, achievements, kick records

The plugin consists of a core Mindustry mod, an annotation processor for compile-time code generation, a React web client, and a Go reverse proxy for TLS/Let's Encrypt and session management.

## Web Dashboard

The included web interface is a React SPA for admin log browsing:

- Infinite scroll event log with search and filtering
- Clickable player names, map names, and block names
- Context menu integration for quick searches and filtering
- Filter by event type and time range
- Works over the Go proxy with TLS and session auth

No external tools needed - the SPA is served by the built-in proxy alongside the API.

## Tech Stack

- **Languages** - Java 17, Go, TypeScript, JavaScript
- **Frontend** - React 19, Vite, Zustand, TypeScript
- **Backend** - Mindustry plugin API, `com.sun.net.httpserver` (lightweight, no framework overhead), custom annotation processor, SQLite
- **Infrastructure** - Gradle, JitPack, Git submodules
- **Protocols** - HTTP REST, SSE streaming, Telegram Bot API

---

## Quick Start

### Download (server owners)

Download the `.jar` from JitPack and place it in your server's `config/mods/` directory. No build required.

```
https://jitpack.io/com/github/Agzam4/Mindustry-plugin/{version}/Mindustry-plugin-{version}.jar
```

Replace `{version}` with a tag name or commit hash. See all versions on [JitPack](https://jitpack.io/#Agzam4/Mindustry-plugin).

### Build from source (developers)

```bash
git clone --recurse-submodules https://github.com/Agzam4/Mindustry-plugin.git
cd Mindustry-plugin
./gradlew build
```

Output: `build/libs/config/mods/Mindustry-plugin.jar`

### First steps

Once the server is running:

- **Grant yourself admin** - `/admin add <your-name>` gives full access to all commands
- **Set up Telegram bot** - `/bot add <username> <token>`
- **Configure API** - `/config apiPort set <port>` (default: server port + 1), `/config authUrl set <url>` (for web dashboard auth callbacks)
- **Set a Discord link** - `/setdiscord <link>` (shown to kicked players for appeals)

---

## Helper System

Admins can grant limited command access to trusted players without giving full admin:

```
/helper add PlayerName
/helper PlayerName +runwave +nextmap
/helper PlayerName -nextmap
```

Players with helper role can only use commands explicitly granted to them.

---

## Building (subprojects)

Web client and proxy have their own builds - see `web/` and `proxy/` respectively.

---

## Commands

### Admin (full access)

**Management:**
- `/admin <add/remove> <name>` - manage admins
- `/helper <add/remove> <name>` / `/helper <name> +/-<command>` - grant limited command access
- `/bot <add/remove/list/start/stop/t/p>` - Telegram bot management

**Game control:**
- `/m` - player control UI menu (change teams, heal, destroy, toggle invincible/fast)
- `/sandbox [on/off] [team]` - infinite resources for all or a specific team
- `/unit <type> [t/c]` - spawn units (`t` for executor, `c` for auto-despawn)
- `/brush <mode> [block]` - paint on the map (block/floor/overlay/none)

**Events:**
- `/etrigger <trigger> [args...]` - fire a custom event trigger

### Helper (can be delegated individually)

- `/nick [name]` - set your own nickname on this server
- `/setnick <player> <name>` - set nickname for another player
- `/custom <join/leave> [message]` - set your own join/leave message (`@name` is replaced)
- `/setcustom <player> <join/leave> [message]` - set join/leave message for another player

### Server (in-game + console)

**Maps:**
- `/nextmap <name>` - set the next map
- `/reloadmaps` - reload all custom maps
- `/event <id> <on/off/faston>` - toggle custom events

**Game:**
- `/runwave` - force next wave (use `force-runwave` to skip enemy check)
- `/fillitems [item] [count]` - add/remove items from team core
- `/team <player> <team>` - change player's team
- `/sandbox [on/off] [team]` - toggle sandbox mode

**Players:**
- `/info <player>` - player info
- `/stat <player>` - detailed player statistics
- `/extrastar <add/remove/list> <name>` - grant a magenta rating star
- `/as <player> <command...>` - execute a command as another player

**Configuration:**
- `/config <name> <set/add> [value...]` - change server config
- `/setdiscord <link>` - set Discord invite (shown on kick)
- `/link <url> [player]` - send a link to a player or everyone
- `/doorscup [count]` - max doors on a map

**Utilities:**
- `/bans` - list all banned IPs and IDs
- `/unban <ip/ID/all>` - unban
- `/js <script...>` - execute JavaScript
- `/chatfilter <on/off>` - a light-hearted command that replaces "noob" with "pro" in chat
- `/restart` - schedule restart after game over
- `/threads` - show active thread information

### Players

- `/help [page]` - list commands
- `/vote <y/n>` - vote in an active session
- `/votekick <player> [reason]` - start a kick vote
- `/skipmap` / `/smvote <y/n>` - skip current map
- `/maps` / `/mapinfo` - map info
- `/discord` - get Discord invite
- `/auth` - authenticate for the web dashboard
- `/a <message...>` - message admins
- `/pluginfo` - plugin version info

### Telegram Bot

**Notification tags** - tag a chat to receive messages of a certain kind:
`event`, `achievement`, `round`, `votekick`, `player-command`, `admin-command`, `player-connection`, `chat-message`, `server-info`

Prefix with `!` to include sensitive data (UUIDs, IPs).

**Commands:**
- `/help` - list bot commands
- `/players` - list online players
- `/player <uuid>` - detailed player info with playtime and achievements
- `/this` - current user/chat info and permissions
- `/map` / `/mapm` - map screenshots
- `/at <player>` - screenshot around a player
- `/say <message>` - send to in-game chat
- `/kick <player> [reason]` - start a kick vote

---

## Configuration

Set via `/config` in-game:

| Setting | Default | Description |
|---------|---------|-------------|
| `apiPort` | server port + 1 | HTTP API port |
| `authUrl` | - | Web auth callback URL |
| `discord-link` | - | Discord invite shown on kick |
| `doors-cap` | unlimited | Max doors on a map |
| `votekickRequiredMapPlaytime` | 5 | Min minutes on map to vote |
| `votekickRequiredPlaytime` | 15 | Min total playtime to vote |

---

## Event Modules

Custom game events are loaded from `config/events/*.jar` (or `.zip`). Each event is an independent Java module built against the plugin's API.

### Project structure

```
event-example.jar
├── event.json               # meta file
├── assets/
│   └── bundles/
│       └── ExampleEvent.properties   # i18n strings
└── agzam4/events/
    └── ExampleEvent.class
```

### Meta file (`event.json`)

```json
{
  "events": ["agzam4.events.ExampleEvent"]
}
```

### Event class

Create a class extending `ServerEvent`:

```java
package agzam4.events;

public class ExampleEvent extends ServerEvent {

    public ExampleEvent() {
        super("ExampleEvent");
    }

    @Override
    public void init() {
        // called once when the event is loaded
    }

    @Override
    public void update() {
        // called every game tick while the event is active
    }
}
```

### Lifecycle

| Phase | When | What to do |
|-------|------|------------|
| `init()` | Event loaded from disk | Register listeners, set up state |
| `prepare()` | World loaded, game not started | Place blocks, modify terrain, read map messages |
| `run()` | Game starts | Start timers, announce rules |
| `update()` | Every tick | Game logic loop |
| `stop()` | Event deactivated | Cleanup |

### Game event hooks

Override these to react to player actions (only called when the event is active):

- `playerJoin(PlayerJoin)` - player connects
- `blockBuildEnd(BlockBuildEndEvent)` - block placed or removed
- `blockDestroy(BlockDestroyEvent)` - block destroyed
- `unitDestroy(UnitDestroyEvent)` - unit destroyed
- `deposit(DepositEvent)` - item deposited into a container
- `withdraw(WithdrawEvent)` - item withdrawn
- `tap(TapEvent)` - player taps a block
- `config(ConfigEvent)` - block configured
- `trigger(Player, String...)` - custom trigger called via `/etrigger`

### Messaging with EventNet

`event.net` provides helpers for sending localized messages:

```java
event.net.announce("info");           // yellow center text to all players
event.net.message("info");            // chat message to all players
event.net.message(player, "info");    // private message to one player
```

### Bundle files

Messages are stored in `assets/bundles/ExampleEvent.properties` - one `.properties` file per event:

```properties
# assets/bundles/ExampleEvent.properties
name=Example Event
info=Custom rules are now active!
announce=[scarlet]Special event started!
```

Access them in code via `bungle("key")` or `bungle("key", arg1, arg2)` for format strings.

### In-game management

- **Toggle** - `event ExampleEvent on/off/faston`
- **Custom trigger** - `etrigger my_event [args...]`
- **Active events** are persisted in `config/active-events.txt` and restored on server restart

### Per-map events

Add `#EventName` to a map description to auto-activate an event when that map loads.

### Build dependency

To compile an event module, depend on the plugin jar:

```groovy
compileOnly files("libs/Mindustry-plugin.jar")
```

## Subprojects

- **`processor/`** - annotation processor (JavaPoet + AutoService) that generates API router registration and TypeScript types at compile time
- **`web/`** - React + TypeScript SPA with a log admin panel featuring real-time searchable event log with SSE streaming, filtering by type and time range
- **`proxy/`** - Go reverse proxy: routes `/api/*` to the Java API, serves the SPA, handles TLS (including Let's Encrypt), and session management

---

## License

GNU General Public License v3.0. See [LICENSE](LICENSE).
