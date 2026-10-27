# 06 — Estilo do cartão

## Objetivo

Prévia completa/compacta, campos, ordem, fontes, tempo e opacidade.

## Referência

Origem: VÍDEO. IDs: F09,F10,F11. Vídeo: 05:00–06:35.

## Campos e controles visíveis

- Opacidade 90%

## Botões e ações

- Voltar ao início → abrir painel
- Ajuda → abrir ajuda
- Boa → alteração de prévia/configuração
- Atenção → alteração de prévia/configuração
- Ruim → alteração de prévia/configuração
- Completo → alteração de prévia/configuração
- Secundário → alteração de prévia/configuração
- destaque → alteração de prévia/configuração
- motivo → alteração de prévia/configuração
- Posição do cartãoAjustar dentro da área segura → abrir sobreposicao
- M → fonte
- Tempo total → alteração de prévia/configuração
- Distância total → alteração de prévia/configuração
- Nota do passageiro → alteração de prévia/configuração
- R$/km → alteração de prévia/configuração
- R$/hora → alteração de prévia/configuração
- R$/minuto → alteração de prévia/configuração
- Valor da oferta → alteração de prévia/configuração
- Resultado estimado → alteração de prévia/configuração
- Reordenar campos → ordem
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
