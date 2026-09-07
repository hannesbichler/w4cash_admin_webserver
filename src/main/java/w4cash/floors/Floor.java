package w4cash.floors;

import java.util.Objects;

/**
 * A row of the Oracle FLOORS table, serialised straight to the client.
 *
 * <p>
 * Was a JPA entity mirrored into in-memory H2; {@code id_} is the real FLOORS.ID
 * and is the only identifier clients ever see.
 */
class Floor {

	private String id_;
	private String name;
	// FLOORS.SORTORDER, nullable: the position the floor is shown in, unset for older rows.
	private Integer sortOrder;

	Floor() {
	}

	Floor(String id_, String name) {
		this.id_ = id_;
		this.name = name;
	}

	Floor(String id_, String name, Integer sortOrder) {
		this.id_ = id_;
		this.name = name;
		this.sortOrder = sortOrder;
	}

	public String getId_() {
		return this.id_;
	}

	public String getName() {
		return this.name;
	}

	public void setId_(String id_) {
		this.id_ = id_;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getSortOrder() {
		return this.sortOrder;
	}

	public void setSortOrder(Integer sortOrder) {
		this.sortOrder = sortOrder;
	}

	@Override
	public boolean equals(Object o) {

		if (this == o)
			return true;
		if (!(o instanceof Floor))
			return false;
		Floor floor = (Floor) o;
		return Objects.equals(this.id_, floor.id_) && Objects.equals(this.name, floor.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.id_, this.name);
	}

	@Override
	public String toString() {
		return "Floor {" + "id_='" + this.id_ + '\'' + ", name='" + this.name + '\'' + '}';
	}
}
