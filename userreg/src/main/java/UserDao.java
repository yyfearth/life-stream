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
public interface UserDAO {
    public void create(int id, String userName,String password, String eMail);
    public void searchId(int id);
    public void searchName(String name);
    public void listAll();
    public void update(int id, String password, String email);
    public void delete(int id);
}
