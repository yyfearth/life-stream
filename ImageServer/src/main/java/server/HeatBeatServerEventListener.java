package server;

public interface HeatBeatServerEventListener {
	void onConnected(Object caller, HeatBeatServerEventArgs eventArgs);

	void onDisconnected(Object caller, HeatBeatServerEventArgs eventArgs);
}
