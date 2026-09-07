package w4cash.product;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Product {

	private String id;
	private String reference;
	private String code;
	private String name;
	private double priceBuy;
	private double priceSell;
	private String taxCatId;
	private String categoryId;
	private String unit;
	private String attributeSetId;
	private boolean active;

	public Product() {
	}

	public Product(String id, String reference, String code, String name, double priceBuy, double priceSell,
			String taxCatId, String categoryId, String unit, String attributeSetId) {
		this.id = id;
		this.reference = reference;
		this.code = code;
		this.name = name;
		this.priceBuy = priceBuy;
		this.priceSell = priceSell;
		this.taxCatId = taxCatId;
		this.categoryId = categoryId;
		this.unit = unit;
		this.attributeSetId = attributeSetId;
	}

	// serialized as "id_" for compatibility with existing REST consumers (e.g. OrderItemPerformanceTest)
	@JsonProperty("id_")
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getReference() {
		return reference;
	}

	public void setReference(String reference) {
		this.reference = reference;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double getPriceBuy() {
		return priceBuy;
	}

	public void setPriceBuy(double priceBuy) {
		this.priceBuy = priceBuy;
	}

	// serialized as "pricesell" (no camelCase) for compatibility with existing REST consumers
	@JsonProperty("pricesell")
	public double getPriceSell() {
		return priceSell;
	}

	public void setPriceSell(double priceSell) {
		this.priceSell = priceSell;
	}

	public String getTaxCatId() {
		return taxCatId;
	}

	public void setTaxCatId(String taxCatId) {
		this.taxCatId = taxCatId;
	}

	public String getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(String categoryId) {
		this.categoryId = categoryId;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	public String getAttributeSetId() {
		return attributeSetId;
	}

	public void setAttributeSetId(String attributeSetId) {
		this.attributeSetId = attributeSetId;
	}

	// "active" is the desktop product editor's catalog flag: a PRODUCTS_CAT row for this
	// product means it is offered in the POS, no row means it is hidden. There is no ACTIVE
	// column on PRODUCTS.
	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
}
