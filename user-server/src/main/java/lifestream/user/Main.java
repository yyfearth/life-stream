package lifestream.user;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;

public class Main {
	public static void main(final String[] args) {
		Configuration conf = new Configuration();
		conf.addResource("hibernate.cfg.xml");
		conf.configure();

		ServiceRegistry serviceRegistry = new ServiceRegistryBuilder().applySettings(conf.getProperties()).buildServiceRegistry();
		SessionFactory sessionFactory = conf.buildSessionFactory(serviceRegistry);
		//Session session = sessionFactory.openSession();
		//Transaction transaction = session.beginTransaction();
		//UserEntity userEntity = new UserEntity();
		//userEntity.setId(4);
		//userEntity.setUsername("Wilson");
		//userEntity.setPassword("peace");
		//session.save(userEntity);
		//transaction.commit();
		//session.close();
	}
}
