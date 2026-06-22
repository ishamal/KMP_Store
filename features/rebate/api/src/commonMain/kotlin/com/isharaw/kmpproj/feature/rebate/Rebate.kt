package com.isharaw.kmpproj.feature.rebate

data class Rebate(val label: String, val amount: Double)

/** Public rebate contract. Implemented by `RealRebateRepository` in the `:real` module. */
interface RebateRepository {
    fun available(): List<Rebate>
    fun total(): Double
}
