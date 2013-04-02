package bean;

import com.vividsolutions.jts.geom.Point;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * User: wilson
 * Date: 3/31/13
 * Time: 7:54 PM
 */
@Table(name = "image", schema = "public", catalog = "lifestream")
@Entity
public class ImageEntity {
	@Id
	@Column(name = "id", unique = true, nullable = false)
	@Type(type="pg-uuid")
	private UUID id;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	@Column(name = "name", nullable = false, length = 256)
	@Basic
	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Column(name = "mime", nullable = false, length = 256)
	@Basic
	private String mime;

	public String getMime() {
		return mime;
	}

	public void setMime(String mime) {
		this.mime = mime;
	}

//	private String desc;
//
//	@Column(name = "desc")
//	@Basic
//	public String getDesc() {
//		return desc;
//	}
//
//	public void setDesc(String desc) {
//		this.desc = desc;
//	}

	@Column(name = "length", nullable = false)
	@Basic
	private long length;

	public long getLength() {
		return length;
	}

	public void setLength(long length) {
		this.length = length;
	}

	@Column(name = "width", nullable = true)
	@Basic
	private int width;

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	@Column(name = "height", nullable = true)
	@Basic
	private int height;

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	@Column(name = "geo_location", nullable = true)
	@Type(type = "org.hibernate.spatial.GeometryType")
	@Basic
	private Point geoLocation;

	public Point getGeoLocation() {
		return geoLocation;
	}

	public void setGeoLocation(Point geoLocation) {
		this.geoLocation = geoLocation;
	}

	@Column(name = "original_ts", nullable = true)
	@Basic
	private Date originalTimestamp;

	public Date getOriginalTimestamp() {
		return originalTimestamp;
	}

	public void setOriginalTimestamp(Date originalTimestamp) {
		this.originalTimestamp = originalTimestamp;
	}

	@Column(name = "created_ts", nullable = false)
	@Basic
	private Date createdTimestamp;

	public Date getCreatedTimestamp() {
		return createdTimestamp;
	}

	public void setCreatedTimestamp(Date createdTimestamp) {
		this.createdTimestamp = createdTimestamp;
	}

	@Column(name = "modified_ts", nullable = false)
	@Basic
	private Date modifiedTimestamp;

	public Date getModifiedTimestamp() {
		return modifiedTimestamp;
	}

	public void setModifiedTimestamp(Date modifiedTimestamp) {
		this.modifiedTimestamp = modifiedTimestamp;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ImageEntity that = (ImageEntity) o;

		if (height != that.height) return false;
		if (length != that.length) return false;
		if (width != that.width) return false;
		if (createdTimestamp != null ? !createdTimestamp.equals(that.createdTimestamp) : that.createdTimestamp != null)
			return false;
//		if (desc != null ? !desc.equals(that.desc) : that.desc != null) return false;
		if (geoLocation != null ? !geoLocation.equals(that.geoLocation) : that.geoLocation != null) return false;
		if (id != null ? !id.equals(that.id) : that.id != null) return false;
		if (mime != null ? !mime.equals(that.mime) : that.mime != null) return false;
		if (modifiedTimestamp != null ? !modifiedTimestamp.equals(that.modifiedTimestamp) : that.modifiedTimestamp != null)
			return false;
		if (name != null ? !name.equals(that.name) : that.name != null) return false;
		if (originalTimestamp != null ? !originalTimestamp.equals(that.originalTimestamp) : that.originalTimestamp != null)
			return false;
//		if (userId != null ? !userId.equals(that.userId) : that.userId != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
//		result = 31 * result + (userId != null ? userId.hashCode() : 0);
		result = 31 * result + (name != null ? name.hashCode() : 0);
		result = 31 * result + (mime != null ? mime.hashCode() : 0);
//		result = 31 * result + (desc != null ? desc.hashCode() : 0);
		result = 31 * result + (int) (length ^ (length >>> 32));
		result = 31 * result + width;
		result = 31 * result + height;
		result = 31 * result + (geoLocation != null ? geoLocation.hashCode() : 0);
		result = 31 * result + (originalTimestamp != null ? originalTimestamp.hashCode() : 0);
		result = 31 * result + (createdTimestamp != null ? createdTimestamp.hashCode() : 0);
		result = 31 * result + (modifiedTimestamp != null ? modifiedTimestamp.hashCode() : 0);
		return result;
	}
}
