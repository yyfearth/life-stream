package bean;

import org.postgis.Point;

import javax.persistence.*;
import java.sql.Timestamp;
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
    private UUID id;

    @Column(name = "id")
    @Id
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    private short nodeId;

    @Column(name = "node_id")
    @Basic
    public short getNodeId() {
        return nodeId;
    }

    public void setNodeId(short nodeId) {
        this.nodeId = nodeId;
    }

    private UUID userId;

    @Column(name = "user_id")
    @Basic
    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    private String name;

    @Column(name = "name")
    @Basic
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private String mime;

    @Column(name = "mime")
    @Basic
    public String getMime() {
        return mime;
    }

    public void setMime(String mime) {
        this.mime = mime;
    }

    private String desc;

    @Column(name = "desc")
    @Basic
    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    private long length;

    @Column(name = "length")
    @Basic
    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    private int width;

    @Column(name = "width")
    @Basic
    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    private int height;

    @Column(name = "height")
    @Basic
    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    private Point geoLocation;

    @Column(name = "geo_location")
    @Basic
    public Point getGeoLocation() {
        return geoLocation;
    }

    public void setGeoLocation(Point geoLocation) {
        this.geoLocation = geoLocation;
    }

    private Timestamp originalTimestamp;

    @Column(name = "original_ts")
    @Basic
    public Timestamp getOriginalTimestamp() {
        return originalTimestamp;
    }

    public void setOriginalTimestamp(Timestamp originalTimestamp) {
        this.originalTimestamp = originalTimestamp;
    }

    private Timestamp createdTimestamp;

    @Column(name = "created_ts")
    @Basic
    public Timestamp getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(Timestamp createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    private Timestamp modifiedTimestamp;

    @Column(name = "modified_ts")
    @Basic
    public Timestamp getModifiedTimestamp() {
        return modifiedTimestamp;
    }

    public void setModifiedTimestamp(Timestamp modifiedTimestamp) {
        this.modifiedTimestamp = modifiedTimestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ImageEntity that = (ImageEntity) o;

        if (height != that.height) return false;
        if (length != that.length) return false;
        if (nodeId != that.nodeId) return false;
        if (width != that.width) return false;
        if (createdTimestamp != null ? !createdTimestamp.equals(that.createdTimestamp) : that.createdTimestamp != null)
            return false;
        if (desc != null ? !desc.equals(that.desc) : that.desc != null) return false;
        if (geoLocation != null ? !geoLocation.equals(that.geoLocation) : that.geoLocation != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (mime != null ? !mime.equals(that.mime) : that.mime != null) return false;
        if (modifiedTimestamp != null ? !modifiedTimestamp.equals(that.modifiedTimestamp) : that.modifiedTimestamp != null)
            return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (originalTimestamp != null ? !originalTimestamp.equals(that.originalTimestamp) : that.originalTimestamp != null)
            return false;
        if (userId != null ? !userId.equals(that.userId) : that.userId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (int) nodeId;
        result = 31 * result + (userId != null ? userId.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (mime != null ? mime.hashCode() : 0);
        result = 31 * result + (desc != null ? desc.hashCode() : 0);
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
