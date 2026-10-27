# Andamento

Atualizado em 27/10/2026. Histórico completo no git.

## Resumo honesto

- Telas documentadas: 50. Implementadas: 49. Bloqueadas por decisão/validação externa: 1 (capturas).
- Código implementado e COMPILADO no GitHub Actions; testes unitários (JVM) executados no CI. Nenhum teste foi executado em aparelho real.
- NÃO existe leitura real de ofertas do 99Pop nem da Uber. Ofertas vêm da entrada manual. Esta é a maior pendência funcional e depende de decisão do usuário (ver abaixo).
- Serviços nativos (botão flutuante, gravação com câmera, voz) estão implementados mas NÃO foram vistos funcionando em aparelho.

## Decisão pendente do usuário

Leitura de ofertas de outro app exige serviço de acessibilidade. A Política de Programas Proibidos da 99 permite monitorar o aparelho e bloquear/cancelar a conta. A lista de programas proibidos não está acessível. Uber: termos não consultados. A leitura não foi implementada. Sem amostras reais de tela também não há como escrever o interpretador. Todo o restante é independente.

## Lista de conclusão por função

| Tela | Situação | O que é real | Limitação / dependência |
|---|---|---|---|
| Painel (`painel`) | Pronta | Cartões do Painel ligados ao estado real do copiloto, análise (leitor), gravação, voz e jornada | Análise só interpreta ofertas quando houver interpretador validado |
| Análise de corrida (`analise`) | Pronta (simulação) | Cálculos reais sobre oferta DIGITADA | Sem leitura real de oferta |
| Critérios de análise (`criterios`) | Pronta | Critérios digitados pelo usuário, persistidos | Valor mínimo/lucro/porcentagem de lucro: especificação pendente |
| Perfis de trabalho (`perfis`) | Pronta | Salvar, ativar, duplicar e excluir perfis (critérios, política, cartão) |  |
| Aplicativos de corrida (`plataformas`) | Pronta | Seleção de apps persistida | Leitura de ofertas não implementada (decisão pendente) |
| Estilo do cartão (`cartao`) | Pronta | Estilo do cartão persistido; mesma renderização no botão flutuante |  |
| Posição segura (`sobreposicao`) | Pronta (simulação) | Política geométrica testada | Sem fonte real dos controles de 99Pop/Uber: cartão oculto até validar |
| Controle flutuante (`atalhos`) | Pronta | Liga/desliga o copiloto, itens do menu, opacidade do botão | Testar em aparelho |
| Jornada (`jornada`) | Pronta | Iniciar, pausar, retomar, encerrar; histórico de jornadas | Fases manuais não existem |
| Relatórios da jornada (`relatorios`) | Pronta | Período, receita, tempo online, ganho/hora online | Sem fases |
| Histórico de corridas (`historico`) | Pronta | Filtros, exportar CSV revisável | Hoje só ofertas manuais |
| Detalhe da corrida (`detalhe`) | Pronta | Métricas, regra da época, confirmação manual -> receita única |  |
| Melhores horários (`horarios`) | Pronta | Contagem e receita por dia da semana e hora | Método de ranking e amostra mínima pendentes |
| Finanças (`financas`) | Pronta | Receitas confirmadas, despesas, caixa por mês | Resultado estimado exige modelo de custo |
| Novo lançamento (`lancamento`) | Pronta | Receita/despesa com confirmação |  |
| Fechar o dia (`fechamento`) | Pronta | Fechar o dia com totais congelados e odômetro |  |
| Metas de ganhos (`metas`) | Pronta | Meta mensal e necessário por dia restante | Valores só do usuário |
| Contas fixas (`contas`) | Pronta | Contas fixas, baixa única por mês | Lembrete por notificação não implementado |
| Reserva de manutenção (`reserva`) | Pronta | Provisão, transferência e gasto separados |  |
| Categorias e plataformas (`categorias`) | Pronta | Categorias e plataformas editáveis, arquivar |  |
| Custo operacional (`custo`) | Pronta | Custo por km a partir de componentes do usuário; resultado estimado no balão | Sem valores sugeridos |
| Meus veículos (`veiculos`) | Pronta | Cadastro, veículo ativo, odômetro |  |
| Manutenção (`manutencao`) | Pronta | Agenda do usuário, registros geram despesa única |  |
| Viagem particular (`particular`) | Pronta | Orçamento com preços do usuário; custo/margem internos |  |
| Orçamento e recibo (`recibo`) | Pronta | Recibo revisável, compartilhamento voluntário, sem custo/margem |  |
| Mensagem preparada (`mensagem`) | Pronta | Monta e copia; nunca envia |  |
| Alertas da viagem (`alertas`) | Pronta (parcial) | Embarque distante com limite do usuário | Paradas e dado de iniciante: não implementados (sem fonte) |
| Locais sinalizados (`locais`) | Pronta | Termos do usuário e correspondência | Não prova segurança |
| Voz e notificações (`voz`) | Pronta | TTS real, itens e ordem, cancela pendentes | Notificações de análise: sem fonte real |
| Navegação (`navegacao`) | Pronta | Abre Maps/Waze com endereço digitado, por ação do usuário | Testar em aparelho |
| Capturas de tela (`capturas`) | Bloqueada |  | Captura de tela exige MediaProjection e decisão sobre termos das plataformas |
| Gravação de segurança (`camera`) | Pronta | Gravação visível real (CameraX, serviço em primeiro plano, sem áudio) | NÃO testada em aparelho |
| Biblioteca de gravações (`gravacoes`) | Pronta | Lista, reprodução, compartilhamento voluntário, exclusão | NÃO testada em aparelho |
| Permissões e funcionamento (`permissoes`) | Pronta | Estado real das permissões, finalidade, efeito de negar |  |
| Aparência e preferências (`preferencias`) | Pronta | Tema persistido | Lembretes e fechamento configurável não implementados |
| Conta e acesso (`conta`) | Pronta (local) | Perfil local | Acesso comercial indefinido |
| Dispositivos (`dispositivos`) | Pronta (local) | Informações deste aparelho | Sincronização e limite comercial não implementados |
| Backup e restauração (`backup`) | Pronta | Exportar/importar JSON validado | Sem sincronização; sem gravações |
| Privacidade e dados (`privacidade`) | Pronta | O que é guardado, apagar dados e gravações |  |
| Ajuda e primeiros passos (`ajuda`) | Pronta | Tutorial e estado real das funções |  |
| Ferramentas (`ferramentas`) | Pronta | Catálogo com estado real |  |
| Balão verde (`balao-boa`) | Pronta (prévia) | Mesmo componente do balão real | Prévia não é avaliação real |
| Balão amarelo (`balao-atencao`) | Pronta (prévia) | Mesmo componente do balão real | Prévia não é avaliação real |
| Balão vermelho (`balao-ruim`) | Pronta (prévia) | Mesmo componente do balão real | Prévia não é avaliação real |
| Menu do botão flutuante (`menu-flutuante`) | Pronta | Mesmos controles e estado do menu flutuante | Testar em aparelho |
| Cadastrar preferência (`editar-criterio`) | Pronta | Cadastro com validação e revisão |  |
| Combinar critérios (`combinacao`) | Pronta | Três escolhas do usuário; nada vem escolhido |  |
| Ordem dos campos (`ordem-campos`) | Pronta | Ordem e ativação dos campos |  |
| Cartão secundário (`cartao-secundario`) | Pronta | Campos do layout compacto |  |
| Aparência escura (`aparencia-escura`) | Pronta | Tema escuro persistido | Contraste a validar em aparelho |

