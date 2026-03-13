#!/data/data/com.brx.botwa/files/usr/bin/bash
echo "Iniciando configuração do Bot WhatsApp..."
pkg update -y
pkg install -y nodejs-lts
mkdir -p ~/botwa
cp -r /sdcard/botwa/* ~/botwa/ || true
cd ~/botwa
if [ ! -d "node_modules" ]; then
  npm install
fi
node index.js
