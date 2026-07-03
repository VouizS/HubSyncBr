# HubSyncBr 0.7.3.2 — Media Hub Base64 Fix

Correção aplicada:
- Remove todas as versões antigas do método `mediaHubDataUrl()`.
- Recria o método com HTML em Base64 para evitar erro de aspas no Java.
- Mantém o Media Hub separado da Home.
- Mantém a proposta de mídia local/offline: imagem, vídeo, áudio e GIF quando o WebView/dispositivo suportar.
