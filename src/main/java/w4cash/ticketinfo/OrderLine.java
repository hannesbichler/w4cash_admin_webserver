package w4cash.ticketinfo;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;
import w4cash.attribute.Attribute;

@Embeddable
public class OrderLine {
    private String id;
    private String orderId;
    private String productId;
    private String productName;
    private double pricesell;
    private double qty;
    private double newQty;
    private String productAttSetId;
    private String attSetInstDesc;
    @Transient
    private List<Attribute> attributes = new ArrayList<>();

    public OrderLine() {
    }

    public OrderLine(String id, String orderId, String productId, String productName, double pricesell, double qty,
            double newQty, String productAttSetId, String attSetInstDesc) {
        this(id, orderId, productId, productName, pricesell, qty, newQty, productAttSetId, attSetInstDesc,
                new ArrayList<>());
    }

    public OrderLine(String id, String orderId, String productId, String productName, double pricesell, double qty,
            double newQty, String productAttSetId, String attSetInstDesc, List<Attribute> attributes) {
        this.id = id;
        this.orderId = orderId;
        this.productId = productId;
        this.productName = productName;
        this.pricesell = pricesell;
        this.qty = qty;
        this.newQty = newQty;
        this.productAttSetId = productAttSetId;
        this.attSetInstDesc = attSetInstDesc;
        this.attributes = attributes;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public double getPricesell() {
        return pricesell;
    }

    public void setPricesell(double pricesell) {
        this.pricesell = pricesell;
    }

    public double getQty() {
        return qty;
    }

    public void setQty(double qty) {
        this.qty = qty;
    }

    public double getNewQty() {
        return newQty;
    }

    public void setNewQty(double newQty) {
        this.newQty = newQty;
    }

    public String getProductAttSetId() {
        return productAttSetId;
    }

    public void setProductAttSetId(String productAttSetId) {
        this.productAttSetId = productAttSetId;
    }

    public String getAttSetInstDesc() {
        return attSetInstDesc;
    }

    public void setAttSetInstDesc(String attSetInstDesc) {
        this.attSetInstDesc = attSetInstDesc;
    }

    public List<Attribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<Attribute> attributes) {
        this.attributes = attributes;
    }
}
