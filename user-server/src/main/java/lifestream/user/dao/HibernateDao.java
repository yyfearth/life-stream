package lifestream.user.dao;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class HibernateDao {
	private static final Logger logger = LoggerFactory.getLogger(HibernateDao.class.getSimpleName());
	private static boolean hasFault = false;
	public static final int BATCH_SIZE = 20; // in the cfg file
	protected static Configuration conf;
	protected static SessionFactory sessionFactory = null;

	{
		try {
			conf = new Configuration();
			conf.addResource("hibernate.cfg.xml");
			conf.configure();

			ServiceRegistry serviceRegistry = new ServiceRegistryBuilder().applySettings(conf.getProperties()).buildServiceRegistry();
			sessionFactory = conf.buildSessionFactory(serviceRegistry);
		} catch (HibernateException ex) {
			logger.error("Hibernate failed to init", ex);
			hasFault = true;
		}
	}

	public static boolean isAvailable() {
		return !hasFault;
	}

	public static Session openSession() throws HibernateException {
		try {
			return sessionFactory.openSession();
		} catch (HibernateException ex) {
			logger.error("Open session failed", ex);
			throw ex;
		}
	}

	public static void closeSession(Session session) {
		try {
			if (session != null) {
				session.close();
			}
		} catch (HibernateException ex) {
			logger.error("Close session failed", ex);
		}
	}


}
