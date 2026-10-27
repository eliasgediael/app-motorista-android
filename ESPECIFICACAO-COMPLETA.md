# Especificação completa — Android

## Direção vigente

Aplicativo Android novo, funcionalmente equivalente em categoria ao Rota Pro mostrado no tutorial. O usuário pediu fidelidade à organização operacional e aos balões coloridos. Identidade e código Apex não serão reutilizados. Nome/marca do novo app ainda indefinidos. Todas as páginas planejadas estão no índice, com imagem, função e comando próprios.

## Abrangência e evidência

Vídeo de 17:45,57: análise visual amostrada e transcrição automática local até o encerramento da fala; limitações em referencias/analise-video.md e audio.md. O site oficial foi consultado para complementar categorias. Função divulgada, configuração visível e execução demonstrada são evidências diferentes; não presumir implementação interna. A proposta não garante paridade técnica integral.

Fonte complementar: https://rotapro.app/ (consultado em 27/10/2026). Categorias adicionais: perfis, limites de tempo/distância, finanças, fechamento diário, metas, contas, reserva, categorias, veículos, manutenção, viagem particular, relatórios e backup. As telas dessas categorias são propostas de produto; não descrições auditadas do software concorrente. Os recursos iFood/Keeta anunciados em beta ficam somente como pesquisa futura, fora do foco 99Pop.

## Organização

1. Operação: início, plataformas, critérios, perfis, cartão, prévia segura, botão/menu flutuante e oferta de exemplo.
2. Jornada: controles de tempo, fases, relatórios, histórico e confirmação manual.
3. Financeiro: receita/despesa, fechamento, metas, contas, reserva e categorias.
4. Veículo: cadastro, custo operacional, manutenção, orçamento particular e recibo.
5. Assistência: mensagem preparada, alertas pessoais/de viagem, voz/notificações, navegação.
6. Registros: captura e gravação propostas com controles visíveis e biblioteca.
7. Configurações: permissões, aparência, conta, dispositivos, backup, privacidade e ajuda.
8. Estados específicos: balão verde, amarelo, vermelho, menu aberto, cadastro do critério, combinação, ordem e cartão secundário.

## Cores do balão

Branco no conteúdo e contorno/selo verde, amarelo ou vermelho. Texto e ícone acompanham a cor. Verde corresponde a estado bom, amarelo a intermediário e vermelho a abaixo dos critérios conforme política confirmada. ACEITAR/ATENÇÃO/RECUSAR são selos informativos, não ações na plataforma. Balão separado do botão flutuante de controle.

## Cadastro das preferências

Análise de corridas → Meus critérios → escolher métrica → ativar opcionalmente → cadastrar mínimo e patamar bom → conferir base de tempo/distância → escolher combinação → revisar e confirmar versão. Não usar valores do apresentador.

Proposta de faixas: abaixo do mínimo, vermelho; faixa entre mínimo e bom, amarelo; a partir de bom, verde. Confirmar comportamento no limite exato e validação das faixas antes da implementação. Nota, valor mínimo, lucro e limites de tempo/distância são opcionais e precisam de especificação própria. Custo ausente não é zero.

Combinação permanece pendente: um mínimo não atendido torna tudo vermelho? Prioridade? Exibir métricas separadas? Como tratar ausentes? A apresentação oferece opções sem escolher a política pelo usuário. Dado ausente não significa automaticamente reprovação.

## Cálculos e distinções

Receita/km e receita/h de oferta precisam de tarifa ao motorista e base explícita. Não descontar comissão novamente nem somar embarque duas vezes a um total. Denominador positivo obrigatório. Lucro só com modelo de custo validado; margem sobre receita e retorno sobre custo são diferentes. Receita/hora online da jornada usa duração da jornada, não apenas a viagem. Caixa, resultado econômico, provisão e manutenção paga precisam de bases distintas.

Todos os números de interface são fictícios. Exemplo do cartão: 24,80 / 12,4 = 2,00 R$/km; 24,80 / 22 × 60 = 67,64 R$/h; 24,80 / 22 = 1,13 R$/min. Isso demonstra aritmética, não configura preferência real.

## Persistência e privacidade

MVP proposto local, sem nuvem/telemetria e sem dados pessoais de passageiros por padrão. Viagem realizada exige confirmação; oferta analisada não gera receita automaticamente. Registros preservam versão da regra. Arredondamento explícito, dinheiro em centavos e domínio independente de Android. Exclusão/exportação voluntárias e revisão dos dados antes de compartilhar.

