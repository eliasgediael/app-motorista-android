# Andamento

Pronto: inventário do vídeo com áudio automático de apoio; direção visual corrigida; 50 imagens de páginas/estados individuais; especificações e comandos por página; apresentação local navegável; arquitetura proposta; pendências Apex.

Verificação: 50 rotas exportadas sem erros de JavaScript; texto e geometria conferidos, com inspeção visual por amostragem. Referências não são testes do aplicativo Android.

Pendente: regras Apex, política de combinação, bases/custo e nome; implementação Kotlin/Compose; compilação/testes Android; análise de termos e viabilidade 99Pop; recursos sensíveis condicionados.

Teste da apresentação local: 50 rotas com conteúdo, sem erros de JavaScript; troca de balão para Recusar, abertura de menu flutuante e navegação para combinação de critérios verificadas. Balões isolados adicionais exportados.

## Versão 0.1.0 (teste) — estado em 27/10/2026

Nenhum build, teste ou execução em aparelho foi feito ainda. Este computador não tem Android SDK/Gradle e o usuário não autorizou instalar. A compilação e os testes ocorrerão no GitHub Actions.

### 1. Código implementado (escrito, não compilado)

- Base: projeto Gradle Kotlin DSL, Compose, tema claro com verde e cores funcionais, navegação para as 50 telas de `telas.json` (as não implementadas mostram "Ainda não implementada").
- 01 Painel; 03 Critérios de análise; 46 Cadastrar preferência; 47 Combinar critérios; 06 Estilo do cartão; 42/43/44 balões verde/amarelo/vermelho; 08 Controle flutuante.
- Extra de teste (fora das 50 telas): Simulador de oferta, com entrada manual.
- Domínio sem Android: validação de critérios (`domain/criteria`), cálculo de R$/km, R$/h, R$/min com base explícita (`domain/calc`), avaliação por faixa e política de combinação (`domain/assessment`), estilo do cartão (`domain/card`).
- Botão flutuante do próprio app (`floating/FloatingControlService`): botão arrastável, menu (balão verde/amarelo/vermelho de exemplo, ocultar, abrir app, desligar), balão em janela separada, sem toque.
- CI: `.github/workflows/android.yml` (testes, APK release assinado com chave de teste estável, release com nome por versão e commit).

### 2. Revisão estática realizada

Leitura do código, conferência de chaves/parênteses e imports por script, conferência de nomes de rotas contra `telas.json`, cálculo manual dos exemplos dos testes. Isto NÃO substitui compilação: erros de tipo, API ou versão de dependência podem existir.

### 3. Compilação e testes Android: NÃO executados

Testes escritos, não executados: `ScreenCatalogTest`, `CriterionValidatorTest`, `CardStyleTest`, `OfferCalculatorTest`, `OfferEvaluatorTest`. Nenhum teste instrumentado/UI. Versões (AGP 9.4.1, Kotlin 2.4.21, Compose BOM 2026.09.00, Navigation 2.10.2, Activity 1.13.0, Lifecycle 2.11.0, Gradle 9.8.2) vieram dos repositórios em 27/10/2026 e nunca foram validadas por build. compileSdk/targetSdk 36 e minSdk 26 são provisórios; Android mínimo não confirmado.

### Políticas e valores pendentes (nada foi inventado)

- Nenhum critério vem preenchido; mínimo, "bom" e base são digitados pelo usuário.
- Sem critério ativo e válido, ou sem as três escolhas da tela 47 (combinação, valor exato no mínimo, dado ausente), nenhuma cor geral é emitida.
- Fórmulas de R$/km, R$/h e R$/min são CANDIDATAS (`candidata-0.1`), de `referencias/validacoes-apex.md`; arredondamento de exibição (2 casas, meio para cima) é provisório; valores internos não são arredondados.
- Não implementados: valor mínimo da viagem, lucro mínimo, porcentagem de lucro, resultado estimado, custo operacional (modelo de custo pendente; "Resultado" aparece como indisponível).
- Ausente é desconhecido; denominador não positivo torna a métrica indisponível.

### Simulação x integração real

- Sem integração com 99Pop, Uber ou inDrive. Marcar aplicativos na tela 03 só registra preferência.
- Balões do cartão e do botão flutuante usam oferta de EXEMPLO (24,80 / 12,4 km / 22 min) ou entrada manual no simulador.
- Selo ACEITAR/ATENÇÃO/RECUSAR é informativo; não existe ação em plataforma, toque, gesto nem automação.
- Interruptores "Análise de corridas" e "Gravação de segurança" no Painel são só estado visual; sem câmera, sem serviço.
- Botão flutuante: apenas janela do próprio app. Exige permissão de sobreposição concedida pelo usuário; não sabe qual app está em primeiro plano nem onde ficam botões de terceiros. Comportamento em aparelhos reais ainda não testado.
- Estado em memória: critérios, política e cartão se perdem ao fechar o app (persistência local fica para a próxima versão).

### Próximas versões

0.2: persistência local, perfis, estilo/ordem de campos (48), cartão secundário (49), posição segura (07). Depois: jornada/histórico manuais, finanças, veículos, assistências. Recursos sensíveis (captura, gravação, leitura de tela) em fases próprias e condicionados a termos/viabilidade.
