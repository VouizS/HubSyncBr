# HubSyncBr 0.6 — Browser Core + Smart Splash

Objetivo: evoluir o HubSyncBr para um navegador multitela próprio, mantendo o motor WebView e a lógica de grupos/workspace.

Implementado nesta etapa:
- SplashActivity nativa premium.
- Carregamento real de preferências antes de abrir o MainActivity.
- Preferências iniciais do navegador:
  - homepage_mode = hub
  - search_engine = google
  - nav_autohide = true
  - open_new_window_home = true
- Nova janela abre na HubSyncBr Home.
- Página inicial própria do HubSyncBr usando data URL local, sem depender do Google como tela inicial.
- Busca inteligente usando mecanismo configurável.
- Opção de configuração do navegador no menu principal.
- Barra de navegação da janela aparece ao carregar URL e pode ocultar ao rolar a página.

Decisão técnica:
- O HubSyncBr não cria um motor de navegador do zero.
- Cada site continua rodando em WebView nativa.
- O núcleo passa a ter comportamento de browser/workspace com identidade própria.

Próximos passos:
- Interface de configurações mais completa.
- Favoritos locais.
- Histórico local.
- Grupos com preview estilo Chrome.
- Minimizar janelas em pílulas.
