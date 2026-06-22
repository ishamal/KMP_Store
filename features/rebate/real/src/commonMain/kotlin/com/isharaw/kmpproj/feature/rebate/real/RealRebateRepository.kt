package com.isharaw.kmpproj.feature.rebate.real

import com.isharaw.kmpproj.core.AppScope
import com.isharaw.kmpproj.feature.rebate.Rebate
import com.isharaw.kmpproj.feature.rebate.RebateRepository
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@Inject
@ContributesBinding(AppScope::class)
class RealRebateRepository : RebateRepository {
    override fun available(): List<Rebate> = listOf(
        Rebate("Welcome rebate", 5.00),
        Rebate("Loyalty rebate", 7.50),
    )

    override fun total(): Double = available().sumOf { it.amount }
}
