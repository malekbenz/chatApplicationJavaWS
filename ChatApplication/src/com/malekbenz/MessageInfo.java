package com.malekbenz;

public class MessageInfo {
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

	private String Message;

	public MessageInfo(String sensor, String message) {
		Sensor = sensor;
		Message = message;
	}
}
