package w4cash.role;

import java.util.Objects;

/**
 * A row of the Oracle ROLES table, serialised straight to the client.
 *
 * <p>
 * ROLES has a third column, PERMISSIONS (a BLOB of the XML listing the POS
 * screens the role may open). It is deliberately not carried here: the admin UI
 * names roles and assigns people to them, while what a role may actually do is
 * edited in the POS. Leaving it off the DTO keeps a rename from ever rewriting
 * it. {@code id_} is the real ROLES.ID and the only identifier clients see.
 */
class Role {

	private String id_;
	private String name;

	Role() {
	}

	Role(String id_, String name) {
		this.id_ = id_;
		this.name = name;
	}

	public String getId_() {
		return this.id_;
	}

	public void setId_(String id_) {
		this.id_ = id_;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public boolean equals(Object o) {

		if (this == o)
			return true;
		if (!(o instanceof Role))
			return false;
		Role role = (Role) o;
		return Objects.equals(this.id_, role.id_) && Objects.equals(this.name, role.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.id_, this.name);
	}

	@Override
	public String toString() {
		return "Role {" + "id_='" + this.id_ + '\'' + ", name='" + this.name + '\'' + '}';
	}
}
