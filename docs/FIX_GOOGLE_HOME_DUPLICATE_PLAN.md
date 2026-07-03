# HubSyncBr 0.7.5.1 — Fix Google Home Duplicate

Correção:
- Remove métodos duplicados `hsGoogleHomeUrl()`.
- Mantém apenas um método oficial retornando `https://www.google.com/`.
- Evita repetir o mesmo erro em novas execuções da atualização.
- Mantém os recursos já aplicados da 0.7.5.
