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
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonWriter;
import javax.websocket.EncodeException;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import com.sun.corba.se.impl.encoding.OSFCodeSetRegistry.Entry;
import com.sun.javafx.collections.MappingChange.Map;
import com.sun.xml.bind.v2.schemagen.xmlschema.List;

@ServerEndpoint("/messagingendpoint")
public class SendInformation {

	private static final Set<Session> sensorSession = Collections.synchronizedSet(new HashSet<Session>());

	private static final Collection<String> Messages = new ArrayList<String>();

	@OnOpen
	public void onOpen(Session session) {

		sensorSession.add(session);

	}

	@OnClose
	public void onClose(Session session) {
		sensorSession.remove(session);
	}

	@OnMessage
	public void message(String message, Session client) throws IOException, EncodeException {

		String username = (String) client.getUserProperties().get("sensor");
		if (username == null) {
			client.getUserProperties().put("sensor", message);
			client.getBasicRemote().sendText(buildArrayData(message));
		} else {
			Iterator<Session> iter = sensorSession.iterator();
			
			String msg = buildData(username, message);
			Messages.add(msg);
			
			while (iter.hasNext()) {
				Session session = (Session) iter.next();
				session.getBasicRemote().sendText(buildData(username, message));

			}
		}
	}

	private String buildData(String username, String message) {
		JsonObject jo = Json.createObjectBuilder().add("message", username + ":" + message).build();
		StringWriter sw = new StringWriter();
		JsonWriter jsw = Json.createWriter(sw);
		jsw.write(jo);
		return sw.toString();
	}

	private String buildArrayData(String username) {
		JsonArrayBuilder messagesArray = Json.createArrayBuilder();
		
		messagesArray.add(buildData("Server", "sensor is connected as>" + username.toUpperCase()));
		
		Messages.forEach(msg -> messagesArray.add(  msg));
		return messagesArray.build().toString();
	}
	
	
}
