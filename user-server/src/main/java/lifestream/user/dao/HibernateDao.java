package lifestream.user.dao;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class HibernateDao {
	private static final Logger logger = LoggerFactory.getLogger(HibernateDao.class.getSimpleName());
	protected static Configuration conf;
	protected static SessionFactory sessionFactory;
	protected Session session = null;
	protected Transaction transaction = null;

	{
		try {
			conf = new Configuration();
			conf.addResource("hibernate.cfg.xml");
			conf.configure();

			ServiceRegistry serviceRegistry = new ServiceRegistryBuilder().applySettings(conf.getProperties()).buildServiceRegistry();
			sessionFactory = conf.buildSessionFactory(serviceRegistry);
		} catch (HibernateException ex) {
			logger.error("Hibernate failed to init", ex);
		}
	}

	// public Session getSession() {
	// 	return session;
	// }

	// public Transaction getTransaction() {
	// 	return transaction;
	// }

	public void beginTransaction() throws HibernateException {
		session = sessionFactory.openSession();
		transaction = session.beginTransaction();
	}

	public void endTransaction() throws HibernateException {
		endTransaction(true);
	}

	public void endTransaction(boolean commit) throws HibernateException {
		if (commit) {
			transaction.commit();
		} else {
			transaction.rollback();
		}
		transaction = null;
		// closeSession();
	}

	public void rollback() {
		if (null != transaction) {
			try {
				transaction.rollback();
			} catch (HibernateException ex) {
				logger.error("Hibernate failed to rollback", ex);
			}
		}
	}

	public void closeSession() {
		try {
			if (session != null) {
				session.close();
				session = null;
			}
		} catch (HibernateException ex) {
			logger.error("Hibernate failed to close session", ex);
		}
	}
}
