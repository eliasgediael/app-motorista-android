package br.app.motorista.domain.card

/** Estado visual de PRÉVIA do balão. Não é avaliação: nenhuma regra confirmada decide isto. */
enum class PreviewState(val label: String, val seal: String) {
    Good("Boa", "ACEITAR"),
    Attention("Atenção", "ATENÇÃO"),
    Bad("Ruim", "RECUSAR"),
}

enum class CardLayout(val label: String) {
    Full("Completo"),
    Secondary("Secundário"),
}

enum class CardFontSize(val label: String, val scale: Float) {
    XS("PP", 0.8f),
    S("P", 0.9f),
    M("M", 1.0f),
    L("G", 1.15f),
}

enum class CardField(val label: String) {
    TotalTime("Tempo total"),
    TotalDistance("Distância total"),
    PassengerRating("Nota do passageiro"),
    RevenuePerKm("R$/km"),
    RevenuePerHour("R$/hora"),
    RevenuePerMinute("R$/minuto"),
    OfferValue("Valor da oferta"),
    EstimatedResult("Resultado estimado"),
}

/**
 * Preferências de aparência do cartão. Os padrões copiam a organização visual da
 * referência (telas/06-cartao) e são apenas padrão de exibição, não regra de avaliação.
 */
data class CardStyle(
    val layout: CardLayout = CardLayout.Full,
    val highlightSeal: Boolean = true,
    val showReason: Boolean = true,
    val fontSize: CardFontSize = CardFontSize.M,
    val opacity: Float = 0.9f,
    val enabledFields: Set<CardField> = setOf(
        CardField.TotalTime,
        CardField.TotalDistance,
        CardField.PassengerRating,
        CardField.RevenuePerKm,
        CardField.RevenuePerHour,
        CardField.RevenuePerMinute,
    ),
) {
    fun toggled(field: CardField, on: Boolean): CardStyle =
        copy(enabledFields = if (on) enabledFields + field else enabledFields - field)
}

/**
 * Oferta de EXEMPLO usada só nas prévias (valores fictícios de ESPECIFICACAO-COMPLETA.md).
 * Não representa preferência, regra nem oferta real.
 */
object SampleOffer {
    const val FARE = "24,80"
    const val MINUTES = "22 min"
    const val DISTANCE = "12,4 km"
    const val RATING = "4,92"
    const val PER_KM = "2,00"
    const val PER_HOUR = "67,64"
    const val PER_MINUTE = "1,13"
}
