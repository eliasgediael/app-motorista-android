package br.app.motorista.data

import br.app.motorista.domain.calc.OfferInput
import br.app.motorista.domain.history.JourneyRecord
import br.app.motorista.domain.history.Profile
import br.app.motorista.domain.history.TripRecord
import br.app.motorista.domain.history.TripStatus
import java.math.BigDecimal
import org.json.JSONObject

private fun JSONObject.longOrNull(key: String): Long? = if (has(key) && !isNull(key)) getLong(key) else null
private fun nul(v: Any?): Any = v ?: JSONObject.NULL

object TripCodec : Codec<TripRecord> {
    override fun toJson(item: TripRecord): JSONObject {
        val i = item.input
        return JSONObject().put("id", item.id).put("atMs", item.atMs).put("epochDay", item.epochDay).put("platform", item.platform)
            .put("origin", item.origin)
            .put("fareCents", nul(i.fareCents)).put("pickupMeters", nul(i.pickupMeters)).put("tripMeters", nul(i.tripMeters))
            .put("returnMeters", nul(i.returnMeters)).put("pickupSeconds", nul(i.pickupSeconds)).put("tripSeconds", nul(i.tripSeconds))
            .put("returnSeconds", nul(i.returnSeconds)).put("rating", i.passengerRating?.toPlainString() ?: JSONObject.NULL)
            .put("metricsText", item.metricsText).put("bandName", nul(item.bandName)).put("note", item.note)
            .put("ruleVersionId", item.ruleVersionId).put("ruleSummary", item.ruleSummary)
            .put("status", item.status.name).put("txId", nul(item.txId))
    }

    override fun fromJson(o: JSONObject) = TripRecord(
        id = o.getString("id"), atMs = o.getLong("atMs"), epochDay = o.getLong("epochDay"),
        platform = o.getString("platform"), origin = o.getString("origin"),
        input = OfferInput(
            o.longOrNull("fareCents"), o.longOrNull("pickupMeters"), o.longOrNull("tripMeters"), o.longOrNull("returnMeters"),
            o.longOrNull("pickupSeconds"), o.longOrNull("tripSeconds"), o.longOrNull("returnSeconds"),
            if (o.has("rating") && !o.isNull("rating")) BigDecimal(o.getString("rating")) else null,
        ),
        metricsText = o.optString("metricsText", ""), bandName = o.optStringOrNull("bandName"), note = o.optString("note", ""),
        ruleVersionId = o.optString("ruleVersionId", ""), ruleSummary = o.optString("ruleSummary", ""),
        status = TripStatus.valueOf(o.optString("status", "NotConfirmed")), txId = o.optStringOrNull("txId"),
    )

    override fun idOf(item: TripRecord) = item.id
}

object JourneyRecordCodec : Codec<JourneyRecord> {
    override fun toJson(item: JourneyRecord) = JSONObject().put("id", item.id).put("startedAtMs", item.startedAtMs).put("endedAtMs", item.endedAtMs)
        .put("workedMs", item.workedMs).put("pausedMs", item.pausedMs).put("pauseCount", item.pauseCount)

    override fun fromJson(o: JSONObject) = JourneyRecord(
        o.getString("id"), o.getLong("startedAtMs"), o.getLong("endedAtMs"), o.getLong("workedMs"), o.getLong("pausedMs"), o.optInt("pauseCount", 0),
    )

    override fun idOf(item: JourneyRecord) = item.id
}

object ProfileCodec : Codec<Profile> {
    override fun toJson(item: Profile) = JSONObject().put("id", item.id).put("name", item.name).put("json", item.json)

    override fun fromJson(o: JSONObject) = Profile(o.getString("id"), o.getString("name"), o.getString("json"))

    override fun idOf(item: Profile) = item.id
}

object FlaggedTermCodec : Codec<br.app.motorista.domain.assist.FlaggedTerm> {
    override fun toJson(item: br.app.motorista.domain.assist.FlaggedTerm) =
        JSONObject().put("id", item.id).put("text", item.text).put("kind", item.kind.name)

    override fun fromJson(o: JSONObject) = br.app.motorista.domain.assist.FlaggedTerm(
        o.getString("id"), o.getString("text"), br.app.motorista.domain.assist.PlaceKind.valueOf(o.getString("kind")),
    )

    override fun idOf(item: br.app.motorista.domain.assist.FlaggedTerm) = item.id
}
