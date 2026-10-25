"""Пересобирает превью настроек так, чтобы YACL показывал их без артефактов.

Проблема с кадрами
==================
YACL (3.8.x для 1.21.x и 3.9.x для 26.x) читает GIF через ImageIO и складывает кадры
в один атлас. При этом он рисует КАЖДЫЙ кадр в точку (0, 0), игнорируя смещение
imageLeftPosition / imageTopPosition из ImageDescriptor, хотя ImageIO возвращает
только изменённый прямоугольник кадра. Оптимизированные GIF (ffmpeg, gifski, любой
редактор) хранят именно такие прямоугольники, поэтому в игре кадры «разъезжаются»:
появляются полосы и пятна из старого содержимого.

Решение: записать каждый кадр целиком на весь холст (bbox = весь кадр). Тогда
(0, 0) — верная позиция. Неизменённые пиксели остаются прозрачными (disposal=1),
поэтому файлы не разрастаются: Pillow вырезает дельту прозрачностью, а не
смещением прямоугольника.

Проблема с яркостью
===================
Яркость в исходниках меняется неравномерно: на затемнениях шаг то крупный, то
мелкий, а часть кадров дублируется. Если просто проредить запись, глаз видит не
плавный переход, а рывки. Поэтому кадры не прореживаются «как есть», а
пересобираются по времени:

  * между соседними исходными кадрами дорисовываются промежуточные (линейная
    интерполяция) с шагом --step мс;
  * общая яркость каждого промежуточного кадра подтягивается к сглаженной кривой
    (--smooth): исходники сняты рывками, и без этого переход идёт «ступеньками»
    даже после интерполяции;
  * затем кадры склеиваются так, чтобы на каждый записанный кадр приходилось
    примерно одинаковое видимое изменение. Яркость тогда меняется равномерно, а
    статичные куски не занимают десятки одинаковых кадров;
  * длительности кадров остаются переменными — YACL читает delayTime каждого
    кадра отдельно, так что длинная пауза — это один кадр, а быстрый переход —
    много коротких.

Дополнительно:
  * ширина 600 px — исходный размер превью; YACL рисует картинку шириной
    ~250-360 GUI-пикселей, то есть до ~720 физических при GUI scale 2;
  * одна общая палитра на всю анимацию без дизеринга: иначе каждый кадр шумит
    по-своему, дельта-прозрачность перестаёт работать, а на затемнениях разные
    участки картинки «срабатывают» в разные кадры (то самое неравномерное
    изменение яркости);
  * лёгкое размытие перед квантованием убирает дизеринг исходника — ту самую
    зернистость, из-за которой превью выглядели шумными.

Запуск:
    python tools/reencode_previews.py                       # из мастер-копий в tools/preview-masters
    python tools/reencode_previews.py --source-dir <dir>    # из другой папки с исходниками
    python tools/reencode_previews.py --only blur,dim --dry-run
    python tools/reencode_previews.py --source-dir .        # пережать текущие файлы на месте

Мастер-копии (600 px, из которых собраны текущие превью) лежат в
tools/preview-masters. Если их там нет, старые 960 px записи можно достать из
релизного jar-а 2.2:
    unzip release/dopes-seamless-loading-screen-2.2+1.21.9*.jar \
          'assets/dopes_seamless_loading_screen/previews/*' -d <dir>
Учтите, что в jar-е лежат более ранние записи: у части превью другое число кадров
и другая длительность, поэтому как источник они годятся только для тех файлов,
где содержимое совпадает покадрово.

Пересборка из мастеров идемпотентна. Пересборка «на месте» (--source-dir .)
только ухудшает картинку, поэтому уже исправленные файлы пропускаются — чтобы
всё-таки пережать их с другими параметрами, добавьте --force.
"""

from __future__ import annotations

import argparse
import math
from pathlib import Path

import PIL.GifImagePlugin as GifPlugin
from PIL import Image, ImageChops, ImageFilter, ImageSequence, ImageStat

PROJECT_ROOT = Path(__file__).resolve().parent.parent
PREVIEWS = PROJECT_ROOT / "src/main/resources/assets/dopes_seamless_loading_screen/previews"
MASTERS = PROJECT_ROOT / "tools/preview-masters"

