# Xiaomi / MIUI Checklist

This checklist is ready to use both for QA and as the basis for an in-app "Ajuda" page.

## User help copy

### PT

Se usares um Xiaomi ou MIUI/HyperOS, confirma estes pontos para o SMS Seguro continuar ativo:

- Ativar `Auto-start` para a app
- Definir `Bateria` como `Sem restrições`
- Fixar a app nos Recent apps, se o modelo tiver essa opção
- Permitir notificações e popups, se o sistema bloquear alertas
- Confirmar que a notificação persistente de proteção está visível

### EN

If you use a Xiaomi device with MIUI or HyperOS, check these settings so SMS Seguro stays active:

- Enable `Auto-start` for the app
- Set `Battery` to `No restrictions`
- Lock the app in Recent apps if your model supports it
- Allow notifications and popups if the system blocks alerts
- Confirm that the persistent protection notification is visible

## QA checklist

- Notification Access enabled
- App notifications enabled
- Persistent notification visible
- Battery set to `No restrictions`
- `Auto-start` enabled when available
- App survives device reboot
- Warning notification still appears after locking and unlocking the device
- Xiaomi help page opens from the app
- Tutorial deep link back to the app works: `smsseguro://protecao`

## Suggested in-app help sections

- Why Xiaomi may block protection
- How to enable Notification Access
- Battery and background restrictions
- Auto-start
- Persistent notification troubleshooting
- Return to app button using `smsseguro://protecao`
