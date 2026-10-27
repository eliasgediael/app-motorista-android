package br.app.motorista.domain.overlay

/** Retângulo em pixels de tela. Independente de Android para poder ser testado na JVM. */
data class IntBox(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

data class IntPoint(val x: Int, val y: Int)

/**
 * Geometria do botão flutuante e do menu, limitada à área útil (tela menos barras do sistema e recorte).
 * Isto garante só que as janelas do PRÓPRIO app ficam dentro da área útil. Não conhece nem protege
 * botões de outros aplicativos (99Pop, Uber): isso depende de contexto e validação em aparelho.
 */
object OverlayGeometry {
    /** Mantém o canto superior esquerdo de um item w x h dentro de [bounds]. Se não couber, fica no canto. */
    fun clamp(x: Int, y: Int, w: Int, h: Int, bounds: IntBox): IntPoint {
        val maxX = maxOf(bounds.left, bounds.right - w)
        val maxY = maxOf(bounds.top, bounds.bottom - h)
        return IntPoint(x.coerceIn(bounds.left, maxX), y.coerceIn(bounds.top, maxY))
    }

    /**
     * Posição do menu junto ao botão: à direita se couber, senão à esquerda, depois limitado à área útil.
     * [menuH] já deve estar limitado a bounds.height pelo chamador (menu rolável).
     */
    fun placeMenu(button: IntPoint, buttonSize: Int, gap: Int, menuW: Int, menuH: Int, bounds: IntBox): IntPoint {
        val right = button.x + buttonSize + gap
        val x = if (right + menuW <= bounds.right) right else button.x - gap - menuW
        return clamp(x, button.y, menuW, menuH, bounds)
    }
}
