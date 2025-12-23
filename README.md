
# Commands

Plugin has custom commands system

Admins can add "helpers" and grant them permission only for certain commands [more](#helper)

## Admin commands

<details><summary>admin</summary>

`/admin <add/remove> <name>` - add/remove admins
</details>

<details><summary>m</summary>
Players control ui menu (change teams)

Allow to control player team if has permission `team`
Allow to heal/destroy/clear player's unit
Allow to toggle `invincible` and `fast` effects
</details>

<details><summary>sandbox</summary>

`/sandbox [on/off] [team]` - enables sandbox/infiniteResources for all/team (and sync executor)
</details>

<details><summary>unit</summary>

`/unit [type] [t/c]` - spawn unit

- `t` - spawn and sets unit for executor
- `c` - spawn auto-despawned unit (like from core) and sets unit for executor
</details>

<details><summary>brush</summary>

`/brush [none/block/floor/overlay/info] [block/none]` - allow to draw on map

- `/brush none` - clear all masks of brush
- `/brush [b/block] <name/emoji>` - set block's mask of brush (`none` for clear)
- `/brush [o/overlay] <name/emoji>` - set overlay's mask of brush (`none` for clear)
- `/brush [f/floor] <name/emoji>` - set floor's mask of brush (`none` for clear)

> Shortcuts of block's names:
> - `core1` - Core Shard
> - `core2` - Core Foundation
> - `core3` - Core Nucleus
> - `core4` - Core Bastion
> - `core5` - Core Citadel
> - `core6` - Core Acropolis
> - `power+` - Power source
> - `power-` - Power void
> - `item+` - Item source
> - `item-` - Item void
> - `liq+` - Liquid source
> - `liq-` - Liquid void
> - `s` - Shield projector
> - `ls` - Large shield projector
</details>

<details><summary>etrigger</summary>

`/etrigger <trigger> [args...]` - calls special trigger in event
</details>

<details><summary>bot</summary>

`/bot [add/remove/list/start/stop/t/p] [id/name] [token...]` - telegram bot control

`/bot t [tags...]` - add notify tags for group/user

Tags mark chats for receiving messages of a certain kind


> By defalt important information like uuid, ip and etc not visible, use `!` before tag to receive it (example `!votekick`) 


Tags:
- `event` - events messages
- `achievement` - achievement messages
- `round` - round messages (game over and etc)
- `votekick` - messages about bans/kicks
- `player-command` - when playr execute command
- `admin-command` - when admin/helper execute command
- `player-connection` - when player join/leave
- `chat-message` - messages from chat
- `server-info` - info about server (restart and etc)

`/bot p [tags...]` - system like "helpers" but with bot

> Permission can be for groups and users, use `$` before permission-name to disable using command in dialogs

Command can be executed if:
- In dialogs:
  - `user` has permission (and permission not has `$` before)
- In groups:
  - `user` and `group` has permission
</details>

<details><summary>helper</summary>
<a name="helper"></a>

Admins can add "helpers" and grant them permission only for certain commands

> Example:
> ```
> /helper add Agzam
> /helper Agzam +runwave +nextmap +reloadmaps
> ```
> Player with name "Agzam" will be able to use commands `runwave`, `nextmap`, `reloadmaps`
> ```
> /helper add Agzam
> /helper Agzam -nextmap -reloadmaps
> ```
> Player with name "Agzam" will be able to use commands `runwave`
</details>

<details><summary>nick</summary>
`/nick [name..]` - sets custom nick for this server
</details>

<details><summary>custom</summary>

`/custom <join/leave> [message...]` - sets custom join/leave message for this server (`@name` - will be replaced on player's name)
</details>


## Server + admins + bot commands

<details><summary>nextmap</summary>

`/nextmap <name...>` - allow to set next map
</details>

<details><summary>runwave</summary>

Runs next wave

Disabled if not all enemies destryoed (`force-runwave` to ignore this rule)
</details>

<details><summary>fillitems</summary>

`/fillitems [item] [count]` - add/remove specific items player's team core with 
</details>
<details><summary>chatfilter</summary>

`/chatfilter <on/off>` - joke (or not) command

Enables replacing the words "noob" with "pro" 
</details>
<details><summary>event</summary>

`/event [id] [on/off/faston]` - allows to enable/disable certain events

`on` - Event will be enabled on the next map

`off` - Disable event

`faston` - immediately enables event and sync players
</details>

<details><summary>team</summary>
`/team [player] [team]` - change team for player
</details>

<details><summary>config</summary>

`/config [name] [set/add] [value...]` - change server config

</details>
<details><summary>bans</summary>

List all banned IPs and IDs

</details>
<details><summary>unban</summary>

`/unban <ip/ID/all>` - Completely unban a person by IP or ID

</details>
<details><summary>reloadmaps</summary>

Reloads all custom maps
</details>
<details><summary>js</summary>

`/js <script...>` - execute js

</details>
<details><summary>link</summary>

`/link <url> [player]` - send link for player/all

</details>
<details><summary>setdiscord</summary>

`/setdiscord <link>` - set link for discord

</details>
<details><summary>doorscup</summary>

`/doorscup [count]` - set max amount of door on map
</details>


# Building

> [!NOTE]
> Gradle Version: 8.14.1
>
> Java Version: 17

Use `build` gradle task for build

The builded file will be in `build/libs/config/mods/[project-name].jar`

## Build structure:

```bash
build/libs
│   server-release.jar # Download mindustry server here
│
└───config
    │
    ├───events # Events modules (optional)
    │       you-event-module.jar
    │       other-you-event-module.jar
    ├───mods
    │       Mindustry-plugin.jar # Builded plugin
    │
    ...
```
