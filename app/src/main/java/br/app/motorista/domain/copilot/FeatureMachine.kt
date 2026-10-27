package br.app.motorista.domain.copilot

/** Estados verdadeiros de um recurso do copiloto. Nunca ON por otimismo: Active só após confirmação. */
enum class FeatureStatus(val label: String) {
    NotImplemented("Não implementado"),
    Unavailable("Indisponível"),
    PermissionNeeded("Permissão necessária"),
    Off("Desligado"),
    Starting("Iniciando"),
    Active("Ativo"),
    Stopping("Parando"),
    Error("Erro"),
}

/**
 * Máquina de estados de um recurso. Garante: transição só por confirmação do serviço, toque duplicado
 * bloqueado durante Starting/Stopping, falha nunca deixa Active, perda do serviço volta a Off.
 */
class FeatureMachine(initial: FeatureStatus = FeatureStatus.Off) {
    var status: FeatureStatus = initial
        private set

    /** Motivo legível de Error, Unavailable ou PermissionNeeded; ou complemento de Starting. */
    var detail: String? = null
        private set

    /** Pedido de ativação. false se já está iniciando/parando/ativo ou se o recurso não existe. */
    fun beginStart(detail: String? = null): Boolean {
        if (status != FeatureStatus.Off && status != FeatureStatus.Error && status != FeatureStatus.PermissionNeeded) return false
        status = FeatureStatus.Starting
        this.detail = detail
        return true
    }

    /** Confirmação do serviço real. Ignorada se ninguém pediu (evita ON espúrio). */
    fun confirmActive(): Boolean {
        if (status != FeatureStatus.Starting) return false
        status = FeatureStatus.Active
        detail = null
        return true
    }

    /** Falha ou negação. [to] deve ser Error, PermissionNeeded ou Unavailable. */
    fun fail(to: FeatureStatus, detail: String?) {
        require(to == FeatureStatus.Error || to == FeatureStatus.PermissionNeeded || to == FeatureStatus.Unavailable)
        status = to
        this.detail = detail
    }

    /** Pedido de parada. false se não está ativo nem iniciando. Starting -> Off direto (cancela o pedido). */
    fun beginStop(): Boolean = when (status) {
        FeatureStatus.Active -> {
            status = FeatureStatus.Stopping
            true
        }
        FeatureStatus.Starting -> {
            status = FeatureStatus.Off
            detail = null
            true
        }
        else -> false
    }

    fun confirmOff() {
        if (status == FeatureStatus.Stopping || status == FeatureStatus.Active) {
            status = FeatureStatus.Off
            detail = null
        }
    }

    /** Processo/serviço morreu ou recurso foi revogado: nunca retomar sozinho. */
    fun forceOff(detail: String? = null) {
        if (status == FeatureStatus.NotImplemented) return
        status = FeatureStatus.Off
        this.detail = detail
    }

    /** Recurso sem implementação: fica assim para sempre; nada é oferecido para ligar. */
    companion object {
        fun notImplemented(reason: String): FeatureMachine = FeatureMachine(FeatureStatus.NotImplemented).also { it.detail = reason }
    }
}
