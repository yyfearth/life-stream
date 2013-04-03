package lifestream.user.dao;

import lifestream.user.bean.UserEntity;
import org.hibernate.HibernateException;
import org.hibernate.exception.ConstraintViolationException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		try {
			beginTransaction();
			session.save(user);
			endTransaction();
			return user;
		} catch (ConstraintViolationException ex) {
			logger.error("Create user failed without rollback", ex);
			throw ex;
		} catch (HibernateException ex) {
			rollback();
			logger.error("Create user failed with rollback", ex);
			throw ex;
		} finally {
			closeSession();
		}
	}

	public UserEntity get(UUID id) throws HibernateException {
		UserEntity user = null;
		try {
			beginTransaction();
			user = (UserEntity) session.get(UserEntity.class, id);
			endTransaction();
			if (user == null) {
				logger.warn("No such user with id " + id.toString());
			} else {
				logger.info("Get user success " + user.toString());
			}
		} catch (HibernateException ex) {
			logger.error("Get user failed", ex);
			throw ex;
		} finally {
			closeSession();
		}
		return user;
	}

//	public void searchByName(String name) {
//		try {
//			beginTransaction();
//
//			Query query = session.createQuery("from UserEntity where username like ?");
//			String str = "%" + name + "%";
//			query.setString(0, str);
//			List list = query.list();
//			System.out.println("Search username by: " + name);
//
//			if (list.size() > 0) {
//				for (int i = 0; i < list.size(); i++) {
//					UserEntity user = (UserEntity) list.get(i);
//					System.out.println("ID: " + user.getId() + ", " + user.getUsername() + " ( " + user.getPassword() + ") , " + user.getEmail());
//				}
//				System.out.println("---------------------------------------------------------------------");
//			} else {
//				System.out.println("No matched record.");
//			}
//			endTransaction();
//		} catch (HibernateException ex) {
//			rollback();
//			System.out.println("Hibernate exception during searching via name.");
//		} finally {
//			closeSession();
//		}
//	}


//	public void list() {
//		try {
//			beginTransaction();
//
//			Query query = session.createQuery("from UserEntity");
//			List list = query.list();
//
//			Iterator it = list.iterator();
//			while (it.hasNext()) {
//				UserEntity user = (UserEntity) it.next();
//				System.out.println("ID: " + user.getId() + ", " + user.getUsername() + " ( " + user.getPassword() + ") , " + user.getEmail());
//			}
//			System.out.println("---------------------------------------------------------------------");
//			endTransaction();
//		} catch (HibernateException ex) {
//			rollback();
//			System.out.println("Hibernate exception during listing all records.");
//		} finally {
//			closeSession();
//		}
//	}

	public UserEntity update(UserEntity user) throws HibernateException {
		return update(user.getId(), user.getUsername(), user.getEmail(), user.getPassword());
	}

	public UserEntity update(UUID id, String username, String email, String password) throws HibernateException {
		UserEntity user = get(id);

		try {

			beginTransaction();

			if (user == null) {
				logger.warn("Cannot update a not existing user with id " + id);
			} else {
				user.setUsername(username);
				user.setPassword(password);
				user.setEmail(email);
				user.setModifiedDateTime(DateTime.now());
				session.update(user);
				session.flush();

				logger.info("Updated user success " + user.toString());
			}
			endTransaction();

			return user;
		} catch (HibernateException ex) {
			rollback();
			logger.error("Update user failed with rollback", ex);
			throw ex;
		} finally {
			closeSession();
		}
	}


	public void delete(UUID id) throws HibernateException {
		try {
			beginTransaction();
			UserEntity user = new UserEntity(id);
			session.delete(user);
			// session.flush();
			endTransaction();
			// TODO: check not exists exception
			System.out.println("Record deleted.");
		} catch (HibernateException ex) {
			logger.error("Delete user failed without rollback", ex);
			throw ex;
		} finally {
			closeSession();
		}
	}
}
