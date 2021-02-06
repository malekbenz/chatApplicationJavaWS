package com.malekbenz;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
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

@ServerEndpoint("/messages")
public class SendInformation {

	private static final Set<Session> sensorSession = Collections.synchronizedSet(new HashSet<Session>());

	private static final Collection<String> Messages = new ArrayList<String>();

	private static final Set<String> sensors = Collections.synchronizedSet(new HashSet<String>());

	@OnOpen
	public void onOpen(Session session) {
		sensorSession.add(session);
	}

	@OnClose
	public void onClose(Session session) {
		String sensorName = (String) session.getUserProperties().get("sensor");
		sensorSession.remove(session);
		sensors.remove(sensorName);
		sendServerEvent(sensorName, "sensorRemoved");
	}

	public void sendServerEvent(String sensorName, String event) {
		String msg = buildMessage("server", sensorName.toUpperCase(), event);

		Iterator<Session> iter = sensorSession.iterator();

		while (iter.hasNext()) {
			Session sensor = (Session) iter.next();

			try {
				if (((String) sensor.getUserProperties().get("sensor")) == null) {
					
				}
				sensor.getBasicRemote().sendText(msg);

			} catch (IOException e) {
			}

		}

	}

	public String getUserNameOrCreate(String message, Session client) throws IOException, EncodeException {
		String username = (String) client.getUserProperties().get("sensor");

		if (username == null) {
			client.getUserProperties().put("sensor", message);
		}

		return username;
	}

	@OnMessage
	public void message(String message, Session client) throws IOException, EncodeException {

		String sensorName = (String) client.getUserProperties().get("sensor");
		if (sensorName == null) {
			sensorName = message.toUpperCase();
			client.getUserProperties().put("sensor", sensorName);
			sensors.add(sensorName);
			client.getBasicRemote().sendText(buildArrayData(sensorName));

			sendServerEvent(sensorName, "sensorAdded");


			// envoyer la list des sensors
			String connectedSensors = buildMessage("server", getAllConnectedSensors(), "connectedSensors");
			client.getBasicRemote().sendText(connectedSensors);

		} else {
			sendMessage(sensorName, message);
		}
	}

	private void sendMessage(String username, String message) {

		String msg = buildMessage(username, message);

		Messages.add(msg);

		sensorSession.forEach(sensor -> {
			try {
				if ((String) sensor.getUserProperties().get("sensor") != null)
					sensor.getBasicRemote().sendText(msg);
			} catch (IOException e) {
			}
		});

	}

	private String buildMessage(String username, String message) {
		return buildMessage(username, message, "message");
	}

	private String buildMessage(String username, String message, String type) {
		JsonObjectBuilder jo = Json.createObjectBuilder().add("payload", message).add("username", username).add("type",
				type);

		return jo.build().toString();
	}

	private String buildArrayData(String username) {
		JsonArrayBuilder messagesArray = Json.createArrayBuilder();

		messagesArray.add(buildMessage("Server", "sensor is connected as>" + username.toUpperCase()));

		Messages.forEach(messagesArray::add);
		return messagesArray.build().toString();
	}

	private String getAllConnectedSensors() {
		JsonArrayBuilder messagesArray = Json.createArrayBuilder();
		Iterator<String> iter = sensors.iterator();

		while (iter.hasNext()) {
			String sensorName = iter.next();
			messagesArray.add(sensorName);
		}
		return messagesArray.build().toString();
	}
}
