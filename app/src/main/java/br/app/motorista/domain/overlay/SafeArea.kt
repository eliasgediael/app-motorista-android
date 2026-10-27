package br.app.motorista.domain.overlay

sealed interface Placement {
    data class At(val box: IntBox, val compact: Boolean) : Placement

    /** Sem posição segura (ou sem contexto): o cartão não aparece. */
    data class Hidden(val reason: String) : Placement
}

/**
 * Política de posição segura do cartão (telas/07-sobreposicao).
 *
 * Recebe os retângulos proibidos (X de recusa, aceite, controles) e procura uma área livre para o cartão
 * completo, depois para o compacto. Sem lista de exclusões (contexto desconhecido) ou sem espaço, oculta.
 * Esta função é só a política geométrica: HOJE não existe fonte real dos retângulos de outro aplicativo,
 * então nada aqui comprova segurança em 99Pop ou Uber.
 */
object SafeAreaPlanner {
    data class Size(val w: Int, val h: Int)

    fun plan(screen: IntBox, exclusions: List<IntBox>?, full: Size, compact: Size, margin: Int): Placement {
        if (exclusions == null) return Placement.Hidden("contexto desconhecido: sem retângulos de exclusão confiáveis")
        for ((size, isCompact) in listOf(full to false, compact to true)) {
            val box = firstFree(screen, exclusions, size, margin)
            if (box != null) return Placement.At(box, isCompact)
        }
        return Placement.Hidden("sem espaço livre fora das exclusões")
    }

    private fun firstFree(screen: IntBox, exclusions: List<IntBox>, size: Size, margin: Int): IntBox? {
        val xs = listOf(screen.left + (screen.width - size.w) / 2, screen.left, screen.right - size.w)
        val ys = buildList {
            add(screen.top)
            add(screen.bottom - size.h)
            exclusions.forEach {
                add(it.top - margin - size.h)
                add(it.bottom + margin)
            }
        }.distinct().sorted()
        for (y in ys) for (x in xs) {
            val box = IntBox(x, y, x + size.w, y + size.h)
            if (inside(screen, box) && exclusions.none { overlaps(box, grow(it, margin)) }) return box
        }
        return null
    }

    fun grow(b: IntBox, m: Int) = IntBox(b.left - m, b.top - m, b.right + m, b.bottom + m)

    fun inside(outer: IntBox, inner: IntBox) = inner.left >= outer.left && inner.top >= outer.top && inner.right <= outer.right && inner.bottom <= outer.bottom

    fun overlaps(a: IntBox, b: IntBox) = a.left < b.right && a.right > b.left && a.top < b.bottom && a.bottom > b.top
}
