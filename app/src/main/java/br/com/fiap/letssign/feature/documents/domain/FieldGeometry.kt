package br.com.fiap.letssign.feature.documents.domain

import br.com.fiap.letssign.core.model.SignatureField

/** Regras geométricas dos campos: tudo em proporção da página, sempre dentro dos limites. */
object FieldGeometry {
    const val DEFAULT_WIDTH = 0.30f
    const val DEFAULT_HEIGHT = 0.08f
    const val MIN_WIDTH = 0.10f
    const val MIN_HEIGHT = 0.04f
    private const val EPSILON = 0.0001f

    /** Limita a posição a 0..1 descontando largura e altura (o campo nunca sai da página). */
    fun clamp(field: SignatureField): SignatureField {
        val w = field.widthRatio.coerceIn(MIN_WIDTH, 1f)
        val h = field.heightRatio.coerceIn(MIN_HEIGHT, 1f)
        return field.copy(
            widthRatio = w,
            heightRatio = h,
            xRatio = field.xRatio.coerceIn(0f, 1f - w),
            yRatio = field.yRatio.coerceIn(0f, 1f - h),
        )
    }

    fun isInsidePage(field: SignatureField): Boolean =
        field.widthRatio > 0f && field.heightRatio > 0f &&
            field.xRatio >= -EPSILON && field.yRatio >= -EPSILON &&
            field.xRatio + field.widthRatio <= 1f + EPSILON &&
            field.yRatio + field.heightRatio <= 1f + EPSILON

    /** Posição de um campo novo: centralizado e deslocado em cascata para não ficar em cima de outro. */
    fun newFieldPosition(existingOnPage: Int): Pair<Float, Float> {
        val step = (existingOnPage % 6) * 0.05f
        val x = (1f - DEFAULT_WIDTH) / 2f + step * 0.4f
        val y = 0.45f + step
        return x.coerceIn(0f, 1f - DEFAULT_WIDTH) to y.coerceIn(0f, 1f - DEFAULT_HEIGHT)
    }
}
