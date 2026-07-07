# Mindustry Plugin

Интегрированная серверная платформа с веб-интерфейсом, Telegram-ботом, модульными ивентами и гибкой системой прав с 30+ командами.

> [**English**](README.md) - english documentation

<img alt="Mindustry" src="https://img.shields.io/badge/Mindustry-v157-9cf"> <img alt="Java" src="https://img.shields.io/badge/Java-17-blue"> <img alt="Gradle" src="https://img.shields.io/badge/Gradle-8.14-green"> <img alt="License" src="https://img.shields.io/badge/License-GPLv3-red"> <img alt="JitPack" src="https://img.shields.io/badge/JitPack-available-brightgreen">

---

## Возможности

- **Telegram бот** - двунаправленный мост: мониторинг сервера, управление игроками, скриншоты карт, чат-бридж
- **Веб-дашборд** - React SPA с панелью логов: бесконечная лента, кликабельные игроки и карты, контекстное меню, поиск и фильтрация
- **Кастомные ивенты** - подключаемые Java-модули, меняющие правила игры на карте
- **HTTP API** - REST API на localhost с SSE для внешних инструментов
- **Антигриферство** - защита ториевых реакторов около ядер, блокировка спавна
- **Легковесность** - менее 500 MB суммарно с Mindustry, включая дашборд; работает на VDS с 1 GB RAM
- **30+ команд** с гибкими правами для помощников
- **Рейтинг игроков** - на основе наигранного времени, киков
- **Голосования** - взвешенные голосования за кик и смену карты
- **SQLite** - статистика игроков, достижения, история киков

Плагин состоит из ядра Mindustry мода, аннотационного процессора для кодогенерации, React веб-клиента и Go обратного прокси для TLS/Let's Encrypt и управления сессиями.

## Веб-дашборд

<img width="800" height="450" alt="output" src="https://github.com/user-attachments/assets/e894434c-9097-4357-b93b-d8e64ad95255" />

Встроенный веб-интерфейс на React SPA для просмотра логов админ-панели:

- Бесконечная лента событий с поиском и фильтрацией
- Кликабельные имена игроков, названия карт и блоков
- Контекстное меню для быстрого поиска и фильтрации
- Фильтрация по типу события и временному диапазону
- Работает через Go-прокси с TLS и сессионной аутентификацией

Никаких внешних инструментов не требуется - SPA раздаётся встроенным прокси вместе с API.

## Технический стек

- **Языки** - Java 17, Go, TypeScript, JavaScript
- **Фронтенд** - React 19, Vite, Zustand, TypeScript
- **Бэкенд** - Mindustry plugin API, `com.sun.net.httpserver` (лёгкий, без фреймворков), собственный аннотационный процессор, SQLite
- **Инфраструктура** - Gradle, JitPack, Git submodules
- **Протоколы** - HTTP REST, SSE, Telegram Bot API

---

## Быстрый старт

### Скачать (владельцам серверов)

Скачайте `.jar` с JitPack и поместите в `config/mods/` вашего сервера. Сборка не требуется.

```
https://jitpack.io/com/github/Agzam4/Mindustry-plugin/{version}/Mindustry-plugin-{version}.jar
```

