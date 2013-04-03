package lifestream.user.bean;

import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@Table(name = "user", schema = "public", catalog = "lifestream")
@Entity
public class UserEntity {

	public UserEntity() {
		this(UUID.randomUUID());
	}

	public UserEntity(UUID id) {
		this(id, null, null, null, null, null);
	}

	public UserEntity(String username, String email, String password) {
		this(UUID.randomUUID(), username, email, password, null, null);
	}

	public UserEntity(UUID id, String username, String email, String password) {
		this(id, username, email, password, null, null);
	}

	public UserEntity(UUID id, String username, String email, String password, DateTime createdDateTime, DateTime modifiedDateTime) {
		this.id = id == null ? UUID.randomUUID() : id;
		this.username = username == null ? "" : username;
		this.email = email == null ? "" : email;
		this.password = password == null ? "" : password;
		setCreatedDateTime(createdDateTime == null ? DateTime.now() : createdDateTime);
		setModifiedDateTime(modifiedDateTime == null ? DateTime.now() : modifiedDateTime);
	}

	@Id
	@Column(name = "id", unique = true, nullable = false)
	@Type(type = "pg-uuid")
	private UUID id;

	@Column(name = "username", nullable = false, length = 256)
	@Basic
	private String username;

	@Column(name = "email", unique = true, nullable = false, length = 256)
	@Basic
	private String email;

	@Column(name = "password", nullable = false, length = 64)
	@Basic
	private String password;

	@Column(name = "created_ts", nullable = false)
	@Type(type = "timestamp")
	@Basic
	private Date createdTimestamp;

	@Column(name = "modified_ts", nullable = false)
	@Type(type = "timestamp")
	@Basic
	private Date modifiedTimestamp;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public DateTime getCreatedDateTime() {
		return new DateTime(createdTimestamp);
	}

	public void setCreatedDateTime(DateTime createdTimestamp) {
		this.createdTimestamp = createdTimestamp.toDate();
	}

	public DateTime getModifiedDateTime() {
		return new DateTime(modifiedTimestamp);
	}

	public void setModifiedDateTime(DateTime modifiedTimestamp) {
		this.modifiedTimestamp = modifiedTimestamp.toDate();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		UserEntity that = (UserEntity) o;

		if (!createdTimestamp.equals(that.createdTimestamp)) return false;
		if (!email.equals(that.email)) return false;
		if (!id.equals(that.id)) return false;
		if (!modifiedTimestamp.equals(that.modifiedTimestamp)) return false;
		if (!password.equals(that.password)) return false;
		if (!username.equals(that.username)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = id.hashCode();
		result = 31 * result + username.hashCode();
		result = 31 * result + email.hashCode();
		result = 31 * result + password.hashCode();
		result = 31 * result + createdTimestamp.hashCode();
		result = 31 * result + modifiedTimestamp.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "UserEntity{" +
				"id=" + id +
				", username='" + username + '\'' +
				", email='" + email + '\'' +
				", password='" + password + '\'' +
				", createdTimestamp=" + createdTimestamp +
				", modifiedTimestamp=" + modifiedTimestamp +
				'}';
	}
}
