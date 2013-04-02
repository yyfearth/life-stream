package data;

public class Resolution {
	double x;
	double y;

	Resolution() {
		this(0, 0);
	}

	Resolution(double x, double y) {
		this.x = x;
		this.y = y;
	}

	static final Resolution ORIGIN = new Resolution();

	public static Resolution getOrigin() {
		try {
			return (Resolution) ORIGIN.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return new Resolution();
		}
	}
}