package w4cash.ticketinfo;

import java.util.List;
import java.util.Objects;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class OrderItem {

    // id: row.ID, code:row.CODE, name: row.NAME, pricesell: row.PRICESELL,
    // category: row.CATEGORY
    private @Id @GeneratedValue Long id;
    private String id_ = "";
    private int tickettype = 0;
    private int ticketId = 0;
    @ElementCollection(fetch = FetchType.EAGER)
    private List<OrderLine> lines = new java.util.ArrayList<>();
    private String lockby = "";
    private String kellner = "";

    public String getId_() {
        return id_;
    }

    public String getKellner() {
        return kellner;
    }

    public void setKellner(String kellner) {
        this.kellner = kellner;
    }

    public String getLockby() {
        return lockby;
    }

    public void setLockby(String lockby) {
        this.lockby = lockby;
    }

    public void setId_(String id_) {
        this.id_ = id_;
    }

    public int getTickettype() {
        return tickettype;
    }

    public void setTickettype(int tickettype) {
        this.tickettype = tickettype;
    }

    public int getTicketId() {
        return ticketId;
    }

    public void setTicketId(int ticketId) {
        this.ticketId = ticketId;
    }

    public List<OrderLine> getLines() {
        return lines;
    }

    public void setLines(List<OrderLine> lines) {
        this.lines = lines;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o)
            return true;
        if (!(o instanceof OrderItem))
            return false;
        OrderItem orderItem = (OrderItem) o;
        return Objects.equals(this.id, orderItem.id) && Objects.equals(this.id_, orderItem.id_)
                && Objects.equals(this.tickettype, orderItem.tickettype)
                && Objects.equals(this.ticketId, orderItem.ticketId)
                && Objects.equals(this.lines, orderItem.lines)
                && Objects.equals(this.lockby, orderItem.lockby)
                && Objects.equals(this.kellner, orderItem.kellner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.id_, this.tickettype, this.ticketId, this.lines, this.lockby, this.kellner);
    }

    @Override
    public String toString() {
        return "OrderItem{id:" + this.id + ",id_:" + this.id_ + ", tickettype:" + this.tickettype + ", ticketId:"
                + this.ticketId + ", lines:" + this.lines + ", lockby:" + this.lockby + ", kellner:" + this.kellner
                + "}";
    }
}
