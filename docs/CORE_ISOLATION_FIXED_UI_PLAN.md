# HubSyncBr 0.7.5.2 — Core Isolation / Fixed UI

Correção de arquitetura visual:
- Sidebar e topbar ficam fixas.
- O gesto de 2 dedos transforma somente o núcleo/workspace interno.
- Janelas e slots internos acompanham o núcleo.
- O seletor de workspace evita pegar a tela inteira.
- Inclui proteção para recentralizar quando o núcleo se afasta demais.

Estrutura desejada:
AppRoot fixo
├── Sidebar fixa
├── Topbar fixa
├── Floating actions fixas
└── CoreViewport fixo
    └── CoreWorkspace com zoom/arrasto
        ├── Janela 1
        ├── Janela 2
        └── Slot +
