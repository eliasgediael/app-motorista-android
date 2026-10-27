# 46 — Cadastrar preferência

## Objetivo

Definir mínimo e patamar bom de um critério, ativação opcional e base do cálculo.

## Referência

Origem: VÍDEO + PROPOSTA. IDs: F04,F05,F06,F07. Vídeo: 03:30–04:35.

## Campos e controles visíveis

- Valor mínimo
- Considerado bom a partir de
- Base de distância/tempo

## Botões e ações

- Voltar ao início → abrir painel
- Ajuda → abrir ajuda
- R$/km → alteração de prévia/configuração
- R$/hora → alteração de prévia/configuração
- Nota → alteração de prévia/configuração
- criterio-ativo → alteração de prévia/configuração
- Definir combinação → abrir combinacao
- Revisar preferência → salvar
- Início → abrir painel
- Finanças → abrir financas
- Histórico → abrir historico
- Ajustes → abrir preferencias

## Regras comuns

O PNG é referência de interface, não screenshot de APK. Dados fictícios e estados coloridos de exemplo não definem preferências reais. Valores ausentes continuam não configurados. Integração 99Pop, leitura automática, permissões e serviços nativos dependem de implementação e validação. Não aceitar/recusar/enviar automaticamente.

## Persistência e validação

Na implementação Android: definir repositório, estados carregando/vazio/erro/sucesso e restauração de processo apropriados; campos monetários em centavos e bases de tempo/distância explícitas. Não persistir dados identificáveis de passageiros por padrão. Nunca registrar receita apenas por análise de oferta.

## Critérios de aceitação

- Navegação abre destinos indicados e permite voltar sem perder rascunho indevidamente.
- Ausências não viram zero nem recomendação inventada.
- Textos em fonte ampliada e alvos de toque testados no Android.
- Nenhum controle aciona aplicativo externo sem ação explícita e suporte válido.
- Testes devem verificar o comportamento real, sem declarar integração com base nesta imagem.

## Cadastro da preferência

Fluxo: escolher métrica → ativar se desejar → informar mínimo e patamar bom → escolher base de cálculo → revisar intervalos → revisar política de combinação → confirmar versão do RuleSet. Rejeitar faixa invertida, negativo onde inválido e conversão decimal ambígua. Comportamento no limite exato deve ser confirmado. Não emitir Recusar geral até a combinação ser aprovada.
