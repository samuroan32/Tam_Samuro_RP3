package com.twinscalev3.domain.growth

import com.twinscalev3.data.model.GrowthMode
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import kotlin.random.Random

class GrowthEngine(private val random: Random = Random.Default) {

    private data class ModeProfile(
        val minRand: Double,
        val maxRand: Double,
        val plusFactor: Double,
        val minusFactor: Double,
        val modeMultiplier: Double
    )

    fun grow(current: BigInteger, mode: GrowthMode): BigInteger =
        applyDelta(current, mode, isIncrease = true)

    fun shrink(current: BigInteger, mode: GrowthMode): BigInteger =
        applyDelta(current, mode, isIncrease = false)

    private fun applyDelta(current: BigInteger, mode: GrowthMode, isIncrease: Boolean): BigInteger {
        val safeCurrent = current.max(BigInteger.ONE)
        val profile = profile(mode)

        val digits = safeCurrent.toString().length
        val scale = 1.0 + kotlin.math.log10(digits.toDouble() + 1.0) + (digits * 0.03)
        val randomFactor = random.nextDouble(profile.minRand, profile.maxRand)
        val directionFactor = if (isIncrease) profile.plusFactor else profile.minusFactor
        val fraction = randomFactor * scale * profile.modeMultiplier * directionFactor

        val delta = safeCurrent
            .toBigDecimal(MathContext.DECIMAL128)
            .multiply(BigDecimal.valueOf(fraction), MathContext.DECIMAL128)
            .toBigInteger()
            .max(BigInteger.ONE)

        return if (isIncrease) safeCurrent + delta else (safeCurrent - delta).max(BigInteger.ONE)
    }

    private fun profile(mode: GrowthMode): ModeProfile = when (mode) {
        GrowthMode.GENTLE -> ModeProfile(0.002, 0.02, 1.0, 0.8, 0.9)
        GrowthMode.BALANCED -> ModeProfile(0.01, 0.07, 1.0, 1.0, 1.25)
        GrowthMode.EXTREME -> ModeProfile(0.03, 0.16, 1.15, 1.15, 1.75)
    }
}
