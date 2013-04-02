/**
 * Created with IntelliJ IDEA.
 * User: Leo
 * Date: 4/1/13
 * Time: 10:31 PM
 * To change this template use File | Settings | File Templates.
 */

import bean.UserEntity;
import org.hibernate.HibernateException;

import java.util.UUID;

public class UserDao extends HibernateDao {

	public void create(UUID id, String username, String password, String email) {
		try {
			beginTransaction();

			UserEntity user = new UserEntity(id);
			user.setUsername(username);
			user.setPassword(password);
			user.setEmail(email);

			session.save(user);
			endTransaction(true);
		} catch (HibernateException ex) {
			rollback();
			System.out.println("Hibernate exception during creating user.");
		} finally {
			closeSession();
		}
	}

	public UserEntity get(UUID id) {
		UserEntity userEntity = null;
		try {
			beginTransaction();
			userEntity = (UserEntity) session.get(UserEntity.class, id);
			endTransaction(true);
		} catch (HibernateException ex) {
			rollback();
			System.out.println("Hibernate exception during searching via id.");
		} finally {
			closeSession();
		}
		return userEntity;
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
//			endTransaction(true);
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
//			endTransaction(true);
//		} catch (HibernateException ex) {
//			rollback();
//			System.out.println("Hibernate exception during listing all records.");
//		} finally {
//			closeSession();
//		}
//	}


	public void update(UUID id, String username, String password, String email) {
		try {
			beginTransaction();

			UserEntity user = get(id);

			if (user == null) {
				System.out.println("no record");
			} else {
				user.setUsername(username);
				user.setPassword(password);
				user.setEmail(email);
				session.update(user);
				// session.flush();

				System.out.println("Record successful updated.");
				System.out.println(user.toString());
			}
			endTransaction(true);

			System.out.println("---------------------------------------------------------------------");
		} catch (HibernateException ex) {
			rollback();
			System.out.println("Hibernate exception during updating.");
		} finally {
			closeSession();
		}
	}


	public void delete(UUID id) {
		try {
			beginTransaction();
			UserEntity user = new UserEntity(id);
			session.delete(user);
			endTransaction(true);
			// TODO: check not exists exception
			System.out.println("Record deleted.");
			System.out.println("---------------------------------------------------------------------");
		} catch (HibernateException ex) {
			// rollback();
			System.out.println("Hibernate exception during deleting a record.");
		} finally {
			closeSession();
		}
	}
}
