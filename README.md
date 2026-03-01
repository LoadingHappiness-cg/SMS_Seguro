<p align="center">
  <img src="./assets/logo.png" alt="SMS Seguro" width="120" />
</p>

# SMS Seguro

Proteção simples contra burlas por SMS.  
Uma aplicação Android gratuita e open source criada para ajudar a identificar mensagens suspeitas antes que seja tarde demais.

O SMS Seguro analisa automaticamente SMS recebidos e alerta quando deteta sinais comuns de fraude, como links suspeitos ou pedidos de pagamento Multibanco.

Este projeto nasceu de um problema real: pessoas próximas perderam dinheiro devido a burlas por SMS.  
O objetivo é simples — usar tecnologia para proteger pessoas.

---

## O que o SMS Seguro faz

- ⚠️ Deteta links suspeitos
- 🔎 Identifica domínios falsificados ou disfarçados
- 💳 Analisa pagamentos Multibanco
- 🏦 Verifica Entidade e Referência
- 🔄 Deteta inconsistências (ex: SMS diz CTT mas entidade é outra)
- 🔐 Funciona sem enviar dados para servidores
- 📱 Mostra alertas claros e simples

Tudo é feito localmente no telemóvel, sem recolha de dados pessoais.

---

## Para quem é

O SMS Seguro foi pensado especialmente para:

- Pessoas menos experientes com tecnologia
- Utilizadores mais velhos
- Familiares que querem proteger os seus pais ou avós
- Qualquer pessoa que queira mais segurança

Se funciona para quem percebe pouco de tecnologia, funciona para todos.

---

## Porque este projeto existe

As burlas por SMS estão a aumentar.

Muitas pessoas perdem dinheiro porque as mensagens parecem legítimas:

- “A sua encomenda está pendente”
- “Taxa alfandegária por pagar”
- “Confirme o seu MBWay”
- “Pagamento em atraso”

O SMS Seguro tenta responder a um problema simples:

E se o telemóvel pudesse avisar antes de a pessoa ser enganada?

---

## Princípios do projeto

### Sempre gratuito

Esta aplicação será sempre gratuita.  
A proteção contra burlas deve estar disponível para todos.

### Sempre open source

O código será sempre aberto.  
Qualquer pessoa pode ver como funciona e contribuir.

### Privacidade primeiro

O SMS Seguro não envia mensagens nem dados pessoais para servidores.  
Tudo é analisado localmente no dispositivo.

### Simplicidade

A aplicação foi desenhada para ser fácil de usar:

- Sem configurações complicadas
- Sem termos técnicos

### Tecnologia com um propósito

A tecnologia deve servir as pessoas.  
Este projeto existe para ajudar.

---

## Como funciona

O SMS Seguro monitoriza notificações de SMS recebidos e analisa:

- URLs suspeitos
- Caracteres disfarçados (ex: domínios falsos)
- Pedidos de pagamento Multibanco
- Entidades conhecidas
- Inconsistências entre a mensagem e o pagamento

Se for detetado risco, a aplicação mostra um alerta claro.

---

## Segurança

- Nenhum SMS é enviado para servidores
- Nenhum dado pessoal é recolhido
- Funciona offline
- Atualizações de regras são assinadas digitalmente

---

## Privacidade

Política de Privacidade pública:

https://loadinghappiness-cg.github.io/SMS_Seguro/

---

## Instalação

### APK (versão de testes)

`app/build/outputs/apk/debug/sms-seguro-0.1.1-alpha.apk`

Instalar manualmente no Android.

## Publicar na Play

### Gerar AAB

A Google Play usa AAB, não APK.

```bash
./gradlew :app:bundleRelease
```

Output esperado:

`app/build/outputs/bundle/release/app-release.aab`

### Signing (Play App Signing)

- Use Play App Signing com uma upload key.
- A keystore deve ficar fora do repositório.
- O local recomendado para credenciais locais é `~/.gradle/gradle.properties`.
- `local.properties` pode ser usado apenas se não houver alternativa, e nunca deve ser commitado.
- Não guarde paths absolutos sensíveis nem passwords no repo.

## Release checks

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleRelease
./gradlew :app:bundleRelease
```

Paths úteis:

- APK release para testes: `app/build/outputs/apk/release/sms-seguro-0.1.1-alpha.apk`
- AAB para Play: `app/build/outputs/bundle/release/app-release.aab`

---

## Contribuições

Contribuições são bem-vindas.

Ideias, melhorias e correções podem ser submetidas através de Pull Requests ou Issues.

---

## Licença

MIT License

---

## Autor

Carlos Gavela  
https://carlosgavela.com

---

## Nota final

Este projeto nasceu de uma ideia simples:

Se conseguirmos evitar que uma pessoa seja burlada, já valeu a pena.
