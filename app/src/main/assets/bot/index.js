const { Client, LocalAuth } = require('whatsapp-web.js');
const qrcode = require('qrcode-terminal');

const client = new Client({
    authStrategy: new LocalAuth(),
    puppeteer: {
        args: ['--no-sandbox', '--disable-setuid-sandbox']
    }
});

client.on('qr', (qr) => {
    console.log('ESCANEIE O QR CODE ABAIXO:');
    qrcode.generate(qr, { small: true });
});

client.on('ready', () => {
    console.log('Bot de WhatsApp está pronto!');
});

client.on('message', message => {
    if (message.body === '!ping') {
        message.reply('pong');
    }
});

client.initialize();
