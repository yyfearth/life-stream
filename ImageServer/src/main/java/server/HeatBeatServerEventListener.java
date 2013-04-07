package server;

public interface HeatBeatServerEventListener {
	void onConnected(HeartbeatServer server, HeatBeatServerEventArgs eventArgs);

	void onDisconnected(HeartbeatServer server, HeatBeatServerEventArgs eventArgs);

	void onClosed(HeartbeatServer server, HeatBeatServerEventArgs eventArgs);
}
