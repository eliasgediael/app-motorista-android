package br.app.motorista.data

import br.app.motorista.domain.finance.Bill
import br.app.motorista.domain.finance.BillPayment
import br.app.motorista.domain.finance.DayClose
import br.app.motorista.domain.finance.Goal
import br.app.motorista.domain.finance.GoalKind
import br.app.motorista.domain.finance.Recurrence
import br.app.motorista.domain.finance.ReserveEntry
import br.app.motorista.domain.finance.ReserveKind
import br.app.motorista.domain.finance.Setting
import br.app.motorista.domain.vehicle.CostComponent
import br.app.motorista.domain.vehicle.CostKind
import br.app.motorista.domain.vehicle.MaintenanceItem
import br.app.motorista.domain.vehicle.MaintenanceRecord
import br.app.motorista.domain.vehicle.Ownership
import br.app.motorista.domain.vehicle.Vehicle
import java.math.BigDecimal
import org.json.JSONObject

private fun JSONObject.longOrNull(key: String): Long? = if (has(key) && !isNull(key)) getLong(key) else null
private fun JSONObject.decimalOrNull(key: String): BigDecimal? = if (has(key) && !isNull(key)) BigDecimal(getString(key)) else null
private fun nul(v: Any?): Any = v ?: JSONObject.NULL

object VehicleCodec : Codec<Vehicle> {
    override fun toJson(item: Vehicle) = JSONObject().put("id", item.id).put("name", item.name).put("ownership", item.ownership.name)
        .put("odometerKm", nul(item.odometerKm)).put("archived", item.archived)

    override fun fromJson(o: JSONObject) = Vehicle(
        o.getString("id"), o.getString("name"), Ownership.valueOf(o.getString("ownership")),
        o.longOrNull("odometerKm"), o.optBoolean("archived", false),
    )

    override fun idOf(item: Vehicle) = item.id
}

object MaintenanceItemCodec : Codec<MaintenanceItem> {
    override fun toJson(item: MaintenanceItem) = JSONObject().put("id", item.id).put("vehicleId", item.vehicleId).put("name", item.name)
        .put("intervalKm", nul(item.intervalKm)).put("lastDoneKm", nul(item.lastDoneKm)).put("lastDoneEpochDay", nul(item.lastDoneEpochDay))

    override fun fromJson(o: JSONObject) = MaintenanceItem(
        o.getString("id"), o.getString("vehicleId"), o.getString("name"),
        o.longOrNull("intervalKm"), o.longOrNull("lastDoneKm"), o.longOrNull("lastDoneEpochDay"),
    )

    override fun idOf(item: MaintenanceItem) = item.id
}

object MaintenanceRecordCodec : Codec<MaintenanceRecord> {
    override fun toJson(item: MaintenanceRecord) = JSONObject().put("id", item.id).put("vehicleId", item.vehicleId)
        .put("itemId", nul(item.itemId)).put("name", item.name).put("km", nul(item.km)).put("epochDay", item.epochDay)
        .put("costCents", nul(item.costCents))

    override fun fromJson(o: JSONObject) = MaintenanceRecord(
        o.getString("id"), o.getString("vehicleId"), o.optStringOrNull("itemId"), o.getString("name"),
        o.longOrNull("km"), o.getLong("epochDay"), o.longOrNull("costCents"),
    )

    override fun idOf(item: MaintenanceRecord) = item.id
}

object CostComponentCodec : Codec<CostComponent> {
    override fun toJson(item: CostComponent) = JSONObject().put("id", item.id).put("name", item.name).put("kind", item.kind.name)
        .put("a", item.a?.toPlainString() ?: JSONObject.NULL).put("b", item.b?.toPlainString() ?: JSONObject.NULL).put("enabled", item.enabled)

    override fun fromJson(o: JSONObject) = CostComponent(
        o.getString("id"), o.getString("name"), CostKind.valueOf(o.getString("kind")),
        o.decimalOrNull("a"), o.decimalOrNull("b"), o.optBoolean("enabled", true),
    )

    override fun idOf(item: CostComponent) = item.id
}

object BillCodec : Codec<Bill> {
    override fun toJson(item: Bill) = JSONObject().put("id", item.id).put("name", item.name).put("amountCents", item.amountCents)
        .put("dueDay", item.dueDay).put("recurrence", item.recurrence.name).put("onceEpochDay", nul(item.onceEpochDay))
        .put("categoryId", nul(item.categoryId)).put("active", item.active)

    override fun fromJson(o: JSONObject) = Bill(
        o.getString("id"), o.getString("name"), o.getLong("amountCents"), o.getInt("dueDay"),
        Recurrence.valueOf(o.getString("recurrence")), o.longOrNull("onceEpochDay"), o.optStringOrNull("categoryId"), o.optBoolean("active", true),
    )

    override fun idOf(item: Bill) = item.id
}

object BillPaymentCodec : Codec<BillPayment> {
    override fun toJson(item: BillPayment) = JSONObject().put("id", item.id).put("billId", item.billId).put("yearMonth", item.yearMonth)
        .put("txId", item.txId).put("epochDay", item.epochDay)

    override fun fromJson(o: JSONObject) = BillPayment(o.getString("id"), o.getString("billId"), o.getString("yearMonth"), o.getString("txId"), o.getLong("epochDay"))

    override fun idOf(item: BillPayment) = item.id
}

object ReserveCodec : Codec<ReserveEntry> {
    override fun toJson(item: ReserveEntry) = JSONObject().put("id", item.id).put("kind", item.kind.name).put("amountCents", item.amountCents)
        .put("epochDay", item.epochDay).put("note", item.note).put("txId", nul(item.txId))

    override fun fromJson(o: JSONObject) = ReserveEntry(
        o.getString("id"), ReserveKind.valueOf(o.getString("kind")), o.getLong("amountCents"), o.getLong("epochDay"),
        o.optString("note", ""), o.optStringOrNull("txId"),
    )

    override fun idOf(item: ReserveEntry) = item.id
}

object GoalCodec : Codec<Goal> {
    override fun toJson(item: Goal) = JSONObject().put("id", item.id).put("kind", item.kind.name).put("targetCents", item.targetCents).put("workDays", item.workDays)

    override fun fromJson(o: JSONObject) = Goal(o.getString("id"), GoalKind.valueOf(o.getString("kind")), o.getLong("targetCents"), o.getInt("workDays"))

    override fun idOf(item: Goal) = item.id
}

object DayCloseCodec : Codec<DayClose> {
    override fun toJson(item: DayClose) = JSONObject().put("id", item.id).put("epochDay", item.epochDay).put("odometerKm", nul(item.odometerKm))
        .put("vehicleId", nul(item.vehicleId)).put("incomeCents", item.incomeCents).put("expenseCents", item.expenseCents)
        .put("note", item.note).put("closedAtMs", item.closedAtMs)

    override fun fromJson(o: JSONObject) = DayClose(
        o.getString("id"), o.getLong("epochDay"), o.longOrNull("odometerKm"), o.optStringOrNull("vehicleId"),
        o.getLong("incomeCents"), o.getLong("expenseCents"), o.optString("note", ""), o.optLong("closedAtMs", 0),
    )

    override fun idOf(item: DayClose) = item.id
}

object SettingCodec : Codec<Setting> {
    override fun toJson(item: Setting) = JSONObject().put("id", item.id).put("value", item.value)

    override fun fromJson(o: JSONObject) = Setting(o.getString("id"), o.getString("value"))

    override fun idOf(item: Setting) = item.id
}
