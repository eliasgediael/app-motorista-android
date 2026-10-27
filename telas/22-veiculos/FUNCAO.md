# 22 — Meus veículos

## Objetivo

Cadastro de veículos e seleção de um ativo; próprio, financiado ou alugado.

## Referência

Origem: SITE. IDs: S10.

## Campos e controles visíveis

- Nome do veículo
- Tipo: carro ou moto
- Situação de propriedade
- Odômetro atual (km)

## Botões e ações

- Voltar ao início → abrir painel
- Ajuda → abrir ajuda
- Salvar na demonstração → salvar
- Custo do veículo → abrir custo
- Manutenção → abrir manutencao
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
