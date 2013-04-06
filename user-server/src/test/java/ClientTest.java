import lifestream.user.bean.UserEntity;
import lifestream.user.client.UserClient;
import lifestream.user.data.UserMessage;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.util.*;

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
	public void testPing() {
		userClient.ping(new UserClient.ResultRequestResponseHandler() {

			@Override
			public void receivedOK(Date timestamp) {
				System.out.println("Passed: \n" + timestamp + "\n");
				assert true;
			}

			@Override
			public void receivedError(UserMessage.Response.ResultCode code, String message) {
				System.out.println("Failed: \n" + message + "\n");
				assert false;
			}
		});
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@Test
	public void test() {
		try {
			// batch add
			int delay, userCount = generator.nextInt(100);

			List<UUID> userIds = new ArrayList<>(userCount);
			while (userCount-- > 0) {
				final UserEntity user = genUser();
				final UUID id = user.getId();
				userIds.add(user.getId());
				userClient.addUser(user, new UserClient.UserRequestResponseHandler() {

					@Override
					public void receivedUser(UserEntity user) {
						assert id.equals(user.getId());
						System.out.println("Passed: \n" + user + "\n");
					}

					@Override
					public void receivedError(UserMessage.Response.ResultCode code, String message) {
						System.out.println("Failed: \n" + message + "\n");
						assert false;
					}
				});
				delay = generator.nextInt(200);
				if (delay > 100) {
					Thread.sleep(delay);
				}
			}
			Thread.sleep(500);
			// batch get
			for (final UUID id : userIds) {
				userClient.getUser(id, new UserClient.UserRequestResponseHandler() {

					@Override
					public void receivedUser(UserEntity user) {
						assert id.equals(user.getId());
						System.out.println("Passed: \n" + user + "\n");
					}

					@Override
					public void receivedError(UserMessage.Response.ResultCode code, String message) {
						System.out.println("Failed: \n" + message + "\n");
						assert false;
					}
				});
				delay = generator.nextInt(111);
				if (delay > 100) {
					Thread.sleep(delay);
				}
			}
			Thread.sleep(3000);
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
