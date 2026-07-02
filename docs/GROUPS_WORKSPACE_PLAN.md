# HubSyncBr 0.5 — Groups / Workspace

Objetivo: transformar o núcleo do app em um workspace de grupos, mantendo as janelas como WebViews nativas.

Implementado nesta versão:
- Tela de grupos do Hub.
- Grupo inicial `Meu Hub` com YouTube e Twitch.
- Criar novo grupo.
- Entrar em um grupo como workspace próprio.
- Cada grupo guarda suas próprias janelas.
- Cada grupo pode ter até 8 janelas abertas.
- Cada grupo pode exibir até 4 janelas visíveis no núcleo.
- Janelas extras ficam ocultas/minimizadas e podem voltar pelo gerenciador.
- Gerenciador de janelas passa a trabalhar no grupo ativo.
- Menu principal ganha ações: Ver grupos e Novo grupo.
- O núcleo deixa de ser apenas uma grade fixa e vira base para grupos estilo Chrome.

Decisão técnica:
- O núcleo não vira iframe/HTML puro.
- O workspace parece um app web/desktop, mas continua nativo no Android.
- Cada site continua rodando em sua própria WebView, com mais chance de compatibilidade.

Próximos passos planejados:
- Nomear/renomear grupos.
- Minimizar janela em pílula inferior.
- Cards de preview mais parecidos com grupos do Chrome.
- Arrastar janelas para formar grupos.
- Layouts salvos por grupo.
- Workspace scale/zoom do núcleo.
