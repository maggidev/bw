#!/bin/sh
set -e -u

echo "Generating ~/botwa-icons/ic_launcher.png..."
mkdir -p ~/botwa-icons/

vector2svg ../app/src/main/res/drawable/ic_launcher.xml ~/botwa-icons/ic_launcher.svg

sed -i "" 's/viewBox="0 0 108 108"/viewBox="18 18 72 72"/' ~/botwa-icons/ic_launcher.svg

SIZE=512
rsvg-convert \
	-w $SIZE \
	-h $SIZE \
	-o ~/botwa-icons/ic_launcher_$SIZE.png \
	~/botwa-icons/ic_launcher.svg

rsvg-convert \
	-b black \
	-w $SIZE \
	-h $SIZE \
	-o ~/botwa-icons/ic_launcher_square_$SIZE.png \
	~/botwa-icons/ic_launcher.svg
