package br.app.motorista.ui.nav

/** Catálogo das 50 telas documentadas em telas/ (gerado de telas.json). */
data class Screen(val id: String, val title: String, val group: String, val description: String)

object ScreenCatalog {
    val all: List<Screen> = listOf(
    Screen("painel", "Painel", "Operação", "Visão geral da jornada e acesso aos recursos essenciais."),
    Screen("analise", "Análise de corrida", "Operação", "Oferta demonstrativa com métricas e motivos, sem ação automática."),
    Screen("criterios", "Critérios de análise", "Operação", "Faixas por km, hora e nota, mínimos, limites de tempo/distância e prioridades."),
    Screen("perfis", "Perfis de trabalho", "Operação", "Agrupar configurações, criar, duplicar e escolher perfil ativo."),
    Screen("plataformas", "Aplicativos de corrida", "Operação", "Escolher apps alvo e distinguir seleção de integração disponível."),
    Screen("cartao", "Estilo do cartão", "Operação", "Prévia completa/compacta, campos, ordem, fontes, tempo e opacidade."),
    Screen("sobreposicao", "Posição segura", "Operação", "Simulação de área livre e exclusões; esconder se contexto for desconhecido."),
    Screen("atalhos", "Controle flutuante", "Operação", "Menu de liga/desliga e atalhos, sem ações em aplicativos de terceiros."),
    Screen("jornada", "Jornada", "Jornada e histórico", "Iniciar, pausar, retomar e encerrar, com fases e tempo online."),
    Screen("relatorios", "Relatórios da jornada", "Jornada e histórico", "Relatórios por período, fase e app; ganho online separado do ganho da oferta."),
    Screen("historico", "Histórico de corridas", "Jornada e histórico", "Filtrar ofertas, confirmar realização e exportar registros revisados."),
    Screen("detalhe", "Detalhe da corrida", "Jornada e histórico", "Métricas, base, regra usada e confirmação manual, sem inferir receita."),
    Screen("horarios", "Melhores horários", "Jornada e histórico", "Ranking por dia/período com amostra explícita e método pendente de aprovação."),
    Screen("financas", "Finanças", "Financeiro", "Receitas confirmadas, despesas, caixa e resultado estimado em bases separadas."),
    Screen("lancamento", "Novo lançamento", "Financeiro", "Receita/despesa com categoria, plataforma, veículo e confirmação."),
    Screen("fechamento", "Fechar o dia", "Financeiro", "Conferir receitas, odômetro, despesas e período antes de concluir."),
    Screen("metas", "Metas de ganhos", "Financeiro", "Objetivo de receita ou resultado e dias de trabalho; nenhum valor presumido."),
    Screen("contas", "Contas fixas", "Financeiro", "Recorrência, vencimento, lembretes e baixa; evitar duplicações."),
    Screen("reserva", "Reserva de manutenção", "Financeiro", "Separar provisão de transferência e gasto efetivo para evitar dupla dedução."),
    Screen("categorias", "Categorias e plataformas", "Financeiro", "Categorias de receita/despesa e plataformas personalizadas, com edição e arquivamento."),
    Screen("custo", "Custo operacional", "Veículo", "Assistente para componentes de custo por km; modelo financeiro validável."),
    Screen("veiculos", "Meus veículos", "Veículo", "Cadastro de veículos e seleção de um ativo; próprio, financiado ou alugado."),
    Screen("manutencao", "Manutenção", "Veículo", "Odômetro, próximo serviço e registros por veículo; agenda configurada pelo usuário."),
    Screen("particular", "Viagem particular", "Veículo", "Orçamento com embarque, percurso, retorno, tempo e pedágios."),
    Screen("recibo", "Orçamento e recibo", "Veículo", "Documento revisável para compartilhar, sem expor custo e margem internos."),
    Screen("mensagem", "Mensagem preparada", "Assistência", "Template com saudação/ETA opcional, prévia e cópia por ação explícita."),
    Screen("alertas", "Alertas da viagem", "Assistência", "Paradas, embarque distante e dado de iniciante somente com fonte e regra confirmadas."),
    Screen("locais", "Locais sinalizados", "Assistência", "Termos pessoais por embarque/destino/mercados e motivos transparentes."),
    Screen("voz", "Voz e notificações", "Assistência", "Métricas faladas selecionáveis, ordem, deduplicação e expiração."),
    Screen("navegacao", "Navegação", "Assistência", "Escolher Maps/Waze e abrir endereço conferido; Street View não comprova segurança."),
    Screen("capturas", "Capturas de tela", "Segurança e registros", "Captura opcional, armazenamento/retenção local e revisão; sem contornar telas protegidas."),
    Screen("camera", "Gravação de segurança", "Segurança e registros", "Proposta visível e consentida; nunca gravação oculta. Sem câmera real no protótipo."),
    Screen("gravacoes", "Biblioteca de gravações", "Segurança e registros", "Lista, reprodução, exclusão e compartilhamento voluntário de mídia local."),
    Screen("permissoes", "Permissões e funcionamento", "Configurações", "Explicação por finalidade, efeito de negativa e revogação."),
    Screen("preferencias", "Aparência e preferências", "Configurações", "Claro/escuro, tamanho, lembretes, fuso e fechamento diário configurável."),
    Screen("conta", "Conta e acesso", "Configurações", "Perfil do produto e acesso comercial a definir; sem copiar preços ou trial."),
    Screen("dispositivos", "Dispositivos", "Configurações", "Visão de aparelhos; limite comercial e sincronização ainda não definidos."),
    Screen("backup", "Backup e restauração", "Configurações", "Exportar localmente ou sincronizar futuramente com consentimento e revisão de conflitos."),
    Screen("privacidade", "Privacidade e dados", "Configurações", "Dados mínimos, retenção, exclusão e consentimento por recurso."),
    Screen("ajuda", "Ajuda e primeiros passos", "Configurações", "Tutorial por recurso, diagnóstico e distinção entre prévia e integração."),
    Screen("ferramentas", "Ferramentas", "Operação", "Catálogo organizado com todos os recursos de operação, finanças e assistência."),
    Screen("balao-boa", "Balão verde", "Operação", "Estado visual de corrida boa, com três métricas e explicação; somente prévia."),
    Screen("balao-atencao", "Balão amarelo", "Operação", "Estado visual de atenção, distinto da avaliação agregada real ainda não configurada."),
    Screen("balao-ruim", "Balão vermelho", "Operação", "Aviso Recusar quando regras ativas determinarem estado abaixo do mínimo; sem recusa automática."),
    Screen("menu-flutuante", "Menu do botão flutuante", "Operação", "Menu aberto do copiloto sobre cenário fictício, com atalhos e desligar/ocultar."),
    Screen("editar-criterio", "Cadastrar preferência", "Operação", "Definir mínimo e patamar bom de um critério, ativação opcional e base do cálculo."),
    Screen("combinacao", "Combinar critérios", "Operação", "Escolher explicitamente tratamento de critérios conflitantes, dados ausentes e prioridades."),
    Screen("ordem-campos", "Ordem dos campos", "Operação", "Selecionar linha superior/inferior e reordenar métricas do cartão com prévia."),
    Screen("cartao-secundario", "Cartão secundário", "Operação", "Prévia das métricas compactas e aviso independente, mantendo legibilidade e segurança."),
    Screen("aparencia-escura", "Aparência escura", "Configurações", "Variação escura da tela operacional, preservando cores funcionais e contraste."),
    )

