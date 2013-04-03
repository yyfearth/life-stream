package server;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class MonitorReport {

	public InetSocketAddress getInetSocketAddress() {
		return inetSocketAddress;
	}

	public void setInetSocketAddress(InetSocketAddress inetSocketAddress) {
		this.inetSocketAddress = inetSocketAddress;
	}

	public boolean isConnected() {
		return isConnected;
	}

	InetSocketAddress inetSocketAddress;
	boolean isConnected;

	public List<IDisconnectHandler> IDisconnectHandlers = new ArrayList<>();

	public MonitorReport(InetSocketAddress inetSocketAddress) {
		this.inetSocketAddress = inetSocketAddress;
	}

	public static MonitorReport[] generateReports(InetSocketAddress[] inetSocketAddresses) {
		MonitorReport[] monitorReports = new MonitorReport[inetSocketAddresses.length];

		for (int i = 0; i < monitorReports.length; i++) {
			monitorReports[i] = new MonitorReport(inetSocketAddresses[i]);
		}

		return monitorReports;
	}

	public void addDisconnectHandler(IDisconnectHandler IDisconnectHandler) {
		IDisconnectHandlers.add(IDisconnectHandler);
	}

	public boolean removeDisconnectHandler(IDisconnectHandler IDisconnectHandler) {
		return IDisconnectHandlers.remove(IDisconnectHandler);
	}

	public void trigger() {
		for (IDisconnectHandler handlder : IDisconnectHandlers) {
			handlder.onDisconnected();
		}
	}
}

interface IDisconnectHandler {
	public void onDisconnected();
}
