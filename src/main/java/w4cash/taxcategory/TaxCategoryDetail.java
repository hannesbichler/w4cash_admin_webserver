package w4cash.taxcategory;

import java.util.List;

public record TaxCategoryDetail(String id, String name, List<TaxRateRef> rates) {
}