## Verificações

- Testes unitários na JVM (CI): cálculo com bases, avaliação por faixa e política, validação de critérios, plataformas sem mistura e validade de oferta, máquina de estados dos recursos, jornada, geometria do botão/menu, posição segura, finanças, contas, reserva, metas, custo por km, manutenção, histórico, relatórios, recibo, mensagem, alertas, locais, voz, persistência (repositório JSON e preferências), ordem de campos.
- Testes Compose na JVM (Robolectric): comportamento do balão (selo, motivo, campos). Robolectric NÃO mede texto de verdade.
- Teste de transbordo/corte de fonte em EMULADOR (job `ui-tests`): vem encontrando problemas reais de layout do balão; correções em andamento. Esse job pode estar falhando; o APK é publicado pelo job `build`.
- Revisão estática por leitura. Nada disso substitui teste no aparelho.

## Limitações gerais

- minSdk 26, compileSdk 37 (exigido pelas dependências), targetSdk 36.
- Dados locais em JSON no armazenamento interno; sem nuvem, sem telemetria, sem dados de passageiros.
- Prazo de validade da oferta (15 s) e limite de fonte do balão (1,3x do sistema) são provisórios.
- Aceitar/recusar corridas nunca é automatizado. O selo ACEITAR/ATENÇÃO/RECUSAR é só informativo.