    private val byId = all.associateBy { it.id }

    fun find(id: String): Screen? = byId[id]

    /** Telas com implementação própria. As demais mostram "Ainda não implementada". */
    val implemented: Set<String> = setOf(
        "painel", "criterios", "editar-criterio", "cartao", "balao-boa", "balao-atencao", "balao-ruim", "combinacao", "atalhos", "camera",
        "jornada", "menu-flutuante", "financas", "lancamento", "categorias", "preferencias", "aparencia-escura", "permissoes", "conta",
        "dispositivos", "backup", "privacidade", "ferramentas", "veiculos", "manutencao", "custo", "contas", "reserva", "metas", "fechamento",
        "perfis", "plataformas", "ordem-campos", "cartao-secundario", "sobreposicao", "historico", "detalhe", "relatorios", "horarios",
    )

    /** Funções que dependem de decisão ou validação externa. Não bloqueiam as demais. */
    private val blocked: Map<String, String> = mapOf(
        "capturas" to "Captura de tela exige MediaProjection e decisão sobre termos das plataformas",
    )

    fun blockedReason(id: String): String? = blocked[id]

    fun status(id: String): ScreenStatus = when {
        id in blocked -> ScreenStatus.Blocked
        id in implemented -> ScreenStatus.Ready
        else -> ScreenStatus.InProgress
    }
}

enum class ScreenStatus(val label: String) {
    Ready("Pronta"),
    Blocked("Bloqueada"),
    InProgress("Em desenvolvimento"),
}
