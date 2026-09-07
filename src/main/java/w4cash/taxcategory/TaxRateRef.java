package w4cash.taxcategory;

// validFrom is a plain "yyyy-MM-dd" date string (rate-change effective date);
// time-of-day on the underlying TIMESTAMP column is not exposed here.
public record TaxRateRef(String id, String name, double rate, String validFrom) {
}
