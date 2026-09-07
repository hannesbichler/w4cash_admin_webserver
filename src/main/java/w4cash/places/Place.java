package w4cash.places;

import java.util.Objects;

/**
 * A row of the Oracle PLACES table, serialised straight to the client.
 *
 * <p>
 * Was a JPA entity mirrored into in-memory H2; {@code id_} is the real PLACES.ID
 * and is the only identifier clients ever see.
 */
class Place {

	private String id_;
	private String name;
	private String floorId;
	private int x;
	private int y;
	// WIDTH, HEIGHT and FONTSIZE are nullable in PLACES: unset means the POS draws the table
	// at its default size, which is not the same as 0.
	private Integer width;
	private Integer height;
	private Integer fontSize;
	private String fontColor;

	Place() {
	}

	Place(String id_, String name) {
		this.id_ = id_;
		this.name = name;
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

	public String getFloorId() {
		return this.floorId;
	}

	public void setFloorId(String floorId) {
		this.floorId = floorId;
	}

	public int getX() {
		return this.x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return this.y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public Integer getWidth() {
		return this.width;
	}

	public void setWidth(Integer width) {
		this.width = width;
	}

	public Integer getHeight() {
		return this.height;
	}

	public void setHeight(Integer height) {
		this.height = height;
	}

	public Integer getFontSize() {
		return this.fontSize;
	}

	public void setFontSize(Integer fontSize) {
		this.fontSize = fontSize;
	}

	public String getFontColor() {
		return this.fontColor;
	}

	public void setFontColor(String fontColor) {
		this.fontColor = fontColor;
	}

	@Override
	public boolean equals(Object o) {

		if (this == o)
			return true;
		if (!(o instanceof Place))
			return false;
		Place place = (Place) o;
		return Objects.equals(this.id_, place.id_) && Objects.equals(this.name, place.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.id_, this.name);
	}

	@Override
	public String toString() {
		return "Place {" + "id_='" + this.id_ + '\'' + ", name='" + this.name + '\'' + '}';
	}
}
