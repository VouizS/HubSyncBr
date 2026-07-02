# HubSyncBr

HubSyncBr é um hub web multitela para abrir sites oficiais de vídeo em janelas independentes.

## Versão 0.1 — Dual View

Esta primeira versão é um MVP com foco em duas telas simultâneas:

- Duas WebViews lado a lado.
- YouTube e Twitch como URLs iniciais de teste.
- Campo de URL por janela.
- Botões voltar, avançar, recarregar e abrir externo.
- Botão Foco para ampliar uma janela dentro do app.
- Botão para alternar layout horizontal/vertical.
- Botão Swap Screens.
- Modo desktop/mobile por janela.
- Suporte a fullscreen do próprio site/player quando o site permitir.
- Aviso legal na primeira abertura.

## Posicionamento legal

O HubSyncBr não hospeda, retransmite, baixa, extrai ou modifica transmissões. Cada janela abre o site escolhido pelo usuário e respeita login, anúncios, créditos, player e regras da plataforma original.

## Compatibilidade

O app tenta abrir sites compatíveis com Android WebView. Alguns serviços podem limitar reprodução por DRM, cookies, login, política da plataforma ou exigência do app/navegador oficial.

## Build

O APK debug é gerado automaticamente pelo GitHub Actions em:

Actions → Android Debug APK → Artifacts → HubSyncBr-debug-apk

## Roadmap

### 0.2
- Favoritos locais.
- Histórico.
- Biblioteca de sites rápidos.
- Melhor tela offline.
- Melhor responsividade em celulares pequenos.

### 0.3
- Preparação para 4 telas.
- Perfis de layout.
- Área premium planejada.

### Futuro — Rooms
- Salas para assistir em grupo.
- Chat em tempo real.
- Papel de dono da sala.
- Papel de narrador.
- Moderação.
- Reações rápidas.
