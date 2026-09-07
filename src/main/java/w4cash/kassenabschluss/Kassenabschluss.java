package w4cash.kassenabschluss;

import java.util.List;

public record Kassenabschluss(
        String tabletId, String money, String host, String hostSequence,
        String dateStart, String dateEnd,
        long ticketCount, // PAYMENTS rows excl. 'free' (a split cash+card ticket counts as 2)
        double cashTotal, double cardTotal, double paperTotal, double cashInOutTotal, double freeTotal,
        List<PaymentLine> paymentLines,
        long salesCount, // COUNT(DISTINCT RECEIPTS.ID) - the actual unique-ticket count
        double salesBase, double salesTax, double salesGrossTotal,
        List<TaxBreakdown> taxBreakdown) {

    public record PaymentLine(String payment, String description, double total) {
    }

    public record TaxBreakdown(String category, double taxAmount, double baseAmount) {
    }
}
