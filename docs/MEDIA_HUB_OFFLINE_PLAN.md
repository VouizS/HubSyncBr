# HubSyncBr 0.7 — Media Hub Offline MVP

Objetivo:
Adicionar utilidade offline para o HubSyncBr, permitindo abrir arquivos de mídia locais dentro de janelas do app.

Primeira versão:
- Media Hub como página local do HubSyncBr.
- Atalho na homepage.
- Selecionar mídia do aparelho.
- Reproduzir vídeo local com `<video controls>`.
- Reproduzir áudio local com `<audio controls>`.
- Exibir imagem e GIF local com `<img>`.
- Sem copiar arquivos grandes para o app.
- Arquivos grandes são acessados pelo seletor do Android, evitando duplicar armazenamento.

Observações:
- Melhor compatibilidade inicial: MP4/H.264/AAC, MP3, JPG, PNG, WebP, GIF.
- MKV/HEVC/áudios especiais dependem do codec do Android/WebView do aparelho.
- Para filmes grandes, usar 1 vídeo ativo por vez em aparelhos fracos.
- Futuro: player nativo/Media3 para suporte mais forte, biblioteca persistente e favoritos offline.
