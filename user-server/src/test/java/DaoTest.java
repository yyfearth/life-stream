import lifestream.user.bean.UserEntity;
import lifestream.user.dao.UserDao;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.UUID;

public class DaoTest {
	UserDao userDao;
	UUID testId = UUID.fromString("00000000-0000-0000-0000-000000000000");
	String testPs = "0000000000000000000000000000000000000000000000000000000000000000";

	@BeforeClass
	public void before() {
		userDao = new UserDao();
	}

	//@Test
	public void testAll() {
		addUser();
		updateUser();
		removeUser();
	}

	@Test
	public void addUser() {
		userDao.create(testId, "testing", "testing@ab.com", testPs);
		UserEntity user = userDao.get(testId);
		assert user.getEmail().equals("testing@ab.com");
	}

	@Test
	public void getUser() {
		System.out.println(userDao.get(testId));
	}

	@Test
	public void updateUser() {
		UserEntity user = userDao.get(testId);
		assert user.getEmail().equals("testing@ab.com");
		userDao.update(testId, "testing2", "testing.2@ab.com", testPs);
		user = userDao.get(testId);
		assert user.getEmail().equals("testing.2@ab.com");
	}

	@Test
	public void removeUser() {
//		UserEntity user = userDao.get(testId);
//		assert user != null;
		userDao.delete(testId);
		UserEntity user = userDao.get(testId);
		assert null == user;
	}
}
