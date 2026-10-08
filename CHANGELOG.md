# Changelog

## 2.0

**The biggest update so far: the mod now brings its own loading screen and turns every world change
into one smooth, seamless transition.**

### Added

- **Seamless transition into and out of your world.** Leaving a world or a server and coming back is a
  single animated sequence: the screenshot fades in over the Minecraft menu, stays on top for the whole
  load (hiding the vanilla screens in between) and melts into the world. No flashing, no jumping
  screens.
- **Own loading screen.** The vanilla "Downloading terrain" text, the chunk map and the vanilla
  progress bar are replaced by the mod's own animated text and icon plus a smooth progress bar that
  fills up while the world and its chunks load. It can be turned off in the settings to keep the
  vanilla look.
- **A progress bar that actually moves.** It follows the chunks streaming in, so it fills up gradually
  instead of jumping, and it always reaches 100% before the world appears.
- **10 localizations:** English, Russian, Ukrainian, German, Spanish, French, Italian, Polish,
  Brazilian Portuguese and Simplified Chinese.

### Changed

- **Fabric API is no longer required** — Fabric Loader and YACL are enough.
- The loading screen waits for the chunks (and for the bar to catch up) and never hangs, even on
  servers that never report a world load.
- Animations are driven by the clock, so a low tick rate or a heavy frame no longer makes them stall.
- **One jar per Minecraft version line:** `1.21.9 – 1.21.11`, `26.1 – 26.1.2`, `26.2`, `26.3`.
  1.21.9 is the minimum supported version.

### Fixed

- The loading screen no longer hangs when the chunk count never reaches the expected number.
- The progress bar no longer jumps from 50% straight to 100%.
- Empty cells at the end of a sprite sheet are no longer played as animation frames.
- The screenshot is taken at the right moment on 26.x, where the level render hook moved.

---

## 2.0 (Русский)

**Самое большое обновление: мод приносит собственный экран загрузки и превращает каждую смену мира в
одну плавную бесшовную анимацию.**

### Добавлено

- **Бесшовный переход в мир и обратно.** Выход из мира или с сервера и возвращение — одна анимация:
  скриншот проявляется поверх меню Minecraft, держится всю загрузку (скрывая промежуточные ванильные
  экраны) и растворяется в мире. Ничего не мелькает и не перескакивает.
- **Свой экран загрузки.** Ванильный текст «Downloading terrain», карта чанков и ванильная полоса
  прогресса заменены собственными анимированными текстом и иконкой и плавной шкалой, которая
  заполняется во время загрузки мира и чанков. Настройку можно выключить и оставить ванильный вид.
- **Шкала, которая реально двигается.** Она идёт по мере приёма чанков, поэтому заполняется
  постепенно, а не скачком, и всегда доходит до 100% до появления мира.
- **10 локализаций:** английская, русская, украинская, немецкая, испанская, французская, итальянская,
  польская, бразильская португальская и упрощённая китайская.

### Изменено

- **Fabric API больше не нужен** — достаточно Fabric Loader и YACL.
- Экран загрузки дожидается чанков (и заполнения шкалы) и не зависает даже на серверах, которые вообще
  не сообщают о загрузке мира.
- Анимации привязаны к часам, поэтому низкий тикрейт или тяжёлый кадр их больше не подвешивают.
- **Отдельный jar на линию версий Minecraft:** `1.21.9 – 1.21.11`, `26.1 – 26.1.2`, `26.2`, `26.3`.
  Минимальная поддерживаемая версия — 1.21.9.

### Исправлено

- Экран загрузки больше не зависает, если число чанков не доходит до ожидаемого.
- Шкала больше не перескакивает с 50% сразу на 100%.
- Пустые ячейки в конце спрайт-листа больше не проигрываются как кадры анимации.
- Скриншот снимается в правильный момент на 26.x, где переехал хук рендера уровня.
