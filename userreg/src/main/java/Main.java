import bean.UserEntity;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;

public class Main {
    public static void main(final String[] args) {
        Configuration configuration = new Configuration();
        configuration.addResource("hibernate.cfg.xml");
        configuration.configure();

        ServiceRegistry serviceRegistry = new ServiceRegistryBuilder().applySettings(configuration.getProperties()).buildServiceRegistry();
        SessionFactory sessionFactory = configuration.buildSessionFactory(serviceRegistry);
        Session session = sessionFactory.openSession();

        Transaction transaction = session.beginTransaction();
        UserEntity userEntity = new UserEntity();
        //userEntity.setId(4);
        userEntity.setUsername("Wilson");
        userEntity.setPassword("peace");
        session.save(userEntity);
        transaction.commit();
        session.close();
    }
}
