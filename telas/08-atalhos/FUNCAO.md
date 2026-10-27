# 08 — Controle flutuante

## Objetivo

Menu de liga/desliga e atalhos, sem ações em aplicativos de terceiros.

## Referência

Origem: VÍDEO. IDs: F12. Vídeo: 07:15–07:35.

## Campos e controles visíveis

- Transparência do botão

## Botões e ações

- Voltar ao início → abrir painel
- Ajuda → abrir ajuda
- botao → alteração de prévia/configuração
- Posição seguraAjustar longe dos botões da oferta → abrir sobreposicao
- atalho-analise → alteração de prévia/configuração
- atalho-voz → alteração de prévia/configuração
- atalho-jornada → alteração de prévia/configuração
- atalho-camera → alteração de prévia/configuração
- atalho-ocultar → alteração de prévia/configuração
- Ver menu de exemplo → menu
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

## Botão flutuante

Botão separado do balão. Abre menu de controles próprios. Precisa de ativação explícita e área segura. Não gerar toques/gestos na plataforma. Sem fonte legítima de contexto, desativar sobreposição automática e manter atalhos internos.
