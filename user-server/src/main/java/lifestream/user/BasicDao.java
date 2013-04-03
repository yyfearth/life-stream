package lifestream.user; /**
 * Created with IntelliJ IDEA.
 * User: Leo
 * Date: 4/2/13
 * Time: 12:19 AM
 * To change this template use File | Settings | File Templates.
 */

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;

public abstract class BasicDao {
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
			System.out.println("Hibernate exception during start.");
		}
	}

	public Session getSession() {
		return session;
	}

	public Transaction getTransaction() {
		return transaction;
	}

	public void beginTransaction() throws HibernateException {
		session = sessionFactory.openSession();
		transaction = session.beginTransaction();
	}

	public void endTransaction(boolean commit) throws HibernateException {
		if (commit) {
			transaction.commit();
		} else {
			transaction.rollback();
		}
		closeSession();
	}

	public void rollback() {
		if (null != transaction) {
			try {
				transaction.rollback();
			} catch (HibernateException ex) {
				System.out.println("Hibernate exception during rollback.");
			}
		}
	}

	public void closeSession() {
		try {
			session.close();
		} catch (HibernateException ex) {
			System.out.println("Hibernate exception during closing the session.");
		}
	}
}
