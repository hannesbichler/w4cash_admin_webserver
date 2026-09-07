package w4cash.attribute;

import jakarta.persistence.Embeddable;
//import jakarta.persistence.Column;

@Embeddable
public class Attribute {
    private String id;
    // @Column(name = "attribute_value")
    private String name;

    Attribute() {
    }

    public Attribute(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }
}
