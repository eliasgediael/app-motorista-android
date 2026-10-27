# 29 — Voz e notificações

## Objetivo

Métricas faladas selecionáveis, ordem, deduplicação e expiração.

## Referência

Origem: VÍDEO. IDs: F21,F13. Vídeo: 11:00–11:25; 13:45.

## Campos e controles visíveis

Sem formulário obrigatório nesta tela.

## Botões e ações

- Voltar ao início → abrir painel
- Ajuda → abrir ajuda
- voz → alteração de prévia/configuração
- notificacao → alteração de prévia/configuração
- voz-Valor da viagem → alteração de prévia/configuração
- voz-Ganho por km → alteração de prévia/configuração
- voz-Ganho por hora → alteração de prévia/configuração
- voz-Nota do passageiro → alteração de prévia/configuração
- voz-Motivo do alerta → alteração de prévia/configuração
- Prévia do aviso → previa-voz
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
