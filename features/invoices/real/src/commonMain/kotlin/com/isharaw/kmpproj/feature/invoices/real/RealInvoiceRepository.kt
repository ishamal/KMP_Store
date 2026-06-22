package com.isharaw.kmpproj.feature.invoices.real

import com.isharaw.kmpproj.core.AppScope
import com.isharaw.kmpproj.feature.invoices.Invoice
import com.isharaw.kmpproj.feature.invoices.InvoiceRepository
import com.isharaw.kmpproj.feature.invoices.InvoiceStatus
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@Inject
@ContributesBinding(AppScope::class)
class RealInvoiceRepository : InvoiceRepository {
    override fun all(): List<Invoice> = listOf(
        Invoice("INV-1001", "2026-06-01", 250.00, InvoiceStatus.PAID),
        Invoice("INV-1002", "2026-06-08", 120.75, InvoiceStatus.PENDING),
        Invoice("INV-1003", "2026-06-15", 540.10, InvoiceStatus.OVERDUE),
    )
}
