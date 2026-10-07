<p align="center">
  <img src="https://raw.githubusercontent.com/zomb1ch/dopes-seamless-loading-screen/main/src/main/resources/assets/dopes_seamless_loading_screen/icon.png" width="128" alt="dope's Seamless Loading Screen">
</p>

# dope's Seamless Loading Screen

Takes a screenshot of the game when you leave a world or a server and shows it again on the loading
screen when you come back. Instead of a generic *"Loading terrain..."* you see the place you are
returning to — blurred at first, then sharpening while the world fades in.

**Client-side only.** Nothing has to be installed on the server, and it works on any server that
does not have the mod.

**English** · [Русский](#русский)

> **Links:** [GitHub](https://github.com/zomb1ch/dopes-seamless-loading-screen) ·
> [Issues & suggestions](https://github.com/zomb1ch/dopes-seamless-loading-screen/issues) ·
> [Source code](https://github.com/zomb1ch/dopes-seamless-loading-screen)

---

## Features

- **Screenshot on exit, picture on rejoin.** The screenshot is captured the moment you leave a
  singleplayer world, disconnect from a server or close the game, and it is displayed again while
  the world loads.
- **Blur → sharp animation.** The screenshot appears fully blurred and melts into the sharp image.
  The blur is built from a pyramid of progressively downscaled levels, so the blur radius changes
  smoothly instead of snapping between two fixed images.
- **Slideshow fallback.** If a world has no screenshot yet, the mod shows a slideshow instead:
  either your own images from `screenshots/seamless/slideshow` or the ten images bundled with the
  mod. Slides cross-fade into each other.
- **Chunk counter.** Optional `loaded / total` counter at the top of the screen.
- **Wait for chunks.** The loading screen can stay open until the chunk count stops growing, so you
  never fade into an empty world. Void spawns and registration lobbies are detected and released by
  a grace period instead of hanging.
- **Min / max show time.** Never flashes by too quickly, never hangs forever.
- **Server screenshots can be disabled** (off by default) for servers where you always spawn in a
  lobby and screenshots make no sense. The slideshow and chunk counter keep working.
- **Fully configurable** through Mod Menu, with an English and a Russian interface.

## Requirements

| | |
|---|---|
| Minecraft | 1.21.11 |
| Mod loader | Fabric Loader 0.16.0+ |
| Required | [Fabric API](https://modrinth.com/mod/fabric-api) |
| Required | [YetAnotherConfigLib (YACL)](https://modrinth.com/mod/yacl) |
| Optional | [Mod Menu](https://modrinth.com/mod/modmenu) — needed to open the settings screen |

## Installation

1. Install Fabric Loader for Minecraft 1.21.11.
2. Put the mod jar into your `mods` folder.
3. Install Fabric API and YetAnotherConfigLib as well.
4. Mod Menu is optional but recommended — the settings screen is opened through it.

## Where the files are stored

Screenshots are saved inside your game directory:

```
screenshots/seamless/singleplayer/<world folder name>.png
screenshots/seamless/servers/<server address>.png
screenshots/seamless/slideshow/<any .png you drop in here>
```

Only the newest screenshot of each world is kept. The slideshow folder accepts any `.png` files;
when it is empty, the ten bundled images are used.

The config file is `config/dopes_seamless_loading_screen.json`.

## Settings

| Setting | Default | What it does |
|---|---|---|
| Mod Enabled | on | Turns the whole mod on or off. |
| Screenshots on Servers | off | Take and show screenshots while playing on servers. When off, servers fall back to the slideshow and the chunk counter still works. A fresh screenshot is always captured when leaving a server, so the exit transition screen has something to show. |
| Transition Screens | on | Show the screenshot (or slideshow) over the transition when entering a world or server and when leaving them. The screen fades in, starts the load and stays on top until the loading screen appears (hiding the screens in between), then fades out. Its blur and dim match the loading screen, but are static. |
| Transition Fade Duration | 800 ms | How long the transition screen takes to appear and to disappear. The same for entering and leaving. 0 = no animation. |
| Slideshow (no screenshot) | on | Show the slideshow when a world has no screenshot yet. |
| Wait for All Chunks | on | Keep the loading screen open until the chunk count stops growing. |
| Chunk Counter | off | Show `loaded / total` chunks at the top of the screen. |
| Blur Strength | 16 | How strongly the screenshot is blurred. 0 = no blur, max 64. |
| Blur Animation Speed | 50% | How much of the fade the blur animation takes. 100% = the blur is gone exactly when the fade ends, 50% = twice as fast, 0% = no animation. The slideshow never animates its blur. |
| Background Dim | 35% | Darkening over the image. Applied at once, it does not fade in. |
| Fade Duration | 800 ms | How long the screenshot takes to fade out once the world is ready. |
| Image Resolution | 100% | Resolution the screenshot is rendered at. The image is always stretched over the whole screen, a lower value only reduces quality and memory usage. |
| Min Show Time | 2000 ms | Keep the loading screen visible for at least this long. |
| Max Show Time | 30000 ms | Never keep the loading screen open longer than this. |
| Slideshow Speed | 5000 ms | How often slideshow images change. |
| Slideshow Fade Speed | 1500 ms | Cross-fade duration between two slideshow images. |

## Building from source

Requires **JDK 21**.

```bash
git clone https://github.com/zomb1ch/dopes-seamless-loading-screen.git
cd dopes-seamless-loading-screen
./gradlew build
```

The jar is written to `build/libs/dopes-seamless-loading-screen.jar`.

## Links

- **GitHub:** <https://github.com/zomb1ch/dopes-seamless-loading-screen>
- **Issues, bug reports and suggestions:**
  <https://github.com/zomb1ch/dopes-seamless-loading-screen/issues>
- **README (full documentation):**
  <https://github.com/zomb1ch/dopes-seamless-loading-screen#readme>

## License

[MIT](https://github.com/zomb1ch/dopes-seamless-loading-screen/blob/main/LICENSE) — free to use,
modify and include in modpacks.

---

<a id="русский"></a>

## Русский

Делает скриншот игры при выходе из мира или с сервера и показывает его снова на экране загрузки
при повторном входе. Вместо безликого *«Loading terrain…»* вы видите то место, куда возвращаетесь:
сначала размытым, а затем всё более резким, пока проявляется мир.

**Только клиент.** На сервере мод ставить не нужно, и он работает на серверах, где мода нет.

> **Ссылки:** [GitHub](https://github.com/zomb1ch/dopes-seamless-loading-screen) ·
> [Ошибки и предложения](https://github.com/zomb1ch/dopes-seamless-loading-screen/issues)

### Возможности

- **Скриншот на выходе, картинка на входе.** Снимок делается в момент выхода из одиночного мира,
  отключения от сервера или закрытия игры, и снова показывается, пока мир загружается.
- **Анимация «размытие → резкость».** Скриншот появляется полностью размытым и плавно
  проявляется. Размытие строится пирамидой уровней, поэтому радиус меняется плавно, а не
  переключением между двумя фиксированными картинками.
- **Слайд-шоу вместо скриншота.** Если у мира ещё нет снимка, показывается слайд-шоу: ваши
  картинки из `screenshots/seamless/slideshow` или десять встроенных изображений. Слайды
  переходят друг в друга кроссфейдом.
- **Счётчик чанков.** Необязательный счётчик «прогружено / всего» сверху экрана.
- **Ожидание прогрузки чанков.** Экран держится, пока число чанков растёт, чтобы вы не проявлялись
  в пустом мире. Спавн в пустоте, регистрация и лобби определяются и отпускаются по таймауту —
  зависания нет.
- **Мин. и макс. время показа.** Экран не мелькнёт слишком быстро и не залипнет навсегда.
- **Скриншоты для серверов можно отключить** (по умолчанию выключено) — для серверов, где вы
  всегда спавнитесь в лобби. Слайд-шоу и счётчик чанков продолжают работать.
- **Всё настраивается** через Mod Menu, есть русский и английский интерфейс.

### Требования

| | |
|---|---|
| Minecraft | 1.21.11 |
| Загрузчик модов | Fabric Loader 0.16.0+ |
| Обязательно | [Fabric API](https://modrinth.com/mod/fabric-api) |
| Обязательно | [YetAnotherConfigLib (YACL)](https://modrinth.com/mod/yacl) |
| Опционально | [Mod Menu](https://modrinth.com/mod/modmenu) — чтобы открыть экран настроек |

### Установка

1. Установите Fabric Loader для Minecraft 1.21.11.
2. Положите jar мода в папку `mods`.
3. Установите также Fabric API и YetAnotherConfigLib.
4. Mod Menu не обязателен, но рекомендуется — через него открывается экран настроек.

### Где лежат файлы

```
screenshots/seamless/singleplayer/<имя папки мира>.png
screenshots/seamless/servers/<адрес сервера>.png
screenshots/seamless/slideshow/<любые .png, которые вы сюда положите>
```

Для каждого мира хранится только последний скриншот. В папку слайд-шоу можно класть любые `.png`;
если она пуста, используются десять встроенных изображений. Файл настроек —
`config/dopes_seamless_loading_screen.json`.

### Настройки

| Настройка | По умолчанию | Что делает |
|---|---|---|
| Мод включён | вкл | Включает или отключает весь мод. |
| Скриншоты для серверов | выкл | Делать и показывать скриншоты на серверах. Если выключено, работает слайд-шоу, а счётчик чанков продолжает работать. При выходе с сервера скриншот делается всегда — чтобы вспомогательному экрану было что показать. |
| Вспомогательные экраны переходов | вкл | Показывать скриншот (или слайд-шоу) поверх перехода при входе в мир или на сервер и при выходе из них. Экран плавно появляется, запускает загрузку и держится, пока не появится экран загрузки (скрывая промежуточные экраны), после чего плавно исчезает. Размытие и затемнение такие же, как на экране загрузки, но статичные. |
| Время fade анимации переходов | 800 мс | Длительность появления и исчезновения вспомогательного экрана. Одна и та же для входа и выхода. 0 = без анимации. |
| Слайд-шоу, если нет скриншота | вкл | Показывать слайд-шоу, когда у мира ещё нет скриншота. |
| Ждать все чанки | вкл | Держать экран загрузки, пока число чанков растёт. |
| Счётчик чанков | выкл | Показывать «прогружено / всего» сверху экрана. |
| Сила размытия | 16 | Насколько сильно размыт скриншот. 0 = без размытия, максимум 64. |
| Скорость анимации размытия | 50% | Какую часть затухания занимает уменьшение размытия. 100% — размытие уходит ровно за время затухания, 50% — вдвое быстрее, 0% — без анимации. В слайд-шоу анимация размытия не работает. |
| Затемнение фона | 35% | Затемнение поверх картинки. Применяется сразу целиком, плавно не появляется. |
| Время fade анимации | 800 мс | Сколько времени скриншот исчезает, когда мир уже готов. |
| Разрешение картинки | 100% | Разрешение, в котором скриншот отрисовывается. Картинка всегда растягивается на весь экран, меньшее значение только снижает качество и расход памяти. |
| Мин. время показа | 2000 мс | Держать экран загрузки видимым минимум столько. |
| Макс. время показа | 30000 мс | Не держать экран загрузки дольше этого. |
| Скорость смены слайд-шоу | 5000 мс | Как часто сменяются картинки слайд-шоу. |
| Скорость анимации слайд-шоу | 1500 мс | Длительность кроссфейда между картинками слайд-шоу. |

### Сборка из исходников

Нужен **JDK 21**.

```bash
git clone https://github.com/zomb1ch/dopes-seamless-loading-screen.git
cd dopes-seamless-loading-screen
./gradlew build
```

Готовый jar появится в `build/libs/dopes-seamless-loading-screen.jar`.

### Ссылки

- **GitHub:** <https://github.com/zomb1ch/dopes-seamless-loading-screen>
- **Ошибки и предложения:** <https://github.com/zomb1ch/dopes-seamless-loading-screen/issues>
- **README (полная документация):**
  <https://github.com/zomb1ch/dopes-seamless-loading-screen#readme>

### Лицензия

[MIT](https://github.com/zomb1ch/dopes-seamless-loading-screen/blob/main/LICENSE) — можно свободно
использовать, изменять и включать в сборки.
