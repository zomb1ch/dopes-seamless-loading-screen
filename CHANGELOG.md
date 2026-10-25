# Changelog

## 2.2

**Loading tips in the corner, a settings preview, and a smarter loading screen.**

### Added

- **Loading tips.** A short tip — a Minecraft fun fact or a joke — is shown in the top left
  corner of the loading screen, drawn in the bundled uni 05_53 pixel font (the same font the rest
  of the mod's text is styled after). Two to three lines at most, half the screen wide, with a little
  dash on the left. Can be turned off in the settings. 50 tips are translated for all ten locales.
- **New in this version: animated previews for the settings.** Custom loading screen, chunk counter,
  tips, waiting for all chunks, blur, dim, blur animation speed and fade duration each show an
  animated preview of the setting right in their description - generated with the mod's actual
  rendering pipeline, not a mock-up.
- New settings groups ("Loading Screen", "Transition Screens") and translations for the new
  settings in all ten languages.

### Changed

- **Waiting for all chunks now only runs while a real screenshot is on screen.** Without one — the
  world has no screenshot yet, or a server that shows the slideshow instead — the loading screen
  closes as soon as the world is ready, like vanilla. (The min / max show times are unchanged.)
- **Strong dim settings no longer darken the picture twice.** During the handover between the
  auxiliary and the main loading screen the overlay used to draw its dim on top of the loading
  screen's own dim, which showed as a clearly visible pulse on dark settings. From 50% dim up, the
  overlay now leaves the dim to the loading screen below it, so the picture stays evenly dark.
- **The settings are regrouped** into General / Loading Screen / Transition Screens / Display /
  Timing / Slideshow, so every option lives with its own kind.

### Fixed

- **The reset button icon in the settings is no longer tiny.** YACL (up to 3.8.x, the newest version
  available for 1.21.9 – 1.21.11) draws the little ↻ of the per-option reset buttons with a thin
  fallback glyph that reads as a dot. The mod now ships its own bold glyph for it, the same way
  newer YACL versions fixed it on 26.x.
- **The animated setting previews no longer tear into stripes and patches.** YACL draws every GIF
  frame at the image origin and ignores the offset stored in the frame itself, while optimised GIFs
  only keep the rectangle that actually changed — so the picture came apart. The previews are now
  re-encoded with every frame covering the whole canvas, at the original 600 px width.
- **Fades in the previews run smoothly now.** The source recordings change brightness in uneven
  steps, so the previews are rebuilt frame by frame instead of being resampled as-is: intermediate
  frames are interpolated, the overall brightness follows a smoothed curve, and frame delays vary —
  a long hold is a single frame, a fast fade is a burst of short ones. Grain is reduced too, with a
  light blur before quantisation and one shared palette without dithering.
  `tools/reencode_previews.py` rebuilds them from the masters in `tools/preview-masters`.
- **The preview atlases are freed when the settings close.** YACL builds an animated preview when the
  screen opens, but keeps the finished atlas in a static cache until the game exits — for these
  previews that adds up to several hundred megabytes of video memory that was never given back. The
  mod now drops its own previews from that cache as soon as the settings screen closes; opening the
  screen again loads them from scratch.

---

## 2.2 (Русский)

**Подсказки в углу, предпросмотр настроек и более умный экран загрузки.**

### Добавлено

- **Подсказки при загрузке.** В левом верхнем углу экрана загрузки показывается короткая подсказка —
  забавный факт или шутка о Minecraft — нарисованная пиксельным шрифтом uni 05_53, встроенным в
  мод (тот же шрифт, в стиле которого оформлен остальной текст мода). Максимум две-три строки, до
  половины ширины экрана, с чёрточкой слева. Отключается в настройках. Все 50 подсказок переведены
  на десять языков.
- **Новое в этой версии: анимированные превью настроек.** В описаниях своего экрана загрузки,
  счётчика чанков, подсказок, ожидания всех чанков, силы размытия, затемнения, скорости анимации
  размытия и времени fade анимации показывается анимированное превью соответствующей настройки -
  сгенерированное настоящим конвейером рендера мода, а не нарисованное от руки.
- Новые группы настроек («Экран загрузки», «Экраны переходов») и переводы новых настроек на все
  десять языков.

### Изменено

- **Ожидание всех чанков теперь работает, только пока на экране настоящий скриншот.** Без него — у
  мира ещё нет скриншота, или сервер показывает слайд-шоу — экран загрузки закрывается сразу, как
  только мир готов, как в ванилле. (Мин./макс. время показа не изменились.)
- **Сильное затемнение больше не затемняет картинку дважды.** При передаче от вспомогательного
  экрана к основному оверлей рисовал своё затемнение поверх затемнения экрана загрузки, что на
  тёмных настройках было видно как заметный скачок. От 50% затемнения и выше оверлей теперь
  оставляет затемнение экрану загрузки под ним — картинка остаётся равномерно тёмной.
- **Настройки перегруппированы** в «Основные» / «Экран загрузки» / «Экраны переходов» / «Отображение»
  / «Тайминги» / «Слайд-шоу», чтобы каждая опция лежала среди своих.

### Исправлено

- **Иконка кнопки сброса в настройках больше не крошечная.** YACL (до 3.8.x — новее для
  1.21.9 – 1.21.11 не выпускалось) рисует ↻ у кнопок сброса тонким запасным глифом, из-за чего
  иконка читается как точка. Мод теперь приносит собственный жирный глиф для неё — так же, как это
  починили в новых версиях YACL на 26.x.
- **Анимированные превью настроек больше не разваливаются на полосы и пятна.** YACL рисует каждый
  кадр GIF в начало картинки и игнорирует смещение, записанное в самом кадре, а оптимизированные GIF
  хранят только изменившийся прямоугольник — поэтому картинка разъезжалась. Превью пересобраны так,
  что каждый кадр занимает весь холст, в исходной ширине 600 px.
- **Переходы в превью стали плавными.** В исходных записях яркость меняется неравномерными
  ступенями, поэтому превью собираются заново, а не прореживаются как есть: промежуточные кадры
  дорисовываются интерполяцией, общая яркость идёт по сглаженной кривой, а задержки кадров разные —
  долгая пауза это один кадр, быстрый переход — серия коротких. Заодно стало меньше зернистости:
  лёгкое размытие перед квантованием и одна общая палитра без дизеринга. Пересобрать их можно через
  `tools/reencode_previews.py` из мастер-копий в `tools/preview-masters`.
- **Атласы превью освобождаются при закрытии настроек.** YACL собирает анимированное превью при
  открытии экрана, но готовый атлас держит в статическом кэше до выхода из игры — для этих превью
  это несколько сотен мегабайт видеопамяти, которые раньше не возвращались. Теперь мод выкидывает
  свои превью из этого кэша, как только экран настроек закрывается; при следующем открытии они
  загружаются заново.

---

## 2.1

**The loading screen background is sharp again: the picture is no longer enlarged by the graphics card,
and the blur is a real blur instead of a grid of blocks.**

### Fixed

- **The screenshot is taken again on 1.21.x.** The render target and the texture filter were looked up
  by name through reflection, which silently does nothing in the obfuscated game, so on 1.21.9 – 1.21.11
  no screenshot was ever written and the background fell back to the slideshow. Both lookups are plain
  calls again.
- **No more big, clearly visible pixels.** The picture used to be capped at its own resolution, so a
  small source — the bundled placeholders, or a screenshot taken at a lower resolution than the current
  window — was magnified by the GPU with nearest filtering, which turned every pixel into a square. It
  is now scaled to exactly the size it is drawn at, so the texture is sampled one to one and the
  texture filter no longer matters at all.
- **The blurred background is smooth.** The blur levels were built by averaging the image down and
  interpolating it back up, which left one visible cell per pixel of the small version. They are now
  produced with a separable box blur — three passes over a sliding window — a real, round blur with no
  cell structure.
- The sharp picture is resized with an area average and a proper interpolation instead of a point
  sample, so the downscaled version no longer shows stair steps.

### Changed

- The **image size** option only softens the picture now. The texture is always built at the size it is
  drawn at, so a lower value no longer saves video memory — use it to make the background softer, not
  smaller.
- Preparing the blur costs about 0.2 – 0.4 s while the loading screen is already up. It used to be
  faster, but it left the artefacts described above.

---

## 2.1 (Русский)

**Фон экрана загрузки снова чёткий: картинку больше не растягивает видеокарта, а размытие — это
настоящее размытие, а не сетка из квадратов.**

### Исправлено

- **Скриншот снова снимается на 1.21.x.** Цель рендера и фильтр текстур искались по имени через
  рефлексию, а в обфусцированной игре это молча ничего не делает: на 1.21.9 – 1.21.11 скриншот не
  сохранялся вообще, и фон подменялся слайдшоу. Теперь это обычные вызовы.
- **Больше нет крупных, отчётливо видимых пикселей.** Картинка ограничивалась собственным разрешением,
  поэтому маленький источник — встроенные плейсхолдеры или скриншот, снятый при меньшем разрешении
  окна — растягивался видеокартой с nearest-фильтрацией, и каждый пиксель превращался в квадрат. Теперь
  картинка масштабируется ровно под размер отрисовки: текстура сэмплится один к одному, и фильтр
  вообще перестаёт влиять.
- **Размытый фон стал гладким.** Уровни размытия строились через уменьшение картинки и обратную
  интерполяцию, из-за чего оставалась одна видимая клетка на пиксель уменьшенной версии. Теперь это
  разделяемое box-размытие — три прохода скользящим окном — настоящее, круглое размытие без клеток.
- Чёткая картинка уменьшается усреднением по площади и нормальной интерполяцией, а не точечной
  выборкой, поэтому ступенек в уменьшенной версии больше нет.

### Изменено

- Опция **«Разрешение картинки»** теперь только смягчает картинку. Текстура всегда строится под размер
  отрисовки, так что меньшее значение больше не экономит видеопамять — используйте его, чтобы сделать
  фон мягче, а не меньше.
- Подготовка размытия занимает примерно 0,2 – 0,4 с, пока экран загрузки уже показан. Раньше было
  быстрее, но с теми самыми артефактами.

---

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
