# HubSyncBr 0.6.2-r1 — Fix Long Logo String

Causa do failure:
A 0.6.2 colocou a logo em base64 diretamente dentro do MainActivity.java.
O Java recusou compilar porque uma constante/string ficou grande demais.

Correção:
- A logo passa a ser carregada como arquivo interno do app em `android_asset`.
- A homepage usa `file:///android_asset/hubsyncbr_mark.png`.
- Removemos `data:image/png;base64,...` gigante do Java.
- Mantemos a lógica de Smart Add Slots.
- Mantemos a splash usando `R.drawable.hubsyncbr_mark`.

Regra:
Nunca colocar imagens grandes em string Java. Logo/imagem deve ficar em `res/drawable` ou `assets`.
