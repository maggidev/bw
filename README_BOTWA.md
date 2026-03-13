# BotWA - Bot WhatsApp com Termux Integrado

Este projeto é um aplicativo Android que integra o código-fonte real do Termux sob o pacote `com.brx.botwa`.

## Funcionalidades
- **Tela Inicial**: Uma interface simples com o botão "Iniciar Bot".
- **Terminal Integrado**: O terminal do Termux roda nativamente dentro do app.
- **Node.js Automático**: O script de inicialização instala o Node.js e executa o bot.
- **Bot de WhatsApp**: Exemplo funcional incluído nas assets.

## Como Usar
1.  Suba este código para um repositório no GitHub.
2.  O **GitHub Actions** irá compilar o APK automaticamente (veja a aba "Actions").
3.  Instale o APK no seu Android.
4.  Ao clicar em "Iniciar Bot", o terminal abrirá.
5.  O script `init_bot.sh` (em assets) fará o resto.

## Notas Técnicas
- **Package Name**: `com.brx.botwa`
- **Bootstrap**: O app tentará baixar o bootstrap oficial do Termux. Para uma experiência 100% offline e customizada, recomenda-se compilar o bootstrap para o seu pacote usando o Docker do Termux.
- **NDK**: O projeto requer o Android NDK para compilar os componentes nativos do terminal.

## Estrutura de Assets
- `assets/bot/index.js`: Código do bot.
- `assets/bot/package.json`: Dependências do bot.
- `assets/bot/init_bot.sh`: Script de automação do terminal.
