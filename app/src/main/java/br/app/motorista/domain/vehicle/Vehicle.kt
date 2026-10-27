package br.app.motorista.domain.vehicle

import java.math.BigDecimal
import java.math.MathContext

enum class Ownership(val label: String) {
    Own("Próprio"),
    Financed("Financiado"),
    Rented("Alugado"),
}

/** Veículo cadastrado pelo usuário. Odômetro desconhecido é null, nunca zero. */
enum class VehicleType(val label: String) { Car("Carro"), Moto("Moto") }

data class Vehicle(
    val id: String,
    val name: String,
    val ownership: Ownership,
    val type: VehicleType = VehicleType.Car,
    val odometerKm: Long? = null,
    val archived: Boolean = false,
)

/** Item de manutenção configurado pelo usuário (intervalo e última execução). Nada é presumido. */
data class MaintenanceItem(
    val id: String,
    val vehicleId: String,
    val name: String,
    val intervalKm: Long? = null,
    val lastDoneKm: Long? = null,
    val lastDoneEpochDay: Long? = null,
)

data class MaintenanceRecord(
    val id: String,
    val vehicleId: String,
    val itemId: String?,
    val name: String,
    val km: Long?,
    val epochDay: Long,
    val costCents: Long?,
)

sealed interface MaintenanceStatus {
    /** Falta intervalo, última execução ou odômetro: não dá para calcular. */
    data class Unknown(val missing: List<String>) : MaintenanceStatus

    data class Scheduled(val nextDueKm: Long, val remainingKm: Long) : MaintenanceStatus {
        val overdue: Boolean get() = remainingKm <= 0
    }
}

object MaintenanceCalc {
    fun status(item: MaintenanceItem, odometerKm: Long?): MaintenanceStatus {
        val missing = buildList {
            if (item.intervalKm == null) add("intervalo")
            if (item.lastDoneKm == null) add("km da última execução")
            if (odometerKm == null) add("odômetro atual")
        }
        if (missing.isNotEmpty()) return MaintenanceStatus.Unknown(missing)
        val next = item.lastDoneKm!! + item.intervalKm!!
        return MaintenanceStatus.Scheduled(next, next - odometerKm!!)
    }

    /** Odômetro só avança: um valor menor que o atual é recusado (provável erro de digitação). */
    fun acceptOdometer(current: Long?, typed: Long): Boolean = current == null || typed >= current
}

/** Tipos de componente do modelo de custo por km. Todos os valores vêm do usuário. */
enum class CostKind(val label: String, val help: String) {
    Fuel("Combustível", "preço por litro ÷ km por litro"),
    PerKm("Custo direto por km", "valor em R$ por km (ex.: manutenção estimada por você)"),
    Monthly("Custo mensal rateado", "valor mensal ÷ km rodados por mês"),
}

data class CostComponent(
    val id: String,
    val name: String,
    val kind: CostKind,
    /** Fuel: preço por litro. PerKm: R$/km. Monthly: valor mensal. */
    val a: BigDecimal?,
    /** Fuel: km por litro. Monthly: km por mês. PerKm: não usado. */
    val b: BigDecimal?,
    val enabled: Boolean = true,
)

sealed interface CostResult {
    data class Valid(val perKm: BigDecimal, val parts: List<Pair<String, BigDecimal>>) : CostResult
    data class Incomplete(val reasons: List<String>) : CostResult
}

object CostModel {
    private val mc = MathContext.DECIMAL128

    fun componentPerKm(c: CostComponent): BigDecimal? = when (c.kind) {
        CostKind.Fuel -> if (c.a != null && c.b != null && c.b.signum() > 0 && c.a.signum() >= 0) c.a.divide(c.b, mc) else null
        CostKind.PerKm -> c.a?.takeIf { it.signum() >= 0 }
        CostKind.Monthly -> if (c.a != null && c.b != null && c.b.signum() > 0 && c.a.signum() >= 0) c.a.divide(c.b, mc) else null
    }

    /**
     * Custo por km = soma dos componentes ativos. Sem componente ativo, ou com algum incompleto, o resultado
     * é Incomplete: custo ausente não é custo zero.
     */
    fun costPerKm(components: List<CostComponent>): CostResult {
        val active = components.filter { it.enabled }
        if (active.isEmpty()) return CostResult.Incomplete(listOf("nenhum componente de custo ativo"))
        val reasons = mutableListOf<String>()
        val parts = mutableListOf<Pair<String, BigDecimal>>()
        active.forEach { c ->
            val v = componentPerKm(c)
            if (v == null) reasons += c.name + ": valores ausentes ou inválidos (denominador deve ser maior que zero)" else parts += c.name to v
        }
        if (reasons.isNotEmpty()) return CostResult.Incomplete(reasons)
        return CostResult.Valid(parts.fold(BigDecimal.ZERO) { acc, p -> acc + p.second }, parts)
    }
}