# Ширина превью в пикселях. 600 — исходный размер, в который укладывается панель
# описания YACL при GUI scale 2.
DEFAULT_WIDTH = 600

# Потолок числа кадров на превью. Атлас YACL занимает
# кадры * ширина * высота * 4 байта видеопамяти, поэтому кадры — самый дорогой
# ресурс. Но с 2.2 мод выгружает атласы, как только экран настроек закрывается
# (PreviewAtlasCache), так что память занята только пока меню открыто. Значения
# подобраны под суммарный атлас ~0.55 ГБ: движение и затемнения идут мелкими
# шагами, а не рывками.
MAX_FRAMES = {
    "blur": 72,
    "blurspeed": 56,
    "chunkcounter": 120,
    "customloadingscreen": 64,
    "dim": 72,
    "fadeduration": 68,
    "tips": 120,
    "waitingallchunks": 36,
}

# Размытие перед квантованием: убирает дизеринг исходника, не съедая детали.
DEFAULT_SOFTEN = 0.45

# Сглаживание кривой яркости: окно в миллисекундах. В исходниках яркость
# меняется неравномерно, поэтому без сглаживания плавное затемнение идёт рывками.
DEFAULT_SMOOTH = 60.0

# Без этого Pillow снова начнёт обрезать кадры до изменённого прямоугольника.
_original_getbbox = GifPlugin._getbbox


def _full_canvas_bbox(base: Image.Image, frame: Image.Image):
    delta, _ = _original_getbbox(base, frame)
    return delta, (0, 0, frame.width, frame.height)


GifPlugin._getbbox = _full_canvas_bbox


def read_frames(path: Path) -> tuple[list[Image.Image], list[int]]:
    """Полностью скомпонованные кадры (Pillow сам учитывает disposal) и их длительности."""
    frames: list[Image.Image] = []
    durations: list[int] = []
    with Image.open(path) as gif:
        for frame in ImageSequence.Iterator(gif):
            frames.append(frame.convert("RGB"))
            durations.append(max(10, frame.info.get("duration") or 40))
    return frames, durations


def difference(left: Image.Image, right: Image.Image) -> float:
    """Средняя абсолютная разница по каналам, 0..255."""
    stat = ImageStat.Stat(ImageChops.difference(left, right))
    return sum(stat.mean) / len(stat.mean)


class Timeline:
    """Исходные кадры с возможностью получить содержимое на любой момент времени."""

    def __init__(self, frames: list[Image.Image], durations: list[int]):
        self.frames = frames
        self.durations = durations
        self.starts: list[int] = []
        elapsed = 0
        for duration in durations:
            self.starts.append(elapsed)
            elapsed += duration
        self.total = elapsed

    def at(self, moment: int) -> Image.Image:
        """Кадр на момент moment мс: между исходными кадрами — линейная интерполяция."""
        index = len(self.frames) - 1
        for candidate in range(len(self.frames) - 1):
            if self.starts[candidate] <= moment < self.starts[candidate + 1]:
                index = candidate
                break
        duration = self.durations[index]
        alpha = (moment - self.starts[index]) / duration if duration else 0.0
        if alpha <= 0.0 or index == len(self.frames) - 1:
            return self.frames[index]
        return Image.blend(self.frames[index], self.frames[index + 1], min(1.0, alpha))


def build_timeline(path: Path, width: int, soften: float) -> Timeline:
    frames, durations = read_frames(path)
    height = max(2, round(frames[0].height * width / frames[0].width))
    frames = [frame.resize((width, height), Image.LANCZOS) for frame in frames]
    if soften > 0:
        frames = [frame.filter(ImageFilter.GaussianBlur(soften)) for frame in frames]
    return Timeline(frames, durations)


def luminance(frame: Image.Image) -> float:
    mean = ImageStat.Stat(frame).mean
    return sum(mean) / len(mean)


def smooth_curve(values: list[float], sigma: float) -> list[float]:
    """Гауссово сглаживание одномерной кривой (значения яркости по времени)."""
    if sigma <= 0 or len(values) < 3:
        return list(values)
    radius = max(1, int(sigma * 3))
    kernel = [math.exp(-0.5 * (offset / sigma) ** 2) for offset in range(-radius, radius + 1)]
    total = sum(kernel)
    out: list[float] = []
    for index in range(len(values)):
        accumulated = 0.0
        for offset, weight in enumerate(kernel):
            sample = min(max(index + offset - radius, 0), len(values) - 1)
            accumulated += values[sample] * weight
        out.append(accumulated / total)
    return out


