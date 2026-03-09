# SMS Seguro Play Store Pack

## Release setup

- Package name: `com.smsguard`
- Privacy Policy: https://loadinghappiness-cg.github.io/SMS_Seguro/
- Support email: `smsseguro@loadinghappiness.pt`
- Play App Signing: enabled

## Assets checklist

- App icon 512x512
- Feature graphic
- Screenshots: activation flow, suspicious alert, history, Xiaomi help
- Short description
- Full description
- Release notes

---

## PT

### App name

SMS Seguro

### Short description

Deteta SMS suspeitos e alerta antes de clicares. Privado e simples.

### Full description

SMS Seguro ajuda-te a identificar mensagens suspeitas (fraudes e phishing) diretamente nas notificações, com alertas claros e linguagem simples.

O que faz
- Analisa o texto das notificações de SMS para detetar padrões comuns de fraude
- Mostra um alerta quando encontra sinais de risco, como links, urgência artificial e pedidos de códigos
- Reduz falsos positivos em fluxos de autenticação confiáveis, por exemplo códigos de login

Privacidade
- Processamento local no dispositivo
- Não vendemos dados
- Sem Crashlytics ou trackers nesta versão

Importante
- Para funcionar, precisas de ativar o Acesso às Notificações
- Antes da ativação, explicamos exatamente o que é lido e porquê
- Podes desativar a monitorização quando quiseres

Ideal para
- PMEs, famílias e utilizadores que querem um alerta extra sem complicações

### Release notes 1.0.0

- Primeira versão pública
- Alertas claros para SMS suspeitos
- Melhorias para reduzir falsos positivos em fluxos de autenticação

---

## EN

### App name

SMS Seguro

### Short description

Detect suspicious SMS and warn before you click. Simple and private.

### Full description

SMS Seguro helps you spot suspicious SMS messages, including fraud and phishing attempts, straight from notifications with clear warnings and simple language.

What it does
- Checks SMS notification text for common scam patterns
- Warns when it detects risk signals such as links, urgency pressure, and code requests
- Reduces false positives for trusted authentication flows, for example login codes

Privacy
- On-device processing
- No data selling
- No Crashlytics or trackers in this release

Important
- To work, the app requires Notification Access
- We show an explicit disclosure before enabling it
- You can disable it anytime

Ideal for
- Families, small businesses, and users who want an extra warning layer without complexity

### Release notes 1.0.0

- First public release
- Clear warnings for suspicious SMS
- Reduced false positives for trusted authentication flows

---

## Notes for submission

- Data Safety should stay aligned with on-device processing and no data collection
- Foreground service declaration must match the `specialUse` implementation in the app
- Notification access must be explained consistently in onboarding, listing, and Play Console answers
