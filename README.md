# selo. — Assinatura eletrônica no seu bolso

> Challenge LetsSign · Sprint 1
> Aplicativo Android nativo para preparar, enviar, acompanhar e assinar documentos PDF, em parceria com a **LetsSign**.

---

## Integrantes

| Nome completo      | RM     |
|--------------------|--------|
| Beatriz Cavalcanti | 568493 |
| Bruna Sangoleti    | 567014 |
| Daniel Mariz       | 566721 |
| Douglas Cristian   | 566847 |
| Wesley Santos      | 567925 |

---

## Sobre o projeto

O **selo.** é um aplicativo mobile nativo para Android que leva o fluxo de assinatura eletrônica de documentos para o celular. Com ele, um usuário **Admin** consegue importar um PDF, escolher quem deve assinar, posicionar os campos de assinatura nas páginas, revisar e disparar o envio. Já o **Signatário** recebe o documento no próprio app, assina com o dedo e conclui o processo, atualizando o status do registro.

### Problemática

Preparar um documento para assinatura costuma depender de computador, de várias ferramentas e de processos pouco transparentes: quem já assinou, quando e se o documento final está íntegro. O desafio proposto pela LetsSign foi transformar a **preparação e o envio de um documento para assinatura em uma experiência simples, segura e intuitiva, direto pelo celular**.

### Solução

Um fluxo guiado em 5 passos para o Admin (dados, signatários, campos, revisão e envio) e um fluxo curto para o Signatário (abrir, assinar, concluir), com:

- validação do PDF na importação (corrompido, protegido por senha, sem páginas ou acima de 20 MB);
- posicionamento de campos por proporção da página, preservando o layout em qualquer dispositivo;
- envio com chave de idempotência, sem duplicar solicitações em caso de nova tentativa;
- acompanhamento com status por signatário e linha do tempo de eventos;
- integridade comprovada por hash SHA-256 do documento.

---

## Funcionalidades

### Acesso (ambos os perfis)
- Splash com verificação de sessão salva.
- Login por e-mail e senha para **Admin** e **Signatário**.
- Sessão persistida: ao reabrir o app, o usuário continua logado.
- Logout limpa a sessão.
- Admin padrão criado no primeiro uso (seed do banco).

### Perfil Admin
- **Home** com abas: Enviados, Parcialmente assinados, Concluídos e Rascunhos.
- **Cadastro de signatários** (criar, editar, listar e buscar por nome, e-mail ou telefone):
    - nome completo (ao menos duas palavras);
    - e-mail válido e único;
    - telefone com DDD e máscara;
    - senha com no mínimo 8 caracteres, armazenada como hash + salt.
- **Novo documento** (rascunho salvo automaticamente):
    1. **Dados:** título (até 120 caracteres) e seleção do PDF pelo Storage Access Framework.
    2. **Signatários:** seleção e classificação como contratante, contratado ou testemunha; remover ou substituir antes do envio.
    3. **Campos:** visualizar páginas, adicionar, arrastar, redimensionar e excluir campos de assinatura; mais de um campo por signatário.
    4. **Revisão:** documento final, lista de signatários, campos por página e mensagem do convite.
    5. **Envio:** resultado do disparo, com orientação e nova tentativa em caso de falha.
- **Acompanhamento:** status geral, status individual de cada signatário, linha do tempo (criação, envio, visualizações e assinaturas com data e hora), hash SHA-256 e acesso ao PDF final quando concluído.

### Perfil Signatário
- **Home** com abas Pendentes e Assinados (somente documentos recebidos e já enviados).
- **Assinar documento:** abre direto no primeiro campo do signatário, apenas os campos dele ficam ativos, navegação por "Próximo campo" e confirmação por biometria ou PIN antes de concluir.
- **Captura da assinatura:** desenho com o dedo em Canvas, exportado como PNG com fundo transparente e aplicado em todos os campos do signatário no documento.
- Ao salvar, o status do registro é atualizado.

---

## Regras de negócio implementadas

