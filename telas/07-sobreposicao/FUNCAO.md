# 07 — Posição segura

## Objetivo

Simulação de área livre e exclusões; esconder se contexto for desconhecido.

## Referência

Origem: PROPOSTA. IDs: F10,F13.

## Campos e controles visíveis

Sem formulário obrigatório nesta tela.

## Botões e ações

- Voltar ao início → abrir painel
- Ajuda → abrir ajuda
- ocultar → alteração de prévia/configuração
- revalidar → alteração de prévia/configuração
- Escolher → posicao
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

## Geometria e validade

Não usar canto fixo como garantia. Contexto legítimo, perfil validado, retângulos de exclusão e margem testada. Nova oferta invalida resultado anterior. Mudança de orientação, escala, teclado, janela ou perda de contexto oculta. Sem área segura, remover a janela. Toque através não resolve colisão.