## Sobreposição e integração

Não há integração real neste pacote. Sobreposição futura exige fonte legítima de contexto, perfil/versão de layout validado e retângulos de exclusão do X, aceite e controles. Se mudar contexto, faltar espaço ou perder validade, ocultar. Não adotar canto fixo como garantia. Não contornar proteção de toques/sobreposições. Sem automatizar aceitação, recusa, gestos, envio de mensagens ou APIs privadas.

Gravação proposta sempre visível e consentida, não oculta. Captura, leitura de tela, acesso a mídia, conta e nuvem permanecem condicionados à viabilidade, políticas e implementação própria. Mapa/Street View não comprova segurança de rua/passageiro.

## Estados a implementar

Cada tela: carregando, vazio, erro, indisponível e sucesso quando aplicáveis; rascunho e restauração; negativa/revogação de permissão; dados incompletos; versão de regra desconhecida. Referências gráficas mostram exemplos, não todos os estados possíveis de um aplicativo em produção.

## Execução no terminal

Etapa 1: projeto Kotlin/Compose, tema e navegação. Etapa 2: cadastro/validação de regras e domínio de cálculo aprovado. Etapa 3: cartão e botão/menu em prévia. Etapa 4: jornada/histórico manuais. Etapa 5: finanças/veículos e assistências. Etapa 6: estudo de fontes/contexto e sobreposição 99Pop; integrar apenas o que for permitido e tecnicamente testado. Recursos sensíveis em fases próprias. Fixar versões após verificar ambiente. Não afirmar testes/compilação que não ocorreram.

## Cobertura por página

### 01 — Painel

Visão geral da jornada e acesso aos recursos essenciais.

Origem: VÍDEO; IDs: F01,F12,F13,F15; minuto: 01:30–02:45; 07:15; 10:15. Detalhes e comando em telas/01-painel/.

### 02 — Análise de corrida

Oferta demonstrativa com métricas e motivos, sem ação automática.

Origem: VÍDEO; IDs: F04,F05,F06,F07,F13; minuto: 03:30–04:35; 08:00–08:30. Detalhes e comando em telas/02-analise/.

### 03 — Critérios de análise

Faixas por km, hora e nota, mínimos, limites de tempo/distância e prioridades.

Origem: VÍDEO + SITE; IDs: F04,F05,F06,F07; minuto: 03:30–04:35. Detalhes e comando em telas/03-criterios/.

### 04 — Perfis de trabalho

Agrupar configurações, criar, duplicar e escolher perfil ativo.

Origem: SITE; IDs: S01. Detalhes e comando em telas/04-perfis/.

### 05 — Aplicativos de corrida

Escolher apps alvo e distinguir seleção de integração disponível.

Origem: VÍDEO; IDs: F03; minuto: 02:45; 04:55. Detalhes e comando em telas/05-plataformas/.

### 06 — Estilo do cartão

Prévia completa/compacta, campos, ordem, fontes, tempo e opacidade.

Origem: VÍDEO; IDs: F09,F10,F11; minuto: 05:00–06:35. Detalhes e comando em telas/06-cartao/.

### 07 — Posição segura

Simulação de área livre e exclusões; esconder se contexto for desconhecido.

Origem: PROPOSTA; IDs: F10,F13. Detalhes e comando em telas/07-sobreposicao/.

### 08 — Controle flutuante

Menu de liga/desliga e atalhos, sem ações em aplicativos de terceiros.

Origem: VÍDEO; IDs: F12; minuto: 07:15–07:35. Detalhes e comando em telas/08-atalhos/.

### 09 — Jornada

Iniciar, pausar, retomar e encerrar, com fases e tempo online.

Origem: VÍDEO; IDs: F15; minuto: 10:15–10:30. Detalhes e comando em telas/09-jornada/.

### 10 — Relatórios da jornada

Relatórios por período, fase e app; ganho online separado do ganho da oferta.

Origem: SITE; IDs: S02. Detalhes e comando em telas/10-relatorios/.

### 11 — Histórico de corridas

Filtrar ofertas, confirmar realização e exportar registros revisados.

Origem: SITE + VÍDEO; IDs: F13,S03; minuto: 13:45–13:50. Detalhes e comando em telas/11-historico/.

### 12 — Detalhe da corrida

Métricas, base, regra usada e confirmação manual, sem inferir receita.

Origem: PROPOSTA; IDs: S03. Detalhes e comando em telas/12-detalhe/.

