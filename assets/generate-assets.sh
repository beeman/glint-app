#!/usr/bin/env bash
set -euo pipefail

ASSETS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$ASSETS_DIR/.." && pwd)"
RES_DIR="$ROOT_DIR/app/src/main/res"

HEADER_SVG="$ASSETS_DIR/glint-header.svg"
ICON_SVG="$ASSETS_DIR/glint-icon.svg"

require_command() {
    if ! command -v "$1" >/dev/null 2>&1; then
        printf "Missing required command: %s\n" "$1" >&2
        exit 1
    fi
}

install_if_changed() {
    local source_file="$1"
    local output_file="$2"

    if [ -f "$output_file" ] && cmp -s "$source_file" "$output_file"; then
        return
    fi

    cp "$source_file" "$output_file"
}

render_padded_png() {
    local source_svg="$1"
    local canvas_size="$2"
    local image_size="$3"
    local output_png="$4"
    local tmp_png="$TMP_DIR/render-${canvas_size}-${image_size}.png"
    local tmp_output="$TMP_DIR/$(basename "$output_png")-${canvas_size}-${image_size}.png"

    rsvg-convert -w "$image_size" -h "$image_size" "$source_svg" -o "$tmp_png"
    magick -size "${canvas_size}x${canvas_size}" xc:none "$tmp_png" -gravity center -compose over -composite -strip -define png:exclude-chunk=time "$tmp_output"
    install_if_changed "$tmp_output" "$output_png"
}

render_png() {
    local source_svg="$1"
    local width="$2"
    local height="$3"
    local output_png="$4"
    local tmp_output="$TMP_DIR/$(basename "$output_png")-${width}-${height}.png"

    rsvg-convert -w "$width" -h "$height" "$source_svg" -o "$tmp_output"
    install_if_changed "$tmp_output" "$output_png"
}

require_command magick
require_command perl
require_command rsvg-convert

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

GLYPH_SVG="$TMP_DIR/glint-icon-glyph.svg"
perl -0pe 's#<rect\b(?=[^>]*\bfill=)[^>]*/>##g' "$ICON_SVG" > "$GLYPH_SVG"

render_png "$HEADER_SVG" 1200 600 "$ASSETS_DIR/glint-header.png"
render_png "$ICON_SVG" 512 512 "$ASSETS_DIR/glint-icon.png"
render_png "$ICON_SVG" 256 256 "$RES_DIR/drawable-nodpi/splash_icon.png"

for density in \
    "hdpi:72:162" \
    "mdpi:48:108" \
    "xhdpi:96:216" \
    "xxhdpi:144:324" \
    "xxxhdpi:192:432"
do
    IFS=: read -r bucket legacy_size foreground_size <<< "$density"
    mipmap_dir="$RES_DIR/mipmap-$bucket"
    legacy_png="$TMP_DIR/ic_launcher-$bucket.png"
    legacy_webp="$TMP_DIR/ic_launcher-$bucket.webp"

    render_png "$ICON_SVG" "$legacy_size" "$legacy_size" "$legacy_png"
    magick "$legacy_png" -strip -quality 90 "$legacy_webp"
    install_if_changed "$legacy_webp" "$mipmap_dir/ic_launcher.webp"
    install_if_changed "$legacy_webp" "$mipmap_dir/ic_launcher_round.webp"

    foreground_inner_size=$((foreground_size * 2 / 3))
    render_padded_png "$ICON_SVG" "$foreground_size" "$foreground_inner_size" "$mipmap_dir/ic_launcher_foreground.png"
    render_padded_png "$GLYPH_SVG" "$foreground_size" "$foreground_inner_size" "$mipmap_dir/ic_launcher_monochrome.png"
done

printf "Generated Glint assets from %s and %s\n" "$ICON_SVG" "$HEADER_SVG"
