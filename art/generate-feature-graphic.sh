#!/bin/bash

echo "Generating feature graphics to ~/botwa-icons/botwa-feature-graphic.png..."
mkdir -p ~/botwa-icons/
rsvg-convert feature-graphic.svg > ~/botwa-icons/feature-graphic.png
