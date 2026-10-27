# 21 — Custo operacional

## Objetivo

Assistente para componentes de custo por km; modelo financeiro validável.

## Referência

Origem: VÍDEO + SITE. IDs: F08. Vídeo: 04:35; 11:00.

## Campos e controles visíveis

- Veículo em uso
- Tipo: próprio, financiado ou alugado
- Combustível (R$/litro)
- Consumo (km/litro)
- Manutenção por km
- Depreciação por km
- Custos fixos mensais
- Km de trabalho por mês

## Botões e ações

- Voltar ao início → abrir painel
- Ajuda → abrir ajuda
- Próprio → alteração de prévia/configuração
- Financiado → alteração de prévia/configuração
- Alugado → alteração de prévia/configuração
- Revisar cálculo → salvar
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

## Fórmula de custo pendente

Definir componentes e rateio antes de implementar. Combustível/consumo, manutenção, depreciação, financiamento/aluguel e custos fixos podem se sobrepor economicamente: validar o modelo para evitar dupla contagem. Custo ausente não é zero. Resultado parcial não é lucro líquido completo.
