package br.app.motorista.data

import br.app.motorista.domain.finance.Category
import br.app.motorista.domain.finance.CategoryKind
import br.app.motorista.domain.finance.EntryKind
import br.app.motorista.domain.finance.Transaction
import br.app.motorista.domain.finance.TxSource
import org.json.JSONObject

internal fun JSONObject.optStringOrNull(key: String): String? = if (has(key) && !isNull(key)) getString(key) else null

object CategoryCodec : Codec<Category> {
    override fun toJson(item: Category) = JSONObject()
        .put("id", item.id).put("name", item.name).put("kind", item.kind.name).put("archived", item.archived)

    override fun fromJson(o: JSONObject) = Category(
        id = o.getString("id"),
        name = o.getString("name"),
        kind = CategoryKind.valueOf(o.getString("kind")),
        archived = o.optBoolean("archived", false),
    )

    override fun idOf(item: Category) = item.id
}

object TransactionCodec : Codec<Transaction> {
    override fun toJson(item: Transaction) = JSONObject()
        .put("id", item.id)
        .put("kind", item.kind.name)
        .put("amountCents", item.amountCents)
        .put("epochDay", item.epochDay)
        .put("categoryId", item.categoryId ?: JSONObject.NULL)
        .put("platformId", item.platformId ?: JSONObject.NULL)
        .put("vehicleId", item.vehicleId ?: JSONObject.NULL)
        .put("note", item.note)
        .put("source", item.source.name)
        .put("sourceRef", item.sourceRef ?: JSONObject.NULL)

    override fun fromJson(o: JSONObject) = Transaction(
        id = o.getString("id"),
        kind = EntryKind.valueOf(o.getString("kind")),
        amountCents = o.getLong("amountCents"),
        epochDay = o.getLong("epochDay"),
        categoryId = o.optStringOrNull("categoryId"),
        platformId = o.optStringOrNull("platformId"),
        vehicleId = o.optStringOrNull("vehicleId"),
        note = o.optString("note", ""),
        source = TxSource.valueOf(o.optString("source", "Manual")),
        sourceRef = o.optStringOrNull("sourceRef"),
    )

    override fun idOf(item: Transaction) = item.id
}
