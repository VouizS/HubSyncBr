# HubSyncBr 0.7.3.5 — HubHome Scope + Toolbar Compile Fix

Correcoes:
- Garante `hubHomeDataUrl()` tambem dentro da `StreamPane`.
- Reescreve `setMediaMode(boolean)` sem `toolbarView`, porque esse nome nao existe na StreamPane atual.
- Mantem `mediaMode` pronto para a proxima etapa: esconder a barra das janelas Media Hub usando o nome real da barra.
