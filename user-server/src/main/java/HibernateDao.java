/**
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

public abstract class HibernateDao {
	protected Session session = null;
	protected Transaction transaction = null;
	protected SessionFactory sessionFactory = null;

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
