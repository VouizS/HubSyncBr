# HubSyncBr 0.7.5.7 — Fast Open Optimization

Foco:
- Reduzir lag ao abrir janela.
- Evitar varreduras profundas repetidas no layout.
- Manter a estrutura visual atual.
- Não insistir agora em remover o "+" interno, porque a tentativa anterior criou custo extra.

Ajustes:
- hsScheduleForceHideInternalPlusSlots vira no-op.
- hsForceHideInternalPlusSlots vira no-op leve.
- hsApplyAddSlotVisibility vira no-op leve.
- hsRestoreWindowVisibilitySafe fica com throttle para não rodar várias vezes seguidas.
- hsApplyCoreTransform não faz varredura profunda em cada movimento.
- O "+" da topbar fica sem long press obrigatório.
