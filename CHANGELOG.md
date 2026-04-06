# CHANGELOG

## v0.1.9

Release focada em tornar a proteção mais discreta e mais clara para beta testers.

- Reintroduzida a secção de atualizações OTA com versão, data e estado visíveis.
- Restaurado o botão "Procurar atualizações agora" no ecrã de proteção.
- Adicionado modo discreto para a notificação persistente do foreground service.
- Melhorada a higiene dos motivos mostrados ao utilizador, evitando IDs técnicos na UI.
- Recalibrada a pontuação da fraude bancária por callback para reduzir sobreposição de sinais.
- Reforçada a robustez das regras OTA com tratamento seguro de regex inválidas.
- Mantida a deteção de fraude bancária com regras OTA assinadas.

## v0.1.8

Release focada em tornar a proteção mais discreta e mais clara para beta testers.

- Reintroduzida a secção de atualizações OTA com versão, data e estado visíveis.
- Restaurado o botão "Procurar atualizações agora" no ecrã de proteção.
- Adicionado modo discreto para a notificação persistente do foreground service.
- Melhorada a higiene dos motivos mostrados ao utilizador, evitando IDs técnicos na UI.
- Recalibrada a pontuação da fraude bancária por callback para reduzir sobreposição de sinais.
- Reforçada a robustez das regras OTA com tratamento seguro de regex inválidas.
- Mantida a deteção de fraude bancária com regras OTA assinadas.

## 0.1.7
- Play Closed Test re-release after versionCode 6 was already consumed
- No functional changes beyond the release version bump

## 0.1.6
- Banking callback scam detection fixed
- OtpDetector false-safe behavior corrected
- History clear added
- OTA ruleset metadata visible in UI
- Regex hardening for OTA rules

## v0.1-alpha

Primeira versão funcional do SMS Seguro, focada na deteção de burlas por SMS em Portugal.

- Adicionado scoring de risco explicável (`LOW` / `MEDIUM` / `HIGH`).
- Adicionado parsing Multibanco com distinção entre entidade conhecida, intermediária e desconhecida.
- Adicionada deteção de inconsistência entre marca e entidade de pagamento.
- Adicionada deteção de spoofing Unicode/Cirílico em hostnames.
- Segurança: atualizações de ruleset assinadas com Ed25519.
- UX: diálogo de confirmação antes de abrir links suspeitos.
- Privacidade: deteção offline; nenhum conteúdo de SMS é enviado para servidores.
