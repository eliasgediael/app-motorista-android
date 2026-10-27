# Regras Apex: validação pendente

## O que foi recuperado

O chat Criar imagem UI Apex, ID 6ae0eaff-c074-83e9-be41-0f3626600b05, registra projeto novo, preferência preto/branco, aproveitar só regras/informações desejadas e preocupação com cartão perto do X. As respostas antigas vieram como referências de conteúdo, sem imagens/especificações completas. Nenhuma fórmula ou limite Apex pôde ser recuperado com confiança.

O vídeo agora enviado é referência funcional Rota Pro. Seus exemplos não aprovam regras Apex.

| Item | Estado | Confirmar |
|---|---|---|
| Faixas por km | Pendente | Valores, faixas e inclusão de embarque/retorno |
| Faixas por hora | Pendente | Valores e tempos incluídos |
| Mínimo da oferta | Pendente | Ativação e limite |
| Nota/passsageiro | Pendente | Se usar e como; nota não prova risco |
| Custo operacional | Pendente | Combustível, consumo, manutenção, fixos e rateio |
| Lucro mínimo | Pendente | Custos, taxas e extras/reembolsos |
| Percentual de lucro | Pendente | Margem sobre receita ou retorno sobre custo |
| Combinação | Pendente | Todos atendem? Prioridade? Dados ausentes? |
| Base km/min | Pendente | Embarque/espera/viagem/retorno, sem duplicar totais |
| Arredondamento | Pendente | Precisão e regra nos limites |
| Alertas | Pendente | Termos pessoais e correspondência |
| Jornada/metas | Pendente | Pausas, fuso, dia e base horária |

## Fórmulas candidatas, ainda não aprovadas como Apex

Receita/km = tarifa ao motorista / distância da base escolhida.
Receita/h estimada = tarifa / minutos da base escolhida × 60. É diferente do ganho/hora online da jornada.
Receita/min = tarifa / minutos da base escolhida.
Resultado estimado = tarifa − custos definidos − extras não reembolsados. Sem custo completo, não chamar de lucro líquido real.
Margem sobre receita = resultado/receita × 100; retorno sobre custo = resultado/custo × 100. Não intercambiar.

Definir entradas primeiro. Se tarifa já for valor ofertado ao motorista, não descontar comissão de novo. Não somar embarque quando campo já contém o total. Denominador não positivo: indisponível. Custo ausente não é custo zero.

Registrar para cada aprovação: ID, valor/fórmula, unidade, mensagem/data, versão e testes de fronteira. Mudanças não reescrevem avaliações históricas silenciosamente.
