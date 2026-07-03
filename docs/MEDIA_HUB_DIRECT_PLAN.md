# HubSyncBr 0.7.1 — Media Hub Direto

Problema observado:
A build 0.7.0 funcionou, mas o Media Hub ficou apenas como card dentro da homepage. Isso fez parecer que o recurso não entrou como seção real do app.

Correção:
- Media Hub vira entrada direta na sidebar.
- Homepage usa link interno `hubsyncbr://media`.
- WebView intercepta `hubsyncbr://media` e abre `mediaHubDataUrl()`.
- Nova janela Media Hub pode ser criada diretamente pelo app.

Arquivos grandes:
- O arquivo local continua sendo selecionado pelo seletor seguro do Android.
- O app não copia filme grande para o armazenamento interno.
- Formatos ideais: MP4/H.264/AAC, MP3, JPG, PNG, WEBP, GIF.