Замените `{version}` на тег или хеш коммита. Все версии на [JitPack](https://jitpack.io/#Agzam4/Mindustry-plugin).

### Сборка из исходников (разработчикам)

```bash
git clone --recurse-submodules https://github.com/Agzam4/Mindustry-plugin.git
cd Mindustry-plugin
./gradlew build
```

Результат: `build/libs/config/mods/Mindustry-plugin.jar`

### Первые шаги

Когда сервер запущен:

- **Выдайте себе админа** - `/admin add <your-name>` даёт полный доступ ко всем командам
- **Настройте Telegram бота** - `/bot add <username> <token>`
- **Настройте API** - `/config apiPort set <port>` (по умолчанию: порт сервера + 1), `/config authUrl set <url>` (для колбэков аутентификации веб-дашборда)
- **Укажите Discord-ссылку** - `/setdiscord <link>` (показывается кикнутым игрокам)

---

## Система помощников

Админы могут давать ограниченный доступ к командам доверенным игрокам без выдачи полного админа:

```
/helper add PlayerName
/helper PlayerName +runwave +nextmap
/helper PlayerName -nextmap
```

Игроки с ролью helper могут использовать только команды, явно им разрешённые.

---

## Сборка (подпроекты)

Веб-клиент и прокси собираются отдельно - см. `web/` и `proxy/` соответственно.

---

## Команды

### Admin (полный доступ)

**Управление:**
- `/admin <add/remove> <name>` - управление админами
- `/helper <add/remove> <name>` / `/helper <name> +/-<command>` - управление доступом помощников
- `/bot <add/remove/list/start/stop/t/p>` - управление Telegram ботом

**Управление игрой:**
- `/m` - меню управления игроками (смена команды, лечение, уничтожение, неуязвимость/ускорение)
- `/sandbox [on/off] [team]` - бесконечные ресурсы для всех или указанной команды
- `/unit <type> [t/c]` - спавн юнитов (`t` для исполнителя, `c` для автодеспавна)
- `/brush <mode> [block]` - рисование на карте (block/floor/overlay/none)

**Ивенты:**
- `/etrigger <trigger> [args...]` - запустить кастомный триггер ивента

### Helper (можно выдавать по одной)

- `/nick [name]` - установить себе ник на этом сервере
- `/setnick <player> <name>` - установить ник другому игроку
- `/custom <join/leave> [message]` - установить своё сообщение при входе/выходе (`@name` заменяется на имя)
- `/setcustom <player> <join/leave> [message]` - установить сообщение входа/выхода другому игроку

### Server (в игре + консоль)

**Карты:**
- `/nextmap <name>` - установить следующую карту
- `/reloadmaps` - перезагрузить все кастомные карты
- `/event <id> <on/off/faston>` - включить/выключить кастомные ивенты

**Игра:**
- `/runwave` - принудительная волна (`force-runwave` пропускает проверку врагов)
- `/fillitems [item] [count]` - добавить/убрать предметы из ядра команды
- `/team <player> <team>` - сменить команду игрока
- `/sandbox [on/off] [team]` - включить/выключить песочницу

**Игроки:**
- `/info <player>` - информация об игроке
- `/stat <player>` - детальная статистика игрока
- `/extrastar <add/remove/list> <name>` - выдать магнитную звезду рейтинга
- `/as <player> <command...>` - выполнить команду от имени другого игрока

**Настройки:**
- `/config <name> <set/add> [value...]` - изменить конфигурацию сервера
- `/setdiscord <link>` - установить Discord-ссылку (показывается при кике)
- `/link <url> [player]` - отправить ссылку игроку или всем
- `/doorscup [count]` - максимум дверей на карте

**Утилиты:**
- `/bans` - список всех забаненных IP и ID
- `/unban <ip/ID/all>` - разбан
- `/js <script...>` - выполнить JavaScript
- `/chatfilter <on/off>` - безобидная команда, заменяющая "noob" на "pro" в чате
- `/restart` - запланировать перезапуск после окончания игры
- `/threads` - информация о активных потоках

### Players

- `/help [page]` - список команд
- `/vote <y/n>` - голосовать в активной сессии
- `/votekick <player> [reason]` - начать голосование за кик
- `/skipmap` / `/smvote <y/n>` - пропустить текущую карту
- `/maps` / `/mapinfo` - информация о картах
- `/discord` - получить Discord-ссылку
- `/auth` - аутентификация для веб-дашборда
- `/a <message...>` - написать админам
- `/pluginfo` - версия плагина

### Telegram Bot

**Теги уведомлений** - привяжите тег к чату, чтобы получать определённые сообщения:
`event`, `achievement`, `round`, `votekick`, `player-command`, `admin-command`, `player-connection`, `chat-message`, `server-info`

Префикс `!` включает чувствительные данные (UUID, IP).

**Команды:**
- `/help` - список команд бота
- `/players` - список онлайн игроков
- `/player <uuid>` - детальная информация об игроке с наигранным временем и достижениями
- `/this` - информация о текущем пользователе/чате и правах
- `/map` / `/mapm` - скриншоты карты
- `/at <player>` - скриншот вокруг игрока
- `/say <message>` - отправить сообщение в игровой чат
- `/kick <player> [reason]` - начать голосование за кик

---

## Конфигурация

Настраивается через `/config` в игре:

| Параметр | По умолчанию | Описание |
|---------|-------------|-----------|
| `apiPort` | порт сервера + 1 | Порт HTTP API |
| `authUrl` | - | URL колбэка веб-аутентификации |
| `discord-link` | - | Discord-ссылка при кике |
| `doors-cap` | безлимит | Максимум дверей на карте |
| `votekickRequiredMapPlaytime` | 5 | Мин. минут на карте для голосования |
| `votekickRequiredPlaytime` | 15 | Мин. всего наигранных минут для голосования |

---

## Модули ивентов

Кастомные игровые ивенты загружаются из `config/events/*.jar` (или `.zip`). Каждый ивент - независимый Java-модуль, собранный против API плагина.

### Структура проекта

```
event-example.jar
├── event.json               # мета-файл
├── assets/
│   └── bundles/
│       └── ExampleEvent.properties   # i18n
└── agzam4/events/
    └── ExampleEvent.class
```

### Мета-файл (`event.json`)

```json
{
  "events": ["agzam4.events.ExampleEvent"]
}
```

### Класс ивента

Создайте класс, наследуемый от `ServerEvent`:

```java
package agzam4.events;

public class ExampleEvent extends ServerEvent {

    public ExampleEvent() {
        super("ExampleEvent");
    }

    @Override
    public void init() {
        // вызывается один раз при загрузке ивента
    }

    @Override
    public void update() {
        // вызывается каждый игровой тик, пока ивент активен
    }
}
```

### Жизненный цикл

| Фаза | Когда | Что делать |
|-------|------|------------|
| `init()` | Ивент загружен с диска | Зарегистрировать слушатели, инициализировать состояние |
| `prepare()` | Мир загружен, игра не началась | Разместить блоки, изменить ландшафт, прочитать сообщения карты |
| `run()` | Игра начинается | Запустить таймеры, объявить правила |
| `update()` | Каждый тик | Игровая логика |
| `stop()` | Ивент деактивирован | Очистка |

### Хуки игровых событий

Переопределите эти методы, чтобы реагировать на действия игроков (вызываются только когда ивент активен):

- `playerJoin(PlayerJoin)` - игрок подключился
- `blockBuildEnd(BlockBuildEndEvent)` - блок размещён или удалён
- `blockDestroy(BlockDestroyEvent)` - блок разрушен
- `unitDestroy(UnitDestroyEvent)` - юнит уничтожен
- `deposit(DepositEvent)` - предмет помещён в контейнер
- `withdraw(WithdrawEvent)` - предмет взят из контейнера
- `tap(TapEvent)` - игрок нажал на блок
- `config(ConfigEvent)` - блок сконфигурирован
- `trigger(Player, String...)` - кастомный триггер через `/etrigger`

### Отправка сообщений через EventNet

`event.net` предоставляет методы для отправки локализованных сообщений:

```java
event.net.announce("info");           // жёлтый текст в центр экрана всем игрокам
event.net.message("info");            // сообщение в чат всем игрокам
event.net.message(player, "info");    // личное сообщение игроку
```

### Файлы бандлов

Сообщения хранятся в `assets/bundles/ExampleEvent.properties` - один `.properties` файл на ивент:

```properties
# assets/bundles/ExampleEvent.properties
name=Example Event
info=Custom rules are now active!
announce=[scarlet]Special event started!
```

Доступ в коде через `bungle("key")` или `bungle("key", arg1, arg2)` для форматированных строк.

### Управление в игре

- **Вкл/Выкл** - `event ExampleEvent on/off/faston`
- **Кастомный триггер** - `etrigger my_event [args...]`
- **Активные ивенты** сохраняются в `config/active-events.txt` и восстанавливаются при перезапуске сервера

### Ивенты на картах

Добавьте `#EventName` в описание карты, чтобы ивент автоматически активировался при загрузке этой карты.

### Зависимость для сборки

Для компиляции модуля ивента укажите зависимость от jar плагина:

```groovy
compileOnly files("libs/Mindustry-plugin.jar")
```

## Подпроекты

- **`processor/`** - аннотационный процессор (JavaPoet + AutoService), генерирующий регистрацию API-роутов и TypeScript-типы на этапе компиляции
- **`web/`** - React + TypeScript SPA с панелью логов: поиск событий, фильтрация по типу и времени
- **`proxy/`** - Go reverse proxy: маршрутизация `/api/*` на Java API, раздача SPA, TLS (включая Let's Encrypt), управление сессиями

---

## Лицензия

GNU General Public License v3.0. См. [LICENSE](LICENSE).
