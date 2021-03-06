package lifestream.user.bean;

import lifestream.user.data.UserMessage;
import org.hibernate.annotations.Type;

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

	public UserEntity(UUID id, String username, String email, String password, Date created, Date modified) {
		this.id = id == null ? UUID.randomUUID() : id;
		this.username = username;
		this.email = email;
		this.password = password;
		this.createdTimestamp = created;
		this.modifiedTimestamp = modified;
	}

	public UserEntity(UserMessage.User user) {
		id = UUID.fromString(user.getId());
		username = user.getUsername();
		email = user.getEmail();
		password = user.getPassword();
		setCreatedTimestamp(user.getCreatedTimestamp());
		setModifiedTimestamp(user.getModifiedTimestamp());
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

	public Date getCreatedTimestamp() {
		return createdTimestamp;
	}

	public void setCreatedTimestamp() {
		this.createdTimestamp = new Date();
	}

	public void setCreatedTimestamp(long created) {
		this.createdTimestamp = created == 0 ? null : new Date(created);
	}

	public void setCreatedTimestamp(Date created) {
		this.createdTimestamp = created;
	}

	public Date getModifiedTimestamp() {
		return modifiedTimestamp;
	}

	public void setModifiedTimestamp() {
		this.modifiedTimestamp = new Date();
	}

	public void setModifiedTimestamp(long modified) {
		this.modifiedTimestamp = modified == 0 ? null : new Date(modified);
	}

	public void setModifiedTimestamp(Date modified) {
		this.modifiedTimestamp = modified;
	}

	public UserMessage.User toProtobuf() {
		return UserMessage.User.newBuilder()
				.setId(id.toString())
				.setUsername(username == null ? "" : username)
				.setEmail(email == null ? "" : email)
				.setPassword(password == null ? "" : password)
				.setCreatedTimestamp(createdTimestamp == null ? 0 : createdTimestamp.getTime())
				.setModifiedTimestamp(modifiedTimestamp == null ? 0 : modifiedTimestamp.getTime())
				.build();
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
