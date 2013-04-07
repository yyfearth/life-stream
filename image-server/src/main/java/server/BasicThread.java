package server;

public abstract class BasicThread implements Runnable {

	public boolean isStopping() {
		return isStopping;
	}

	public void stop() {
		isStopping = true;
	}

	boolean isStopping = false;
}
