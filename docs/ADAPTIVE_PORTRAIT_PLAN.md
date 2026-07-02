# HubSyncBr 0.5.1 — Adaptive Portrait Workspace

Objetivo: permitir que o modo retrato continue útil com múltiplas janelas visíveis, sem perder a lógica de Hub/Workspace.

Implementado:
- Modo retrato com layout adaptativo.
- 2 janelas em retrato passam a ficar empilhadas para mostrar melhor o conteúdo.
- 3/4 janelas em retrato entram em grade compacta 2x2.
- Quando o retrato precisa mostrar 3/4 janelas, a barra URL de cada janela é ocultada para liberar área do site.
- A sidebar é recolhida automaticamente em retrato com 2+ janelas.
- Cards de `Janelas abertas` ficam mais compactos.
- Botões `Ocultar`, `Foco` e `Fechar` viram ícones pequenos.
- `Nova janela` no gerenciador vira linha limpa, sem botão/card grande.

Decisão de design:
- Conteúdo primeiro.
- Controles pequenos ficam por tooltip/menus.
- O workspace no retrato é uma visão útil, não apenas uma cópia apertada do modo paisagem.

Próximos passos:
- Minimizar janela em pílula.
- Grupos com cards de preview estilo Chrome.
- Escala/zoom do workspace.
- Arrastar para agrupar.
