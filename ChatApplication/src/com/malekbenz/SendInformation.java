package com.malekbenz;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import javax.websocket.EncodeException;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

class MessageInfo {
	private String Sensor; 
	public String getSensor() {
		return Sensor;
	}
	public void setSensor(String sensor) {
		Sensor = sensor;
	}
	public String getMessage() {
		return Message;
	}
	public void setMessage(String message) {
		Message = message;
	}
	private String Message ;
	public MessageInfo(String sensor, String message) {
			Sensor = sensor;
			Message = message;
		}
}

@ServerEndpoint("/messages")
public class SendInformation {
	private static int messageCounter = 0;
	private static final Set<Session> Sessions = Collections.synchronizedSet(new HashSet<Session>());

	private static final HashMap<Integer, MessageInfo> Messages = new HashMap<Integer, MessageInfo>();

	
	private static final HashMap<String, Integer> sensorsHistory = new HashMap<String, Integer>();

	@OnOpen
	public void onOpen(Session session) {
		Sessions.add(session);
	}

	@OnClose
	public void onClose(Session session) {
		String sensorName = getSensorName(session);
		Sessions.remove(session);
		sendServerEvent(sensorName, "sensorRemoved");
	}

	String getSensorName(Session sensor) {
		return ((String) (sensor.getUserProperties().get("sensor")));
	}

	boolean isSensorConnect(Session sensor) {
		return (getSensorName(sensor) != null);
	}

	public void sendServerEvent(String sensorName, String event) {
		String msg = getJsonMessage("server", sensorName.toUpperCase(), event);

		Iterator<Session> iter = Sessions.iterator();

		while (iter.hasNext()) {
			Session sensor = (Session) iter.next();

			try {
				if (isSensorConnect(sensor)) {
					sensor.getBasicRemote().sendText(msg);
				}

			} catch (IOException e) {
			}

		}

	}

	public String getUserNameOrCreate(String message, Session sensor) throws IOException, EncodeException {
		String sensorName = getSensorName(sensor);

		if (!isSensorConnect(sensor)) {
			sensor.getUserProperties().put("sensor", message);
		}

		return sensorName;
	}

	public void addToSensorHistory(String sensorName) {
		sensorsHistory.put(sensorName, messageCounter);
	}

	public void newSensorConnected(Session sensor, String sensorName) throws IOException, EncodeException {

		sensorName = sensorName.toUpperCase();
		sensor.getUserProperties().put("sensor", sensorName);

		sendServerEvent(sensorName, "sensorAdded");

		String msgJsonListConnectedSensors = getJsonMessage("server", getJsonListConnectedSensors(),
				"connectedSensors");

		String msgJsonMissingMessage = getJsonMessage("server", getJsonListAllMessagesFrom(sensor),
				"missingMessage");

		sendMessageTo(sensor, msgJsonListConnectedSensors, false);
		sendMessageTo(sensor, msgJsonMissingMessage, true);
		

	}

	@OnMessage
	public void message(String message, Session sensor) throws IOException, EncodeException {

		if (isSensorConnect(sensor))
			broadcastMessage(getSensorName(sensor), message);
		else
			newSensorConnected(sensor, message);

	}

	private void sendMessageTo(Session sensor, String message, boolean isAddToHistory) {
		if (isSensorConnect(sensor))
			try {
				sensor.getBasicRemote().sendText(message);
				if (isAddToHistory)
					addToSensorHistory(getSensorName(sensor));

			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	private void broadcastMessage(String sender, String message) {

		String jsonMessage = getJsonMessage(sender, message);
		Messages.put(messageCounter++,  new MessageInfo( sender, message));

		Sessions.forEach(sensor -> {
			sendMessageTo(sensor, jsonMessage, true);
		});

		String allMessagesWithMessageId = getJsonMessage("server", getJsonListAllSensors(), "allMessages");
		Sessions.forEach(sensor -> {
			sendMessageTo(sensor, allMessagesWithMessageId, false);
		});

//		String allMessages = getJsonMessage("server", getJsonListAllMessagesFrom(2), "allMessages");
//		Sessions.forEach(sensor -> {
//			sendMessageTo(sensor, allMessages, false);
//		});

	}

	private String getJsonMessage(String sender, String message) {
		return getJsonMessage(sender, message, "message");
	}

	private String getJsonMessage(String sender, String message, String type) {
		JsonObjectBuilder jo = Json.createObjectBuilder().add("payload", message).add("sender", sender).add("type",
				type);

		return jo.build().toString();
	}

	private String getJsonListConnectedSensors() {
		JsonArrayBuilder jsonListConnectedSensors = Json.createArrayBuilder();
		Sessions.stream().filter(this::isSensorConnect).forEach(sensor -> {
			jsonListConnectedSensors.add(getSensorName(sensor));
		});
		return jsonListConnectedSensors.build().toString();
	}

	private String getJsonListAllSensors() {
		JsonArrayBuilder jsonListConnectedSensors = Json.createArrayBuilder();
		sensorsHistory.forEach((sensorName, lastMessageId) -> {
			jsonListConnectedSensors.add(sensorName + " " + lastMessageId);
		});

		return jsonListConnectedSensors.build().toString();
	}

	private String getJsonListAllMessagesFrom(Session sensor) {
		JsonArrayBuilder jsonListConnectedSensors = Json.createArrayBuilder();
		int lastMessageID =  sensorsHistory.get(getSensorName(sensor));
		Messages
				.entrySet()
				.stream()
				.filter(map -> map.getKey().intValue() >= lastMessageID)
				.map((map) -> map.getValue())
				.collect(Collectors.toList()).forEach((messageInfo) -> {
					jsonListConnectedSensors.add(getJsonMessage(messageInfo.getSensor(),messageInfo.getMessage()));
				});

		return jsonListConnectedSensors.build().toString();
	}
}
