# 45 — Menu do botão flutuante

## Objetivo

Menu aberto do copiloto sobre cenário fictício, com atalhos e desligar/ocultar.

## Referência

Origem: VÍDEO + PROPOSTA. IDs: F12. Vídeo: 07:15–07:35.

## Campos e controles visíveis

Sem formulário obrigatório nesta tela.

## Botões e ações

- Voltar ao início → abrir painel
- Ajuda → abrir ajuda
- Análise de corridas → abrir criterios
- Avisos por voz → abrir voz
- Jornada → abrir jornada
- null → ocultar-menu
- menu-analise → alteração de prévia/configuração
- menu-voz → alteração de prévia/configuração
- Ocultar → ocultar-menu
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

## Operação do menu

A imagem mostra menu aberto em cenário fictício. Análise, voz, jornada e ocultar são atalhos do próprio app. Abrir menu não pode cobrir X/recusa/aceite. Perda de contexto implica remover janela. Arraste/posicionamento na configuração, com veículo parado.
