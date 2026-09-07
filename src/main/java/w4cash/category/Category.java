package w4cash.category;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A row of the Oracle CATEGORIES table, serialised straight to the client.
 *
 * <p>
 * Was a JPA entity mirrored into in-memory H2; {@code id_} is the real
 * CATEGORIES.ID and is the only identifier clients ever see. {@code children} is
 * assembled per request by {@code CategoryController}.
 */
class Category {

    private String id_;
    private String name;
    private String parentId;
    private int printer = 1;
    private List<Category> children;

    public Category() {
    }

    public Category(String id_, String name, String parentId) {
        this.id_ = id_;
        this.name = name;
        this.parentId = parentId;
    }

    public Category(String id_, String name, String parentId, int printer) {
        this.id_ = id_;
        this.name = name;
        this.parentId = parentId;
        this.printer = printer;
    }

    public String getId_() {
        return this.id_;
    }

    public void setId_(String id_) {
        this.id_ = id_;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public int getPrinter() {
        return printer;
    }

    public void setPrinter(int printer) {
        this.printer = printer;
    }

    public List<Category> getChildren() {
        if (this.children == null) {
            this.children = new ArrayList<Category>();
        }
        return children;
    }

    public void setChildren(List<Category> children) {
        this.children = children;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o)
            return true;
        if (!(o instanceof Category))
            return false;
        Category category = (Category) o;
        return Objects.equals(this.id_, category.id_)
                && Objects.equals(this.name, category.name) && Objects.equals(this.parentId, category.parentId)
                && this.printer == category.printer;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id_, this.name, this.parentId, this.printer);
    }

    @Override
    public String toString() {
        return "Category {" + "id_='" + this.id_ + '\'' + ", name='" + this.name + '\''
                + ", parentId='" + this.parentId + '\'' + ", printer=" + this.printer + '}';
    }
}
