# 11 — Histórico de corridas

## Objetivo

Filtrar ofertas, confirmar realização e exportar registros revisados.

## Referência

Origem: SITE + VÍDEO. IDs: F13,S03. Vídeo: 13:45–13:50.

## Campos e controles visíveis

Sem formulário obrigatório nesta tela.

## Botões e ações

- Voltar ao início → abrir painel
- Ajuda → abrir ajuda
- Hoje → alteração de prévia/configuração
- Semana → alteração de prévia/configuração
- Mês → alteração de prévia/configuração
- Todos → alteração de prévia/configuração
- 99 → alteração de prévia/configuração
- Uber → alteração de prévia/configuração
- inDrive → alteração de prévia/configuração
- Exportar CSV de exemplo → csv
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

## Confirmação e exportação

Analisada não significa aceita nem realizada. Só corrida manualmente confirmada gera sugestão de receita; lançar/ajustar/ignorar explicitamente. Exportação revisável e sem identificação de passageiro por padrão. Testar dupla confirmação e deduplicação.