### 13 — Melhores horários

Ranking por dia/período com amostra explícita e método pendente de aprovação.

Origem: VÍDEO; IDs: F16; minuto: 10:45–10:55. Detalhes e comando em telas/13-horarios/.

### 14 — Finanças

Receitas confirmadas, despesas, caixa e resultado estimado em bases separadas.

Origem: SITE; IDs: S04. Detalhes e comando em telas/14-financas/.

### 15 — Novo lançamento

Receita/despesa com categoria, plataforma, veículo e confirmação.

Origem: SITE; IDs: S04. Detalhes e comando em telas/15-lancamento/.

### 16 — Fechar o dia

Conferir receitas, odômetro, despesas e período antes de concluir.

Origem: SITE; IDs: S05. Detalhes e comando em telas/16-fechamento/.

### 17 — Metas de ganhos

Objetivo de receita ou resultado e dias de trabalho; nenhum valor presumido.

Origem: SITE; IDs: S06. Detalhes e comando em telas/17-metas/.

### 18 — Contas fixas

Recorrência, vencimento, lembretes e baixa; evitar duplicações.

Origem: SITE; IDs: S07. Detalhes e comando em telas/18-contas/.

### 19 — Reserva de manutenção

Separar provisão de transferência e gasto efetivo para evitar dupla dedução.

Origem: SITE; IDs: S08. Detalhes e comando em telas/19-reserva/.

### 20 — Categorias e plataformas

Categorias de receita/despesa e plataformas personalizadas, com edição e arquivamento.

Origem: SITE; IDs: S09. Detalhes e comando em telas/20-categorias/.

### 21 — Custo operacional

Assistente para componentes de custo por km; modelo financeiro validável.

Origem: VÍDEO + SITE; IDs: F08; minuto: 04:35; 11:00. Detalhes e comando em telas/21-custo/.

### 22 — Meus veículos

Cadastro de veículos e seleção de um ativo; próprio, financiado ou alugado.

Origem: SITE; IDs: S10. Detalhes e comando em telas/22-veiculos/.

### 23 — Manutenção

Odômetro, próximo serviço e registros por veículo; agenda configurada pelo usuário.

Origem: SITE; IDs: S11. Detalhes e comando em telas/23-manutencao/.

### 24 — Viagem particular

Orçamento com embarque, percurso, retorno, tempo e pedágios.

Origem: SITE; IDs: S12. Detalhes e comando em telas/24-particular/.

### 25 — Orçamento e recibo

Documento revisável para compartilhar, sem expor custo e margem internos.

Origem: SITE; IDs: S12. Detalhes e comando em telas/25-recibo/.

### 26 — Mensagem preparada

Template com saudação/ETA opcional, prévia e cópia por ação explícita.

Origem: VÍDEO; IDs: F17; minuto: 11:25–11:35; 12:53–13:30. Detalhes e comando em telas/26-mensagem/.

### 27 — Alertas da viagem

Paradas, embarque distante e dado de iniciante somente com fonte e regra confirmadas.

Origem: VÍDEO + SITE; IDs: F21; minuto: 11:00–11:25. Detalhes e comando em telas/27-alertas/.

### 28 — Locais sinalizados

Termos pessoais por embarque/destino/mercados e motivos transparentes.

Origem: VÍDEO; IDs: F19; minuto: 12:00–12:20. Detalhes e comando em telas/28-locais/.

### 29 — Voz e notificações

Métricas faladas selecionáveis, ordem, deduplicação e expiração.

Origem: VÍDEO; IDs: F21,F13; minuto: 11:00–11:25; 13:45. Detalhes e comando em telas/29-voz/.

### 30 — Navegação

Escolher Maps/Waze e abrir endereço conferido; Street View não comprova segurança.

Origem: VÍDEO; IDs: F20,F24; minuto: 13:42–14:15. Detalhes e comando em telas/30-navegacao/.

### 31 — Capturas de tela

Captura opcional, armazenamento/retenção local e revisão; sem contornar telas protegidas.

Origem: VÍDEO; IDs: F18; minuto: 11:45. Detalhes e comando em telas/31-capturas/.

### 32 — Gravação de segurança

Proposta visível e consentida; nunca gravação oculta. Sem câmera real no protótipo.

Origem: VÍDEO / ADAPTADO; IDs: F14; minuto: 09:00–10:05. Detalhes e comando em telas/32-camera/.

### 33 — Biblioteca de gravações

