# CHANGELOG

## v0.1-alpha

- Adicionado scoring de risco explicável (`LOW` / `MEDIUM` / `HIGH`).
- Adicionado parsing Multibanco com distinção entre entidade conhecida, intermediária e desconhecida.
- Adicionada deteção de inconsistência entre marca e entidade de pagamento.
- Adicionada deteção de spoofing Unicode/Cirílico em hostnames.
- Segurança: atualizações de ruleset assinadas com Ed25519.
- UX: diálogo de confirmação antes de abrir links suspeitos.
- Privacidade: deteção offline; nenhum conteúdo de SMS é enviado para servidores.
