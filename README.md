# dope's Seamless Loading Screen

**English** | [Русский](#русский)

A client-side Fabric mod for **Minecraft 1.21.11** that takes a screenshot of the game when you
leave a world or a server and shows it on the loading screen when you come back — so instead of a
generic "Loading terrain..." screen you see the place you are returning to.

---

## English

### Features

- **Screenshot on exit, picture on rejoin.** A screenshot is captured the moment you leave a
  singleplayer world, disconnect from a server or close the game, and it is displayed again while
  the world is loading.
- **Blur → sharp animation.** The screenshot appears fully blurred and melts into the sharp image
  while the world fades in. The blur is built as a pyramid of progressively downscaled levels, so
  the radius changes smoothly instead of cross-fading between two fixed images.
- **Slideshow fallback.** If a world has no screenshot yet, the mod shows a slideshow instead:
  either the images you drop into `screenshots/seamless/slideshow` or the ten images bundled with
  the mod. Slides cross-fade into each other.
- **Chunk counter.** Optional `loaded / total` counter at the top of the screen.
- **Wait for chunks.** The loading screen can stay open until the chunk count stops growing, so you
  never fade into an empty world. It gives up after a short grace period, which keeps void spawns
  and registration lobbies from hanging.
- **Min / max show time.** Never flashes by too quickly, never hangs forever.
- **Server screenshots can be turned off** (off by default), for servers where you always spawn in
  a lobby and screenshots make no sense.
- **Everything is configurable** through Mod Menu (YetAnotherConfigLib), with a Russian and an
  English localization.

### Requirements

| | |
|---|---|
| Minecraft | 1.21.11 |
| Mod loader | Fabric Loader 0.16.0+ |
| Required | Fabric API |
| Required | YetAnotherConfigLib 3.8.0+ |
| Optional | Mod Menu 17.0.0+ (to open the settings screen) |

The mod is **client-side only** — it does not have to be installed on a server, and it works on
servers that do not have it.

### Installation

1. Install Fabric Loader for Minecraft 1.21.11.
2. Drop `dopes-seamless-loading-screen.jar` into your `mods` folder.
3. Also install [Fabric API](https://modrinth.com/mod/fabric-api) and
   [YetAnotherConfigLib](https://modrinth.com/mod/yacl).
4. Mod Menu is optional but recommended — the settings screen is opened through it.

### Where the files live

Screenshots are stored inside your game directory:

```
screenshots/seamless/singleplayer/<world folder name>.png
screenshots/seamless/servers/<server address>.png
screenshots/seamless/slideshow/<any .png you drop in here>
```

Only the newest screenshot of each world is kept. The slideshow folder accepts any `.png` files;
when it is empty, the ten bundled placeholder images are used.

The config file is `config/dopes_seamless_loading_screen.json`.

### Settings

| Setting | Default | What it does |
|---|---|---|
| Mod Enabled | on | Turns the whole mod on or off. |
| Screenshots on Servers | off | Take and show screenshots while playing on servers. When off, servers fall back to the slideshow and the chunk counter still works. |
| Slideshow (no screenshot) | on | Show the slideshow when a world has no screenshot yet. |
| Wait for All Chunks | on | Keep the loading screen open until the chunk count stops growing. |
| Chunk Counter | off | Show `loaded / total` chunks at the top of the screen. |
| Blur Strength | 16 | How strongly the screenshot is blurred. 0 = no blur, max 64. |
| Blur Animation Speed | 50% | How much of the fade the blur animation takes. 100% = the blur is gone exactly when the fade ends, 50% = twice as fast, 0% = no animation (the blur stays until the end of the fade). The slideshow never animates its blur. |
| Background Dim | 35% | Darkening over the image. It is applied at once, it does not fade in. |
| Fade Duration | 800 ms | How long the screenshot takes to fade out once the world is ready. |
| Image Resolution | 100% | Resolution the screenshot is rendered at. The image is always stretched over the whole screen, a lower value only reduces quality and memory usage. |
| Min Show Time | 2000 ms | Keep the loading screen visible for at least this long. |
| Max Show Time | 30000 ms | Never keep the loading screen open longer than this, even if chunks are still loading. |
| Slideshow Speed | 5000 ms | How often slideshow images change. |
| Slideshow Fade Speed | 1500 ms | Cross-fade duration between two slideshow images. |

### Building from source

Requires **JDK 21**.

```bash
./gradlew build
```

The mod jar is written to `build/libs/dopes-seamless-loading-screen.jar`.

### License

[MIT](LICENSE)

---

## Русский

Клиентский мод для **Minecraft 1.21.11** на Fabric: делает скриншот игры при выходе из мира или с
сервера и показывает его на экране загрузки при повторном входе — вместо безликого «Loading
terrain…» вы видите то место, куда возвращаетесь.

### Возможности

- **Скриншот на выходе, картинка на входе.** Снимок делается в момент выхода из одиночного мира,
  отключения от сервера или закрытия игры, и снова показывается, пока мир загружается.
- **Анимация «размытие → резкость».** Скриншот появляется полностью размытым и плавно
  проявляется, пока мир проявляется за ним. Размытие строится пирамидой уровней, поэтому радиус
  меняется плавно, а не переключением между двумя фиксированными картинками.
- **Слайд-шоу вместо скриншота.** Если у мира ещё нет снимка, показывается слайд-шоу: либо ваши
  картинки из `screenshots/seamless/slideshow`, либо десять встроенных изображений. Слайды
  переходят друг в друга кроссфейдом.
- **Счётчик чанков.** Необязательный счётчик «прогружено / всего» сверху экрана.
- **Ожидание прогрузки чанков.** Экран загрузки держится, пока число чанков растёт, чтобы вы не
  проявлялись в пустом мире. Если чанки так и не приходят (спавн в пустоте, регистрация, лобби),
  ожидание корректно заканчивается по таймауту и не зависает.
- **Мин. и макс. время показа.** Экран не мелькнёт слишком быстро и не залипнет навсегда.
- **Скриншоты для серверов можно отключить** (по умолчанию выключено) — для серверов, где вы всегда
  спавнитесь в лобби и снимки не имеют смысла.
- **Всё настраивается** через Mod Menu (YetAnotherConfigLib), есть русская и английская локализация.

### Требования

| | |
|---|---|
| Minecraft | 1.21.11 |
| Загрузчик модов | Fabric Loader 0.16.0+ |
| Обязательно | Fabric API |
| Обязательно | YetAnotherConfigLib 3.8.0+ |
| Опционально | Mod Menu 17.0.0+ (чтобы открыть экран настроек) |

Мод **полностью клиентский** — на сервере его ставить не нужно, и он работает на серверах, где
мода нет.

### Установка

1. Установите Fabric Loader для Minecraft 1.21.11.
2. Положите `dopes-seamless-loading-screen.jar` в папку `mods`.
3. Установите также [Fabric API](https://modrinth.com/mod/fabric-api) и
   [YetAnotherConfigLib](https://modrinth.com/mod/yacl).
4. Mod Menu не обязателен, но рекомендуется — через него открывается экран настроек.

### Где лежат файлы

Скриншоты хранятся в папке игры:

```
screenshots/seamless/singleplayer/<имя папки мира>.png
screenshots/seamless/servers/<адрес сервера>.png
screenshots/seamless/slideshow/<любые .png, которые вы сюда положите>
```

Для каждого мира хранится только последний скриншот. В папку слайд-шоу можно класть любые `.png`;
если она пуста, используются десять встроенных изображений.

Файл настроек — `config/dopes_seamless_loading_screen.json`.

### Настройки

| Настройка | По умолчанию | Что делает |
|---|---|---|
| Мод включён | вкл | Включает или отключает весь мод. |
| Скриншоты для серверов | выкл | Делать и показывать скриншоты при игре на серверах. Если выключено, для серверов работает слайд-шоу, а счётчик чанков продолжает работать. |
| Слайд-шоу, если нет скриншота | вкл | Показывать слайд-шоу, когда у мира ещё нет скриншота. |
| Ждать все чанки | вкл | Держать экран загрузки, пока число чанков растёт. |
| Счётчик чанков | выкл | Показывать «прогружено / всего» сверху экрана. |
| Сила размытия | 16 | Насколько сильно размыт скриншот. 0 = без размытия, максимум 64. |
| Скорость анимации размытия | 50% | Какую часть затухания занимает уменьшение размытия. 100% — размытие уходит ровно за время затухания, 50% — вдвое быстрее, 0% — без анимации (размытие держится до конца). В слайд-шоу анимация размытия не работает. |
| Затемнение фона | 35% | Затемнение поверх картинки. Применяется сразу целиком, плавно не появляется. |
| Время fade анимации | 800 мс | Сколько времени скриншот исчезает, когда мир уже готов. |
| Разрешение картинки | 100% | Разрешение, в котором скриншот отрисовывается. Картинка всегда растягивается на весь экран, меньшее значение только снижает качество и расход памяти. |
| Мин. время показа | 2000 мс | Держать экран загрузки видимым минимум столько. |
| Макс. время показа | 30000 мс | Не держать экран загрузки дольше этого, даже если чанки всё ещё грузятся. |
| Скорость смены слайд-шоу | 5000 мс | Как часто сменяются картинки слайд-шоу. |
| Скорость анимации слайд-шоу | 1500 мс | Длительность кроссфейда между картинками слайд-шоу. |

### Сборка из исходников

Нужен **JDK 21**.

```bash
./gradlew build
```

Готовый jar появится в `build/libs/dopes-seamless-loading-screen.jar`.

### Лицензия

[MIT](LICENSE)