- Um documento precisa ter ao menos um signatário para ser enviado.
- Todo campo de assinatura é associado a um participante.
- Campos permanecem dentro dos limites da página.
- Dados obrigatórios são validados antes do envio (`ValidateDocumentUseCase`); o botão de confirmação fica desabilitado enquanto houver pendências, inclusive signatário sem campo.
- O documento enviado e suas configurações são **somente leitura**; qualquer alteração gera uma nova versão e um novo fluxo.
- O documento só é **concluído** quando todos os signatários obrigatórios assinam.
- Isolamento de dados: toda consulta filtra pelo usuário da sessão (`ownerId` para o Admin, signatário vinculado para o Signatário).

---

## Tecnologias utilizadas

| Camada | Tecnologia |
|--------|------------|
| Plataforma | Android nativo |
| Banco de dados local | Room (SQLite) |
| Sessão do usuário | DataStore |
| Armazenamento de PDF | Sistema de arquivos do dispositivo (`filesDir/documents/<docId>/v1.pdf`), com nome, caminho, data, hash e status registrados no Room |
| Seleção de arquivo | Storage Access Framework (`application/pdf`) |
| Visualização e validação de PDF | `PdfRenderer` |
| Campos e assinatura | Canvas (posicionamento dos campos e captura da assinatura em PNG transparente) |
| Segurança | Hash de senha com salt (BCrypt/PBKDF2), SHA-256 dos documentos, biometria/PIN na assinatura |

> Persistência estruturada em **repositórios**, para permitir a troca futura por APIs REST sem impacto nas demais camadas.

### Posicionamento dos campos

Cada campo é salvo com **página**, `xRatio`, `yRatio`, `widthRatio` e `heightRatio`, sempre em proporção ao tamanho da página do PDF. Assim, o posicionamento é preservado ao abrir o documento em outro dispositivo ou resolução. Tamanho padrão: 30% × 8% da página.

---

## Fluxo da aplicação

```
Splash ─► Login ─┬─► Admin ──► Home ─┬─► Signatários (listar / criar / editar)
                 │                   └─► Novo documento
                 │                         Dados ► Signatários ► Campos ► Revisão ► Resultado do envio
                 │                                                                      │
                 │                                              Acompanhamento ◄────────┘
                 │
                 └─► Signatário ──► Home (Pendentes / Assinados) ──► Assinar documento ──► Captura da assinatura ──► Concluir
```

---

## Telas

| # | Tela | Perfil |
|---|------|--------|
| 01 | Splash | Todos |
| 02 | Login | Todos |
| 03 | Home do Admin | Admin |
| 04 | Lista de signatários | Admin |
| 05 | Formulário de signatário | Admin |
| 06 | Novo documento: dados | Admin |
| 07 | Novo documento: signatários | Admin |
| 08 | Posicionamento de campos | Admin |
| 09 | Revisão e envio | Admin |
| 10 | Resultado do envio | Admin |
| 11 | Detalhe e acompanhamento | Admin |
| 12 | Home do Signatário | Signatário |
| 13 | Assinar documento | Signatário |
| 14 | Captura da assinatura | Signatário |

---

## Diferenciais

- Rascunho salvo automaticamente em todo o fluxo de criação.
- Envio idempotente: nova tentativa não duplica convites.
- Envio e convites gravados em uma única transação (rollback em caso de falha).
- Integridade comprovada por SHA-256, exibida no acompanhamento.
- Versionamento: documento enviado é imutável, alterações geram nova versão.
- Confirmação por biometria ou PIN antes de concluir a assinatura.
- Modo desenvolvedor para simular falha de envio na demonstração.
- Identidade visual própria, com cor de cada signatário refletida nos campos.

---

## Como executar

**Requisitos:** Android Studio atualizado e dispositivo ou emulador Android.

1. Clone ou extraia o projeto.
2. Abra a pasta no Android Studio e aguarde a sincronização do Gradle.
3. Execute em um emulador ou dispositivo físico (`Run ▶`).
4. Alternativamente, instale o **APK** entregue junto ao projeto.

### Acesso de demonstração

| Perfil | E-mail | Senha |
|--------|--------|-------|
| Admin | `admin@letssign.com` | _(preencher)_ |
| Signatário (Maria) | _(preencher)_ | _(preencher)_ |

---

## Entregas

- Código-fonte completo (`.zip`)
- APK
- Apresentação da solução (PPT/PDF)
- Vídeo demonstrativo (máx. 5 minutos): _(inserir link)_

---

## Créditos

Projeto desenvolvido para o **Challenge LetsSign · Sprint 1**, em parceria com a **LetsSign**.
