/**
 * Created with IntelliJ IDEA.
 * User: Leo
 * Date: 3/4/13
 * Time: 4:05 PM
 * To change this template use File | Settings | File Templates.
 */
public interface UserDao {
	public void create(int id, String userName, String password, String eMail);

	public void searchId(int id);

	public void searchName(String name);

	public void listAll();

	public void update(int id, String password, String email);

	public void delete(int id);
}
