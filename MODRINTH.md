<p align="center">
  <img src="https://raw.githubusercontent.com/zomb1ch/dopes-seamless-loading-screen/main/src/main/resources/assets/dopes_seamless_loading_screen/icon.png" width="128" alt="dope's Seamless Loading Screen">
</p>

# dope's Seamless Loading Screen

Turns leaving a world or a server and coming back into one smooth, seamless animated sequence, and
gives the loading screen a fresh look. A screenshot of the place you are leaving is taken on the way
out; on the way back in it fades in over the Minecraft menu, covers the whole load and melts into the
world. Instead of a generic *"Loading terrain..."* you see the place you are returning to — blurred
at first, then sharpening while the world fades in.

**Client-side only.** Nothing has to be installed on the server, and it works on any server that
does not have the mod.

**English** · [Русский](#русский)

> **Links:** [Modrinth](https://modrinth.com/mod/dopes-seamless-loading-screen) ·
> [GitHub](https://github.com/zomb1ch/dopes-seamless-loading-screen) ·
> [Issues & suggestions](https://github.com/zomb1ch/dopes-seamless-loading-screen/issues) ·
> [Source code](https://github.com/zomb1ch/dopes-seamless-loading-screen)

---

## Features

- **Seamless transition into and out of your world.** Entering a world or a server from the menu, and
  going back to the menu, is a single smooth animated sequence: the screenshot fades in over the
  menu, stays on top for the whole load (hiding the vanilla screens in between) and melts into the
  world. No flashing, no jumping screens.
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
- **Refreshed loading screen.** The vanilla text, chunk map and progress bar are replaced by the
  mod's own animated text and icon plus a smooth progress bar that fills up while the world and its
  chunks load. It can be turned off to keep the vanilla look.
- **Server position check.** The server screenshot is only shown when you rejoin at (roughly) the
  same spot; if the server drops you into a lobby, the slideshow is shown instead.
- **Fully configurable** through Mod Menu, with 10 localizations: English, Russian, Ukrainian,
  German, Spanish, French, Italian, Polish, Brazilian Portuguese and Simplified Chinese.

## Supported Minecraft versions

The mod is built **separately for each Minecraft version line**, because the client APIs it hooks into
change between them. Download the file that matches your game:

| Your Minecraft | Download |
|---|---|
| **1.21.9 – 1.21.11** | `dopes-seamless-loading-screen-2.1+1.21.9-1.21.10-1.21.11.jar` |
| **26.1 – 26.1.2** | `dopes-seamless-loading-screen-2.1+26.1-26.1.2.jar` |
| **26.2** | `dopes-seamless-loading-screen-2.1+26.2.jar` |
| **26.3** | `dopes-seamless-loading-screen-2.1+26.3.jar` |
| 1.21 – 1.21.8 | **Not supported.** The mod needs `LevelLoadTracker` (added in 1.21.9), `Identifier`, `ARGB` and `RenderPipelines`. **1.21.9 is the minimum.** |

Why several files: 1.21.x is obfuscated, so the mod is remapped through Fabric's intermediary names,
while 26.x ships unobfuscated and needs a build without remapping; and between 26.2 and 26.3
`RenderPipeline` moved packages, which changes what the mod links against at runtime.

## Requirements

| | |
|---|---|
| Minecraft | 1.21.9 – 26.3 (see the table above) |
| Mod loader | Fabric Loader 0.16.0+ |
| Required | [YetAnotherConfigLib (YACL)](https://modrinth.com/mod/yacl) |
| Optional | [Mod Menu](https://modrinth.com/mod/modmenu) — needed to open the settings screen |

## Installation

1. Install Fabric Loader for your Minecraft version.
2. Put the jar that matches that version (see the table above) into your `mods` folder. Only one of them — these are alternative builds, not add-ons.
3. Install YetAnotherConfigLib as well — Fabric API is **not** needed.
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
| Server Position Check | on | Remember where you were when you left a server, and only show the screenshot again when you rejoin at (roughly) the same spot. If you end up somewhere else (a lobby, another world, ...) the slideshow is shown instead, so the screenshot is never misleading. |
| Transition Screens | on | Show the screenshot (or slideshow) over the transition when entering a world or server and when leaving them. The screen fades in, starts the load and stays on top until the loading screen appears (hiding the screens in between), then fades out. Its blur and dim match the loading screen, but are static. |
| Transition Fade Duration | 800 ms | How long the transition screen takes to appear and to disappear. The same for entering and leaving. 0 = no animation. |
| Slideshow (no screenshot) | on | Show the slideshow when a world has no screenshot yet. |
| Wait for All Chunks | on | Keep the loading screen open until the chunk count stops growing. |
| Custom Loading Screen | on | Replace the vanilla loading screen text, chunk map and progress bar with the mod's own animated text, icon and progress bar. Off = the vanilla loading screen is shown. |
| Chunk Counter | off | Show `loaded / total` chunks at the top of the screen. |
| Blur Strength | 16 | How strongly the screenshot is blurred. 0 = no blur, max 64. |
| Blur Animation Speed | 50% | How much of the fade the blur animation takes. 100% = the blur is gone exactly when the fade ends, 50% = twice as fast, 0% = no animation. The slideshow never animates its blur. |
| Background Dim | 35% | Darkening over the image. Applied at once, it does not fade in. |
| Fade Duration | 800 ms | How long the screenshot takes to fade out once the world is ready. |
| Image Resolution | 100% | Resolution the screenshot is sampled at. The picture is always scaled to the screen, so a lower value only makes it softer. |
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

The jar is written to `build/libs/dopes-seamless-loading-screen-<mod version>+<Minecraft version>.jar`.

## Links

- **Modrinth page:** <https://modrinth.com/mod/dopes-seamless-loading-screen>
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

Превращает выход из мира или с сервера и возвращение обратно в одну плавную бесшовную анимацию и
обновляет вид экрана загрузки. Снимок того места, откуда вы уходите, делается на выходе; при
возвращении он плавно проявляется поверх меню Minecraft, держится всю загрузку и растворяется в мире.
Вместо безликого *«Loading terrain…»* вы видите то место, куда возвращаетесь: сначала размытым, а
затем всё более резким, пока проявляется мир.

**Только клиент.** На сервере мод ставить не нужно, и он работает на серверах, где мода нет.

> **Ссылки:** [Modrinth](https://modrinth.com/mod/dopes-seamless-loading-screen) ·
> [GitHub](https://github.com/zomb1ch/dopes-seamless-loading-screen) ·
> [Ошибки и предложения](https://github.com/zomb1ch/dopes-seamless-loading-screen/issues)

### Возможности

- **Бесшовный переход в мир и обратно.** Вход в мир или на сервер из меню и возвращение в меню — одна
  плавная анимация: скриншот проявляется поверх меню, держится всю загрузку (скрывая промежуточные
  ванильные экраны) и растворяется в мире. Ничего не мелькает и не перескакивает.
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
- **Обновлённый вид экрана загрузки.** Ванильные текст, карта чанков и шкала заменены собственными
  анимированными текстом и иконкой и плавной шкалой, которая заполняется во время загрузки мира и
  чанков. Настройку можно выключить и оставить ванильный вид.
- **Проверка места на сервере.** Скриншот показывается только если вы вернулись примерно на то же место;
  если сервер забросил вас в лобби — показывается слайд-шоу, чтобы скриншот не вводил в заблуждение.
- **Всё настраивается** через Mod Menu, есть 10 локализаций: английская, русская, украинская, немецкая,
  испанская, французская, итальянская, польская, бразильская португальская и упрощённая китайская.

### Поддерживаемые версии Minecraft

Мод собирается **отдельно под каждую линию версий Minecraft**, потому что клиентские API, за которые
он цепляется, между ними меняются. Скачай файл, который подходит твоей игре:

| Твой Minecraft | Файл |
|---|---|
| **1.21.9 – 1.21.11** | `dopes-seamless-loading-screen-2.1+1.21.9-1.21.10-1.21.11.jar` |
| **26.1 – 26.1.2** | `dopes-seamless-loading-screen-2.1+26.1-26.1.2.jar` |
| **26.2** | `dopes-seamless-loading-screen-2.1+26.2.jar` |
| **26.3** | `dopes-seamless-loading-screen-2.1+26.3.jar` |
| 1.21 – 1.21.8 | **Не поддерживается.** Моду нужны `LevelLoadTracker` (появился в 1.21.9), `Identifier`, `ARGB` и `RenderPipelines`. **Минимум — 1.21.9.** |

Почему файлов несколько: 1.21.x обфусцирован, поэтому мод ремапится через intermediary-имена Fabric, а
26.x поставляется необфусцированным и требует сборки без remapping; кроме того между 26.2 и 26.3
переехал `RenderPipeline`, из-за чего меняется то, к чему мод привязывается в рантайме.

### Требования

| | |
|---|---|
| Minecraft | 1.21.9 – 26.3 (см. таблицу выше) |
| Загрузчик модов | Fabric Loader 0.16.0+ |
| Обязательно | [YetAnotherConfigLib (YACL)](https://modrinth.com/mod/yacl) |
| Опционально | [Mod Menu](https://modrinth.com/mod/modmenu) — чтобы открыть экран настроек |

### Установка

1. Установите Fabric Loader для своей версии Minecraft.
2. Положите в папку `mods` тот jar, который подходит этой версии (см. таблицу выше). Только один — это альтернативные сборки, а не дополнения друг к другу.
3. Установите также YetAnotherConfigLib — Fabric API **не** нужен.
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
| Проверка места на сервере | вкл | Запоминать, где вы были при выходе с сервера, и показывать скриншот снова только если при входе вы оказались примерно на том же месте. Если вы попали в другое место (лобби, другой мир и т.п.) — показывается слайд-шоу, чтобы скриншот не вводил в заблуждение. |
| Вспомогательные экраны переходов | вкл | Показывать скриншот (или слайд-шоу) поверх перехода при входе в мир или на сервер и при выходе из них. Экран плавно появляется, запускает загрузку и держится, пока не появится экран загрузки (скрывая промежуточные экраны), после чего плавно исчезает. Размытие и затемнение такие же, как на экране загрузки, но статичные. |
| Время fade анимации переходов | 800 мс | Длительность появления и исчезновения вспомогательного экрана. Одна и та же для входа и выхода. 0 = без анимации. |
| Слайд-шоу, если нет скриншота | вкл | Показывать слайд-шоу, когда у мира ещё нет скриншота. |
| Ждать все чанки | вкл | Держать экран загрузки, пока число чанков растёт. |
| Свой экран загрузки | вкл | Заменять ванильный текст загрузки, карту чанков и полосу прогресса на собственные анимированные текст, иконку и полосу прогресса мода. Выкл. — показывается ванильный экран загрузки. |
| Счётчик чанков | выкл | Показывать «прогружено / всего» сверху экрана. |
| Сила размытия | 16 | Насколько сильно размыт скриншот. 0 = без размытия, максимум 64. |
| Скорость анимации размытия | 50% | Какую часть затухания занимает уменьшение размытия. 100% — размытие уходит ровно за время затухания, 50% — вдвое быстрее, 0% — без анимации. В слайд-шоу анимация размытия не работает. |
| Затемнение фона | 35% | Затемнение поверх картинки. Применяется сразу целиком, плавно не появляется. |
| Время fade анимации | 800 мс | Сколько времени скриншот исчезает, когда мир уже готов. |
| Разрешение картинки | 100% | Разрешение, в котором берётся скриншот. Картинка всегда масштабируется под экран, меньшее значение только делает её мягче. |
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

Готовый jar появится в `build/libs/dopes-seamless-loading-screen-<версия мода>+<версия Minecraft>.jar`.

### Ссылки

- **Страница на Modrinth:** <https://modrinth.com/mod/dopes-seamless-loading-screen>
- **GitHub:** <https://github.com/zomb1ch/dopes-seamless-loading-screen>
- **Ошибки и предложения:** <https://github.com/zomb1ch/dopes-seamless-loading-screen/issues>
- **README (полная документация):**
  <https://github.com/zomb1ch/dopes-seamless-loading-screen#readme>

### Лицензия

[MIT](https://github.com/zomb1ch/dopes-seamless-loading-screen/blob/main/LICENSE) — можно свободно
использовать, изменять и включать в сборки.
