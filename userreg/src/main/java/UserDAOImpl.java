/**
 * Created with IntelliJ IDEA.
 * User: Leo
 * Date: 4/1/13
 * Time: 10:31 PM
 * To change this template use File | Settings | File Templates.
 */

import bean.UserEntity;
import org.hibernate.*;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;

import java.util.Iterator;
import java.util.List;

public class UserDAOImpl extends HibernateDAO implements UserDAO {
    private Configuration conf = null;
    //private SessionFactory sessionFactory = null;
    //private Session session = null;
    //private Transaction transaction = null;

    public UserDAOImpl() {
        try {
            conf = new Configuration();
            conf.addResource("hibernate.cfg.xml");
            conf.configure();

            ServiceRegistry serviceRegistry = new ServiceRegistryBuilder().applySettings(conf.getProperties()).buildServiceRegistry();
            sessionFactory = conf.buildSessionFactory(serviceRegistry);
        }
        catch (HibernateException ex) {
            System.out.println("Hibernate exception during start.");
        }
    }

    public void create(int id, String userName,String password, String eMail){
        try {
            beginTransaction();

            UserEntity user = new UserEntity();
            user.setId(id);
            user.setUsername(userName);
            user.setPassword(password);
            user.setEmail(eMail);

            session.save(user);
            endTransaction(true);
        }
        catch (HibernateException ex) {
            rollback();
            System.out.println("Hibernate exception during creating user.");
        }
        finally{
            closeSession();
        }
    }


    public void searchId(int id){
        try {
            beginTransaction();

            Query query = getSession().createQuery("from UserEntity where id = ?");
            query.setInteger(0,id);
            List list = query.list();

            if(list.size()>0){
                UserEntity user = (UserEntity)list.get(0);
                System.out.println("ID: " + user.getId() + ", " + user.getUsername() + " ( " + user.getPassword() + ") , " + user.getEmail());
                System.out.println("---------------------------------------------------------------------");
            }
            else{
                System.out.println("No matched record.");
            }
            endTransaction(true);
        }
        catch (HibernateException ex) {
            rollback();
            System.out.println("Hibernate exception during searching via id.");
        }
        finally{
            closeSession();
        }
    }


    public void searchName(String name){
        try {
            beginTransaction();

            Query query = getSession().createQuery("from UserEntity where username like ?");
            String str = "%"+name+"%";
            query.setString(0,str);
            List list = query.list();
            System.out.println("Search username by: " + name);

            if(list.size()>0){
                for(int i=0;i<list.size();i++){
                    UserEntity user = (UserEntity)list.get(i);
                    System.out.println("ID: " + user.getId() + ", " + user.getUsername() + " ( " + user.getPassword() + ") , " + user.getEmail());
                }
                System.out.println("---------------------------------------------------------------------");
            }
            else{
                System.out.println("No matched record.");
            }
            endTransaction(true);
        }
        catch (HibernateException ex) {
            rollback();
            System.out.println("Hibernate exception during searching via name.");
        }
        finally{
            closeSession();
        }
    }


    public void listAll(){
        try {
            beginTransaction();

            Query query = getSession().createQuery("from UserEntity ");
            List list = query.list();

            Iterator it = list.iterator();
            while(it.hasNext()){
                UserEntity user=(UserEntity)it.next();
                System.out.println("ID: " + user.getId() + ", " + user.getUsername() + " ( " + user.getPassword() + ") , " + user.getEmail());
            }
            System.out.println("---------------------------------------------------------------------");
            endTransaction(true);
        }
        catch (HibernateException ex) {
            rollback();
            System.out.println("Hibernate exception during listing all records.");
        }
        finally{
            closeSession();
        }
    }


    public void update(int id, String password, String email){
        try {
            beginTransaction();

            Query query = getSession().createQuery("from UserEntity where id=?");
            query.setInteger(0,id);
            List list = query.list();

            if(list.size()>0){
                UserEntity user = (UserEntity)list.get(0);
                user.setPassword(password);
                user.setEmail(email);
                session.update(user);
                session.flush();
                endTransaction(true);

                System.out.println("Record successful updated.");
                System.out.println("ID: " + user.getId() + ", " + user.getUsername() + " ( " + user.getPassword() + ") , " + user.getEmail());
                System.out.println("---------------------------------------------------------------------");
            }
            else{
                System.out.println("Fail to update, no record.");
            }
        }
        catch (HibernateException ex) {
            rollback();
            System.out.println("Hibernate exception during updating.");
        }
        finally{
            closeSession();
        }
    }


    public void delete(int id){
        try {
            beginTransaction();

            Query query = getSession().createQuery("from UserEntity where id = ?");
            query.setInteger(0,id);
            List list = query.list();
            if(list.size()>0){
                UserEntity user=(UserEntity)list.get(0);
                session.delete(user);
                session.flush();
                endTransaction(true);
                System.out.println("Record successful deleted.");
            }
            else{
                System.out.println("Fail to delete, no record.");
            }
        }
        catch (HibernateException ex) {
            rollback();
            System.out.println("Hibernate exception during deleting a record.");
        }
        finally{
            closeSession();
        }
    }
}
