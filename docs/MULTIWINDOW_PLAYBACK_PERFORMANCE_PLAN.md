# HubSyncBr 0.7.5.8 — MultiWindow Playback Performance

Foco:
- Melhorar reprodução simultânea em duas ou mais janelas.
- Reduzir travadas ao abrir janelas.
- Evitar que uma WebView atrapalhe a outra.

Estratégia:
- Aplicar tunagem leve em cada WebView encontrada.
- Usar aceleração por hardware nas WebViews.
- Liberar reprodução sem gesto obrigatório quando possível.
- Manter DOM/cache/storage por WebView.
- Definir prioridade alta de renderização para WebViews em Android compatível.
- Instalar um observador leve de layout para otimizar novas janelas sem varredura agressiva.
- Usar throttle para não otimizar repetidamente no mesmo segundo.

O que esta versão não faz:
- Não remove o + interno.
- Não muda o desenho do núcleo.
- Não adiciona mover janela individual.
