import bean.UserEntity;
import org.hibernate.*;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Restrictions;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.testng.annotations.BeforeClass;

import java.util.List;

import static org.testng.Assert.assertEquals;

/**
 * Created with IntelliJ IDEA.
 * User: Leo
 * Date: 3/4/13
 * Time: 4:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserDao {

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

    public void createRegistry() {
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


    public void readSingleRegistry(){
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        Criteria criteria = session.createCriteria(UserEntity.class);
        criteria.add(Restrictions.eq("username", "testing"));
        List<UserEntity> userList = criteria.list(); // This stupid method implicitly executes the query.

        assertEquals(userList.size(), 1);
    }

    public void readAllRegistry() {
        Session session = sessionFactory.openSession();
        Query query = session.createQuery("from UserEntity");

        System.out.println("The query is " + query.getQueryString());

        List<UserEntity> userList = (List<UserEntity>) query.list();

        for (UserEntity user : userList) {
            System.out.format("Id: %s Name: %s\n", user.getId(), user.getUsername());
        }

        session.close();
    }


    public void updateRegistry() {


    }


    public void deleteRegistry() {


    }
}
