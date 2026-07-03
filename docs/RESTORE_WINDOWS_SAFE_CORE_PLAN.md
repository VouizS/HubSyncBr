# HubSyncBr 0.7.5.3 — Restore Windows / Safe Core

Diagnóstico:
- A janela está viva, porque aparece em "Janelas abertas".
- Ela não aparece no núcleo porque alguma camada visual do workspace ficou oculta ou fora do alvo correto.
- O candidato mais provável é o auto-hide dos slots "+" internos, que pode ter escondido um container maior do que deveria.

Correção:
- Desativar temporariamente o auto-hide agressivo dos slots internos.
- Restaurar visibilidade de containers com "Janela".
- Restaurar visibilidade de WebViews.
- Não usar bringToFront() no workspace transformado.
- Manter topbar/sidebar fixos.
