package com.isharaw.kmpproj.feature.invoices

import com.isharaw.kmpproj.core.Capability
import com.isharaw.kmpproj.core.Experience
import com.isharaw.kmpproj.core.has

enum class InvoiceStatus { PAID, PENDING, OVERDUE }

data class Invoice(
    val number: String,
    val date: String,
    val amount: Double,
    val status: InvoiceStatus,
)

/** Public invoices contract. Implemented by `RealInvoiceRepository` in the `:real` module. */
interface InvoiceRepository {
    fun all(): List<Invoice>
}

private val statusCapability: Map<InvoiceStatus, Capability> = mapOf(
    InvoiceStatus.PAID to Capability.VIEW_PAID_INVOICES,
    InvoiceStatus.PENDING to Capability.VIEW_PENDING_INVOICES,
    InvoiceStatus.OVERDUE to Capability.VIEW_OVERDUE_INVOICES,
)

/** Experience-scoped invoice list (no branching on the experience). */
fun InvoiceRepository.invoicesFor(experience: Experience): List<Invoice> =
    all().filter { experience.has(statusCapability.getValue(it.status)) }
