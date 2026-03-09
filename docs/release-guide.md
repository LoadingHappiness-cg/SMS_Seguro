# SMS Seguro Release Guide

## Objetivo

Gerar um `AAB` assinado para Google Play, validar o binário e preparar a submissão com metadata consistente.

## Pré-requisitos

- Keystore de produção disponível
- Variáveis configuradas:
  - `RELEASE_STORE_FILE`
  - `RELEASE_STORE_PASSWORD`
  - `RELEASE_KEY_ALIAS`
  - `RELEASE_KEY_PASSWORD`

## Build

```bash
./gradlew clean test lintDebug bundleRelease
```

Se ainda não existir keystore de produção, gerar um pacote verificável sem assinatura:

```bash
./gradlew bundleReleaseUnsigned
```

## Artefactos esperados

- `app/build/outputs/bundle/release/app-release.aab`
- ou `app/build/outputs/bundle/releaseUnsigned/app-releaseUnsigned.aab`

## Checklist antes do upload

- `versionName` e `versionCode` corretos
- Política de privacidade publicada
- Data Safety consistente com o binário
- Descrições e screenshots finais carregadas na Play Console
- Foreground service declarado na Play Console com a mesma justificação usada na app

## Verificação pós-upload

- Executar internal testing
- Rever Pre-launch report
- Confirmar ausência de crashes, ANRs e flags de policy
