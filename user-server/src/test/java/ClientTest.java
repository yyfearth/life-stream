import lifestream.user.bean.UserEntity;
import lifestream.user.client.UserClient;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class ClientTest {
	UserClient userClient = new UserClient("localhost", 8888);

	@BeforeSuite
	public void before() {
		userClient.connect();
	}

	@AfterSuite
	public void after() {
		userClient.close();
	}

	@Test
	public void test() {
		try {
			userClient.ping();
			// batch add
			int delay, userCount = generator.nextInt(100);
			UserEntity user;
			List<UUID> userIds = new ArrayList<>(userCount);
			while (--userCount > 0) {
				user = genUser();
				userIds.add(user.getId());
				userClient.addUser(user);
				delay = generator.nextInt(200);
				if (delay > 100) {
					Thread.sleep(delay);
				}
			}
			Thread.sleep(500);
			// batch get
			for (UUID id : userIds) {
				userClient.getUser(id);
				delay = generator.nextInt(111);
				if (delay > 100) {
					Thread.sleep(delay);
				}
			}
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	private Random generator = new Random();

	private UserEntity genUser() {

		String username = "test." + generator.nextInt(65535);
		UserEntity user = new UserEntity();
		user.setEmail(username + "@ab.com");
		user.setUsername(username);
		user.setPassword(new String(new char[64]).replace('\0', '0'));

		return user;
	}
}
