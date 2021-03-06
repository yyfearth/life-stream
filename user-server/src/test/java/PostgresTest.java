import lifestream.user.bean.UserEntity;
import org.hibernate.*;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Restrictions;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

public class PostgresTest {

	SessionFactory sessionFactory;

	@BeforeClass
	public void before() {
		Configuration configuration = new Configuration();
		configuration.addResource("hibernate.cfg.xml");
		configuration.configure();

		ServiceRegistryBuilder serviceRegistryBuilder = new ServiceRegistryBuilder().applySettings(configuration.getProperties());
		ServiceRegistry serviceRegistry = serviceRegistryBuilder.buildServiceRegistry();
		sessionFactory = configuration.buildSessionFactory(serviceRegistry);
	}

	@Test(groups = {"create"})
	public void addRegistration() {
		Session session = sessionFactory.openSession();
		Transaction transaction = session.beginTransaction();

		UserEntity userEntity = new UserEntity();
		userEntity.setEmail("testing@ab.com");
		userEntity.setUsername("testing");
		userEntity.setPassword("testpass");
		session.saveOrUpdate(userEntity);

		transaction.commit();

		session.close();
	}

	@Test(groups = {"create"})
	public void addRegistration2() {
		Session session = sessionFactory.openSession();
		Transaction transaction = session.beginTransaction();

		UserEntity userEntity = new UserEntity();
		userEntity.setUsername("testing.1");
		userEntity.setEmail("testing.1@ab.com");
		userEntity.setPassword("testpass");
		session.save(userEntity);

		transaction.commit();
		session.close();

		session = sessionFactory.openSession();
		transaction = session.beginTransaction();

		UserEntity userEntity2 = new UserEntity();
		userEntity2.setEmail("testing.2@ab.com");
		userEntity2.setUsername("testing.2");
		userEntity2.setPassword("testpass");
		session.save(userEntity2);

		transaction.commit();
		session.close();
	}

	@Test(groups = {"read"})
	public void selectAllEntities() {
		Session session = sessionFactory.openSession();

		Query query = session.createQuery("from UserEntity");

		System.out.println("The query is " + query.getQueryString());

		List<UserEntity> userList = (List<UserEntity>) query.list();

		for (UserEntity user : userList) {
			System.out.format("Id: %s Name: %s\n", user.getId(), user.getUsername());
		}

		session.close();
	}

	@Test(groups = {"read"}, dependsOnGroups = {"create"})
	public void selectParticularEntity() {
		Session session = sessionFactory.openSession();
		Transaction transaction = session.beginTransaction();

		Criteria criteria = session.createCriteria(UserEntity.class);
		criteria.add(Restrictions.eq("username", "testing"));
		List<UserEntity> userList = criteria.list(); // This stupid method implicitly executes the query.

		assertEquals(userList.size(), 1);
	}

	@Test(groups = {"update"}, dependsOnGroups = {"read"})
	public void updateUsername() {
		Session session = sessionFactory.openSession();
		Transaction transaction = session.beginTransaction();

		Criteria criteria = session.createCriteria(UserEntity.class).add(Restrictions.like("username", "testing%"));
		List<UserEntity> userEntityList = (List<UserEntity>) criteria.list();

		for (UserEntity userEntity : userEntityList) {
			userEntity.setUsername("testing");
			userEntity.setEmail("test@test.com");
			userEntity.setPassword("testing");
			session.update(userEntity);
		}

		// transaction.commit();

		//  criteria = session.createCriteria(UserEntity.class).add(Restrictions.eq("username", "testing"));
		//  userEntityList = (List<UserEntity>) criteria.list();
		//  assertEquals(userEntityList.size(), 3);

		session.close();
	}

	@Test(groups = {"delete"}, dependsOnGroups = {"update"})
	public void deleteRegistrations() {
		Session session = sessionFactory.openSession();
		Transaction transaction = session.beginTransaction();

		List<UserEntity> userEntityList = (List<UserEntity>) session.createCriteria(UserEntity.class).add(Restrictions.like("username", "testing%")).list();
		for (UserEntity userEntity : userEntityList) {
			session.delete(userEntity);
		}

		transaction.commit();

		// transaction = session.beginTransaction();

		// Criteria criteria = session.createCriteria(UserEntity.class);
		//  criteria.add(Restrictions.like("username", "testing%"));
		//  List<UserEntity> userList = criteria.list(); // This stupid method implicitly executes the query.

		//  transaction.commit();

		// assertEquals(userList.size(), 0);

		session.close();
	}
}
