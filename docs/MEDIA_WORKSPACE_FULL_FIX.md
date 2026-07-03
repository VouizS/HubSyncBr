# HubSyncBr 0.7.3.1 — Fix Media Workspace Full

Correção:
- A build da 0.7.3 falhou porque o HTML/JavaScript do Media Hub tinha aspas quebrando a String Java.
- Esta correção reescreve o método `mediaHubDataUrl()` usando criação de elementos por JavaScript, sem HTML com aspas perigosas.
- Mantém a ideia da 0.7.3: Media Hub separado, sem barra de URL na janela de mídia, e mídia local em tela cheia dentro da janela.
