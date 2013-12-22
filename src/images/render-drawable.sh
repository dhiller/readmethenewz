#!/usr/bin/env sh

set -e

SCRIPTPATH=$( cd $(dirname "$0") ; pwd -P )

PROJECT_PATH=$( cd $(dirname "$SCRIPTPATH/../../..") ; pwd -P )
DRAWABLE_INPUT_PATH="$SCRIPTPATH/drawable"
DRAWABLE_OUTPUT_PATH="$PROJECT_PATH/src/main/res/drawable"

convert_and_optimize() {
  convert -background none -resize "$1" "$2" "$3"
  optipng -o7 "$3"
}

convert_and_optimize_all_sizes() {
  convert_and_optimize "36x36" "$1/$2" "$3-ldpi/$4"
  convert_and_optimize "48x48" "$1/$2" "$3-mdpi/$4"
  convert_and_optimize "72x72" "$1/$2" "$3-hdpi/$4"
  convert_and_optimize "96x96" "$1/$2" "$3-xhdpi/$4"
}

( cd "$DRAWABLE_INPUT_PATH" && find . -type f -name \*.svg ) | while read IFILE ; do
  OFILE="$( dirname $IFILE )/$( basename "$IFILE" .svg ).png"
  convert_and_optimize_all_sizes      \
    "$DRAWABLE_INPUT_PATH"  "$IFILE"  \
    "$DRAWABLE_OUTPUT_PATH" "$OFILE"
done
