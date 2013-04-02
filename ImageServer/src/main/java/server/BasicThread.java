package server;

public abstract class BasicThread implements Runnable {

	public boolean isStopping() {
		return isStopping;
	}

	public void setStopping(boolean stopping) {
		isStopping = stopping;
	}

	boolean isStopping = false;
}