Lista, reprodução, exclusão e compartilhamento voluntário de mídia local.

Origem: VÍDEO; IDs: F14; minuto: 09:30–10:05. Detalhes e comando em telas/33-gravacoes/.

### 34 — Permissões e funcionamento

Explicação por finalidade, efeito de negativa e revogação.

Origem: VÍDEO; IDs: F02; minuto: 02:00–02:20; 06:45–07:15. Detalhes e comando em telas/34-permissoes/.

### 35 — Aparência e preferências

Claro/escuro, tamanho, lembretes, fuso e fechamento diário configurável.

Origem: VÍDEO + SITE; IDs: F22,S13; minuto: 02:30; 15:15. Detalhes e comando em telas/35-preferencias/.

### 36 — Conta e acesso

Perfil do produto e acesso comercial a definir; sem copiar preços ou trial.

Origem: VÍDEO; IDs: F01; minuto: 01:30–02:30. Detalhes e comando em telas/36-conta/.

### 37 — Dispositivos

Visão de aparelhos; limite comercial e sincronização ainda não definidos.

Origem: VÍDEO; IDs: F23; minuto: 14:27–15:45. Detalhes e comando em telas/37-dispositivos/.

### 38 — Backup e restauração

Exportar localmente ou sincronizar futuramente com consentimento e revisão de conflitos.

Origem: SITE; IDs: S14. Detalhes e comando em telas/38-backup/.

### 39 — Privacidade e dados

Dados mínimos, retenção, exclusão e consentimento por recurso.

Origem: PROPOSTA; IDs: F02,F14,F18. Detalhes e comando em telas/39-privacidade/.

### 40 — Ajuda e primeiros passos

Tutorial por recurso, diagnóstico e distinção entre prévia e integração.

Origem: VÍDEO + SITE; IDs: F01,F02; minuto: 01:30–02:20. Detalhes e comando em telas/40-ajuda/.

### 41 — Ferramentas

Catálogo organizado com todos os recursos de operação, finanças e assistência.

Origem: PROPOSTA; IDs: F01–F24,S01–S14. Detalhes e comando em telas/41-ferramentas/.

### 42 — Balão verde

Estado visual de corrida boa, com três métricas e explicação; somente prévia.

Origem: VÍDEO; IDs: F09,F13; minuto: 08:23–08:32. Detalhes e comando em telas/42-balao-boa/.

### 43 — Balão amarelo

Estado visual de atenção, distinto da avaliação agregada real ainda não configurada.

Origem: VÍDEO; IDs: F09,F13; minuto: 08:00. Detalhes e comando em telas/43-balao-atencao/.

### 44 — Balão vermelho

Aviso Recusar quando regras ativas determinarem estado abaixo do mínimo; sem recusa automática.

Origem: VÍDEO; IDs: F09,F13; minuto: 08:00–08:20. Detalhes e comando em telas/44-balao-ruim/.

### 45 — Menu do botão flutuante

Menu aberto do copiloto sobre cenário fictício, com atalhos e desligar/ocultar.

Origem: VÍDEO + PROPOSTA; IDs: F12; minuto: 07:15–07:35. Detalhes e comando em telas/45-menu-flutuante/.

### 46 — Cadastrar preferência

Definir mínimo e patamar bom de um critério, ativação opcional e base do cálculo.

Origem: VÍDEO + PROPOSTA; IDs: F04,F05,F06,F07; minuto: 03:30–04:35. Detalhes e comando em telas/46-editar-criterio/.

### 47 — Combinar critérios

Escolher explicitamente tratamento de critérios conflitantes, dados ausentes e prioridades.

Origem: PROPOSTA; IDs: F04,F05,F06,F07. Detalhes e comando em telas/47-combinacao/.

### 48 — Ordem dos campos

Selecionar linha superior/inferior e reordenar métricas do cartão com prévia.

Origem: VÍDEO; IDs: F11; minuto: 06:13–06:35. Detalhes e comando em telas/48-ordem-campos/.

### 49 — Cartão secundário

Prévia das métricas compactas e aviso independente, mantendo legibilidade e segurança.

Origem: VÍDEO; IDs: F09; minuto: 05:00–05:40. Detalhes e comando em telas/49-cartao-secundario/.

### 50 — Aparência escura

Variação escura da tela operacional, preservando cores funcionais e contraste.

Origem: VÍDEO + PROPOSTA; IDs: F22; minuto: 15:15. Detalhes e comando em telas/50-aparencia-escura/.
