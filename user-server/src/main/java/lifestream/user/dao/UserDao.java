package lifestream.user.dao;

import lifestream.user.bean.UserEntity;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserDao extends HibernateDao {
	private static final Logger logger = LoggerFactory.getLogger(UserDao.class.getSimpleName());

	public void create(UUID id, String username, String password, String email) {
		create(new UserEntity(id, username, password, email));
	}

	public UserEntity create(String username, String password, String email) {
		return create(new UserEntity(username, password, email));
	}

	public UserEntity create(UserEntity user) throws HibernateException {
		Session session = openSession();
		Transaction transaction = null;
		try {
			transaction = session.beginTransaction();
			user.setCreatedTimestamp();
			user.setModifiedTimestamp();
			session.save(user);
			transaction.commit();
			return user;
		} catch (ConstraintViolationException ex) {
			logger.error("Create user failed without rollback", ex);
			throw ex;
		} catch (HibernateException ex) {
			if (transaction != null) {
				transaction.rollback();
			}
			logger.error("Create user failed with rollback", ex);
			throw ex;
		} finally {
			closeSession(session);
		}
	}

	public List<UserEntity> create(List<UserEntity> users) throws HibernateException {
		Session session = openSession();
		Transaction transaction = null;
		try {
			transaction = session.beginTransaction();
			int i = 0;
			for (UserEntity user : users) {
				user.setCreatedTimestamp();
				user.setModifiedTimestamp();

				session.save(user);
				if (++i % BATCH_FLUSH_SIZE == 0) { // same as the JDBC batch size
					// flush a batch of inserts and release memory
					if (session.isDirty()) {
						session.flush();
						session.clear();
					}
				}
			}
			transaction.commit();
			return users;
		} catch (HibernateException ex) {
			if (transaction != null) {
				transaction.rollback();
			}
			logger.error("Create users failed with rollback", ex);
			throw ex;
		} finally {
			closeSession(session);
		}
	}

	public UserEntity get(UUID id) throws HibernateException {
		Session session = openSession();
		Transaction transaction;
		UserEntity user = null;
		try {
			transaction = session.beginTransaction();
			user = (UserEntity) session.get(UserEntity.class, id);
			transaction.commit();
			if (user == null) {
				logger.warn("No such user with id " + id.toString());
			} else {
				logger.info("Get user success " + user.toString());
			}
		} catch (HibernateException ex) {
			logger.error("Get user failed", ex);
			throw ex;
		} finally {
			closeSession(session);
		}
		return user;
	}

	// not guarantee the order of return list
	public List<UserEntity> get(List<UUID> ids) throws HibernateException {
		Session session = openSession();
		Transaction transaction;
		List<UserEntity> users = new ArrayList<>(ids.size());
		try {
			transaction = session.beginTransaction();
			List list = session.createCriteria(UserEntity.class).add(Restrictions.in("id", ids)).list();
			for (Object u : list) {
				users.add((UserEntity) u);
			}
			transaction.commit();
		} catch (HibernateException ex) {
			logger.error("Get user failed", ex);
			throw ex;
		} finally {
			closeSession(session);
		}
		return users;
	}

	public UserEntity update(UserEntity user) throws HibernateException {
		return update(user.getId(), user.getUsername(), user.getEmail(), user.getPassword());
	}

	public UserEntity update(UUID id, String username, String email, String password) throws HibernateException {
		Session session = openSession();
		Transaction transaction = null;
		UserEntity user = get(id);

		try {

			transaction = session.beginTransaction();

			if (user == null) {
				logger.warn("Cannot update a not existing user with id " + id);
			} else {
				user.setUsername(username);
				user.setPassword(password);
				user.setEmail(email);
				user.setModifiedTimestamp();
				session.update(user);
				// session.flush();

				logger.info("Updated user success " + user.toString());
			}
			transaction.commit();

			return user;
		} catch (HibernateException ex) {
			if (transaction != null) {
				transaction.rollback();
			}
			logger.error("Update user failed with rollback", ex);
			throw ex;
		} finally {
			closeSession(session);
		}
	}

	public void delete(UUID id) throws HibernateException {
		Session session = openSession();
		Transaction transaction;
		try {
			transaction = session.beginTransaction();
			UserEntity user = new UserEntity(id);
			session.delete(user);
			// session.flush();
			transaction.commit();
			System.out.println("Record deleted.");
		} catch (HibernateException ex) {
			logger.error("Delete user failed without rollback", ex);
			throw ex;
		} finally {
			closeSession(session);
		}
	}
}