_LUT_CACHE: dict[float, list[int]] = {}


def gain_lut(gain: float) -> list[int]:
    key = round(gain, 4)
    lut = _LUT_CACHE.get(key)
    if lut is None:
        table = [max(0, min(255, round(value * key))) for value in range(256)]
        lut = table * 3
        _LUT_CACHE[key] = lut
    return lut


def graded_sampler(timeline: Timeline, step: int, smooth: float):
    """Функция момент -> кадр, у которого общая яркость сглажена по времени."""
    moments = list(range(0, timeline.total, step))
    raw = [luminance(timeline.at(moment)) for moment in moments]
    curve = smooth_curve(raw, smooth / step)
    gains = [
        max(0.8, min(1.25, target / value)) if value > 1.0 else 1.0
        for target, value in zip(curve, raw)
    ]

    def at(moment: int) -> Image.Image:
        frame = timeline.at(moment)
        gain = gains[min(moment // step, len(gains) - 1)]
        return frame.point(gain_lut(gain)) if abs(gain - 1.0) > 0.002 else frame

    return at


def measure(at, total: int, step: int) -> list[float]:
    """Изменение между соседними моментами времени (шаг step мс)."""
    changes: list[float] = []
    previous = at(0)
    moment = step
    while moment < total:
        current = at(moment)
        changes.append(difference(previous, current))
        previous = current
        moment += step
    return changes


def segment(changes: list[float], step: int, budget: int, min_delay: int, max_hold: int) -> list[tuple[int, int]]:
    """Разбить таймлайн на кадры с примерно равным видимым изменением.

    Возвращает пары (момент кадра, длительность). Порог изменения подбирается
    так, чтобы уложиться в budget кадров.
    """
    total = sum(changes)
    if not changes or total <= 0:
        return [(0, max(step, len(changes) * step))]

    def run(threshold: float) -> list[tuple[int, int]]:
        out: list[tuple[int, int]] = []
        start = 0
        accumulated = 0.0
        for index, change in enumerate(changes):
            accumulated += change
            elapsed = (index + 1 - start) * step
            if (accumulated >= threshold and elapsed >= min_delay) or elapsed >= max_hold:
                moment = start * step + elapsed // 2
                out.append((moment, elapsed))
                start = index + 1
                accumulated = 0.0
        remainder = (len(changes) - start) * step
        if remainder > 0:
            if out:
                moment, delay = out[-1]
                out[-1] = (moment, delay + remainder)
            else:
                out.append((0, remainder + step))
        return out

    low, high = 1e-4, max(total, 1e-3)
    best = run(high)
    for _ in range(24):
        middle = (low + high) / 2
        candidate = run(middle)
        if len(candidate) > budget:
            low = middle
        else:
            best = candidate
            high = middle
    return best


def global_palette(frames: list[Image.Image]) -> Image.Image:
    """Одна палитра на 255 цветов для всей анимации: стабильные пиксели -> рабочая дельта."""
    width, height = frames[0].size
    mosaic = Image.new("RGB", (width, height * len(frames)))
    for index, frame in enumerate(frames):
        mosaic.paste(frame, (0, index * height))
    return mosaic.quantize(colors=255, method=Image.Quantize.MEDIANCUT)


def is_full_canvas(path: Path) -> bool:
    """True, если кадры уже на весь холст (файл пересобран этим скриптом)."""
    with Image.open(path) as gif:
        full = (0, 0, gif.width, gif.height)
        for index in range(gif.n_frames):
            gif.seek(index)
            if gif.tile and gif.tile[0][1] != full:
                return False
    return True


def process(source: Path, target: Path, width: int, budget: int, step: int,
            min_delay: int, max_hold: int, soften: float, smooth: float) -> None:
    timeline = build_timeline(source, width, soften)
    at = graded_sampler(timeline, step, smooth)
    changes = measure(at, timeline.total, step)
    segments = segment(changes, step, budget, min_delay, max_hold)
    print(f"{target.name}: {len(timeline.frames)} src frames -> {len(segments)} frames "
          f"({timeline.total} ms, {timeline.frames[0].width}px wide)")

    frames = [at(moment) for moment, _ in segments]
    palette = global_palette(frames)
    quantized = [frame.quantize(palette=palette, dither=Image.Dither.NONE) for frame in frames]
    for frame in quantized:
        # Прозрачность YACL не нужна: кадр должен занимать весь холст.
        frame.info.pop("transparency", None)

    delays = [delay for _, delay in segments]
    # YACL хранит задержку в миллисекундах, GIF — в сотых долях секунды.
    durations = [max(1, round(delay / 10)) * 10 for delay in delays]

    quantized[0].save(
        target,
        save_all=True,
        append_images=quantized[1:],
        duration=durations,
        loop=0,
        optimize=True,
        disposal=1,
    )

    check = Image.open(target)
    boxes = set()
    for index in range(check.n_frames):
        check.seek(index)
        boxes.add(check.tile[0][1] if check.tile else None)
    full = (0, 0, check.width, check.height)
    if boxes != {full}:
        raise SystemExit(f"{target.name}: not all frames cover the full canvas: {sorted(boxes)}")

    # Оценка атласа, который построит YACL: кадры пакуются в сетку.
    count = check.n_frames
    cols = math.ceil(math.sqrt(count) / math.sqrt(check.width / check.height))
    rows = math.ceil(count / cols)
    atlas_mb = check.width * cols * check.height * rows * 4 / 1024 / 1024
    print(
        f"  {check.width}x{check.height}, {count} frames, "
        f"{target.stat().st_size / 1024 / 1024:.2f} MB, "
        f"YACL atlas {cols}x{rows} ~ {atlas_mb:.0f} MB VRAM"
    )


def main() -> None:
    parser = argparse.ArgumentParser(description="Re-encode config preview GIFs for YACL.")
    parser.add_argument("--source-dir", type=Path, default=None,
                        help="directory with master GIFs; default: tools/preview-masters if present, else in place")
    parser.add_argument("--width", type=int, default=DEFAULT_WIDTH, help="preview width in pixels")
    parser.add_argument("--max-frames", type=int, default=None, help="override the per-preview frame budget")
    parser.add_argument("--step", type=int, default=10, help="interpolation step in ms")
    parser.add_argument("--min-delay", type=int, default=30, help="minimum frame duration in ms")
    parser.add_argument("--max-hold", type=int, default=800, help="maximum frame duration in ms")
    parser.add_argument("--soften", type=float, default=DEFAULT_SOFTEN,
                        help="gaussian blur radius applied before quantisation (0 disables)")
    parser.add_argument("--smooth", type=float, default=DEFAULT_SMOOTH,
                        help="brightness curve smoothing window in ms (0 disables)")
    parser.add_argument("--only", default=None, help="comma separated list of previews to process")
    parser.add_argument("--dry-run", action="store_true", help="encode to a temp file, do not overwrite")
    parser.add_argument("--force", action="store_true",
                        help="re-encode files that are already full-canvas when encoding in place")
    args = parser.parse_args()

    if not PREVIEWS.is_dir():
        raise SystemExit(f"Not found: {PREVIEWS}")
    sources = args.source_dir or (MASTERS if MASTERS.is_dir() else PREVIEWS)
    only = {name.strip() for name in args.only.split(",") if name.strip()} if args.only else None

    for gif in sorted(PREVIEWS.glob("*.gif")):
        name = gif.stem
        if only and name not in only:
            continue
        source = sources / gif.name
        if not source.exists():
            print(f"{gif.name}: no source, skipped")
            continue
        in_place = source.resolve() == gif.resolve()
        if in_place and not args.force and is_full_canvas(gif):
            print(f"{gif.name}: already full-canvas, skipped (use --force to redo)")
            continue
        target = gif
        if args.dry_run:
            target = Path(PROJECT_ROOT / "build" / "preview-dry-run" / gif.name)
            target.parent.mkdir(parents=True, exist_ok=True)
        budget = args.max_frames or MAX_FRAMES.get(name, 60)
        process(source, target, args.width, budget, args.step,
                args.min_delay, args.max_hold, args.soften, args.smooth)


if __name__ == "__main__":
    main()
