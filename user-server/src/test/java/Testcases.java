import org.hibernate.*;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Restrictions;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import bean.UserEntity;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

/**
 * Created with IntelliJ IDEA.
 * User: Leo
 * Date: 3/4/13
 * Time: 4:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class Testcases {

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
        userEntity.setId(10);
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
        session.save(userEntity);

        transaction.commit();
        session.close();

        session = sessionFactory.openSession();
        transaction = session.beginTransaction();

        UserEntity userEntity2 = new UserEntity();
        userEntity2.setUsername("testing.2");
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
            userEntity.setId(99);
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
