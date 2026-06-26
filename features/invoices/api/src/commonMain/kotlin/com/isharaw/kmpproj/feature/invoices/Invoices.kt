package com.isharaw.kmpproj.feature.invoices

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
