import bean.ImageEntity;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.joda.time.DateTime;

import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * User: wilson
 * Date: 3/31/13
 * Time: 6:09 PM
 */
public class Server {
	private static final SessionFactory sessionFactory;
	private static final GeometryFactory geometryFactory = new GeometryFactory();

	static {
		try {

			Configuration configuration = new Configuration();
			configuration.addResource("hibernate.cfg.xml");
			configuration.configure();

			ServiceRegistry serviceRegistry = new ServiceRegistryBuilder().applySettings(configuration.getProperties()).buildServiceRegistry();
			sessionFactory = configuration.buildSessionFactory(serviceRegistry);

		} catch (Throwable ex) {
			throw new ExceptionInInitializerError(ex);
		}
	}

	public static void main(final String[] args) throws Exception {
		Session session = sessionFactory.openSession();
		try {
			Transaction transaction = session.beginTransaction();
			ImageEntity imageEntity = new ImageEntity();
			imageEntity.setId(UUID.randomUUID());
			imageEntity.setName("test");
			imageEntity.setCreatedTimestamp(DateTime.now().toDate());
			imageEntity.setLength(0);
			imageEntity.setHeight(0);
			imageEntity.setWidth(0);
			imageEntity.setMime("image/png");
			imageEntity.setModifiedTimestamp(DateTime.now().toDate());
			imageEntity.setOriginalTimestamp(DateTime.now().toDate());
			Point point = geometryFactory.createPoint(new Coordinate(0, 0));
			imageEntity.setGeoLocation(point);
			session.save(imageEntity);
			transaction.commit();
		} finally {
			session.close();
		}
	}
}
