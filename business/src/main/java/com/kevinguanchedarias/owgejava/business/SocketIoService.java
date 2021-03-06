package com.kevinguanchedarias.owgejava.business;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import com.kevinguanchedarias.owgejava.dto.UserStorageDto;
import com.kevinguanchedarias.owgejava.dto.WebsocketMessageStatusDto;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.entity.WebsocketMessageStatus;
import com.kevinguanchedarias.owgejava.exception.CommonException;
import com.kevinguanchedarias.owgejava.exception.ConfigurationException;
import com.kevinguanchedarias.owgejava.exception.ProgrammingException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.exception.SocketIoException;
import com.kevinguanchedarias.owgejava.exception.SocketServerProgrammingException;
import com.kevinguanchedarias.owgejava.pojo.AbstractMissionReport;
import com.kevinguanchedarias.owgejava.pojo.DeliveryQueueEntry;
import com.kevinguanchedarias.owgejava.pojo.WebsocketMessage;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;

import io.socket.client.IO;
import io.socket.client.Socket;

@Service
public class SocketIoService implements Serializable {
	private static final long serialVersionUID = 8523658662096226104L;

	private static final String PROTOCOL_VERSION = "0.1.0";
	private static final Logger LOCAL_LOGGER = Logger.getLogger(SocketIoService.class);
	private static final String MESSAGE_STATUS_JSON_KEY = "status";
	private static final String DELIVER_EVENT_NAME = "deliver_message";

	@Autowired
	private ConfigurationBo configurationBo;

	@Autowired
	private WebsocketMessageBo websocketMessageBo;

	private DtoUtilService dtoUtilService = new DtoUtilService();
	private transient Socket io;
	private ObjectMapper mapper = new ObjectMapper();

	/**
	 * Represents the initializing promise, if defined, means an initializing
	 * proccess is going on
	 */
	private transient CompletableFuture<Boolean> initializingSocket;

	/**
	 * This field stores the Promises to keep trace of the <b>message
	 * delivery</b>
	 * 
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private transient Map<BigInteger, DeliveryQueueEntry> messageHandshakingStorage = new HashMap<>();

	/**
	 * Has the successfully delivered messages <br>
	 * The messages should be removed after first read
	 */
	private transient Map<BigInteger, DeliveryQueueEntry> completedDeliveryQueries = new HashMap<>();

	public SocketIoService() {
		mapper.registerModule(new JsonOrgModule());
	}

	/**
	 * Sends a websocket message to target user, the CompletableFuture solves
	 * when the all the delivery flow has end
	 * 
	 * @param sourceUser
	 *            Source user, if null, will be the system
	 * @param targetuser
	 *            Target user
	 * @param report
	 *            mission report
	 * @return If success will return the related {@link DeliveryQueueEntry},
	 *         else will return <b>null</b>
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public CompletableFuture<DeliveryQueueEntry> sendMissionReport(UserStorage sourceUser, UserStorage targetUser,
			AbstractMissionReport report) {
		return sendMessage(sourceUser, targetUser, report.getEventName(), report);
	}

	public CompletableFuture<DeliveryQueueEntry> sendMessage(UserStorage sourceUser, UserStorage targetUser,
			String eventName, Object messageContent) {
		CompletableFuture<DeliveryQueueEntry> retVal = new CompletableFuture<>();
		Map<String, Object> jsonRoot = new HashMap<>();
		if (sourceUser != null) {
			jsonRoot.put("sourceUser", mapper
					.convertValue(dtoUtilService.dtoFromEntity(UserStorageDto.class, sourceUser), JSONObject.class));
		}
		if (targetUser != null) {
			jsonRoot.put("targetUser", mapper
					.convertValue(dtoUtilService.dtoFromEntity(UserStorageDto.class, targetUser), JSONObject.class));
		}
		Object parsedContent;
		if (messageContent instanceof String || messageContent instanceof Number) {
			parsedContent = messageContent;
		} else {
			parsedContent = mapper.convertValue(messageContent, JSONObject.class);
		}

		jsonRoot.put("content", parsedContent);
		deliverMessage(eventName, targetUser, jsonRoot)
				.thenAccept(websocketMessageStatus -> retVal.complete(findCompletedAndRemove(websocketMessageStatus)));
		return retVal;
	}

	public CompletableFuture<DeliveryQueueEntry> sendMessage(UserStorage targetUser, String eventName,
			Object messageContent) {
		return sendMessage(null, targetUser, eventName, messageContent);
	}

	/**
	 * Sends a websocket message <b>as system </b> to target user, the
	 * CompletableFuture solves when the all the delivery flow has end
	 * 
	 * @param targetuser
	 *            Target user
	 * @param report
	 *            mission report
	 * @return If success will return the related {@link DeliveryQueueEntry},
	 *         else will return <b>null</b>
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public CompletableFuture<DeliveryQueueEntry> sendMissionReport(UserStorage targetuser,
			AbstractMissionReport report) {
		return sendMissionReport(null, targetuser, report);
	}

	private DeliveryQueueEntry findQueueEntry(BigInteger id) {
		return messageHandshakingStorage.get(id);
	}

	/**
	 * Stores the message in the pending to deliver collection
	 * 
	 * @param status
	 *            message to store
	 * @param completableStatus
	 *            Related promise
	 * @param data
	 *            Server data
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private DeliveryQueueEntry storeMessageStatus(WebsocketMessageStatus status,
			CompletableFuture<WebsocketMessageStatus> completableStatus, Map<String, Object> data) {
		if (status.getId() == null) {
			throw new ProgrammingException("messageStatus MUST have id prior to adding to delivery cache");
		}
		LOCAL_LOGGER.debug("Storing message with id " + status.getId());
		DeliveryQueueEntry deliveryQueueEntry = new DeliveryQueueEntry(completableStatus, data);
		return messageHandshakingStorage.put(status.getId(), deliveryQueueEntry);
	}

	private CompletableFuture<WebsocketMessageStatus> deliverMessage(String eventName, UserStorage user,
			Map<String, Object> data) {
		CompletableFuture<WebsocketMessageStatus> retVal = new CompletableFuture<>();
		WebsocketMessageStatus websocketMessageStatus = new WebsocketMessageStatus();
		websocketMessageStatus.setEventName(eventName);
		websocketMessageStatus.setUser(user);
		final WebsocketMessageStatus persistedMessage = websocketMessageBo.save(websocketMessageStatus);

		data.put(MESSAGE_STATUS_JSON_KEY,
				mapper.convertValue(convertMessageStatusToDto(websocketMessageStatus), JSONObject.class));
		storeMessageStatus(websocketMessageBo.save(persistedMessage), retVal, data);
		initializeSocketIo().thenAccept(status -> io.emit(DELIVER_EVENT_NAME, data));
		return retVal;
	}

	/**
	 * Initializes Socket IO if required
	 * 
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private CompletableFuture<Boolean> initializeSocketIo() {
		CompletableFuture<Boolean> retVal;
		if ((io == null || !io.connected()) && this.initializingSocket == null) {
			initializingSocket = new CompletableFuture<>();
			retVal = initializingSocket;
			try {
				io = IO.socket(
						configurationBo.findConfigurationParam(ConfigurationBo.WEBSOCKET_ENDPOINT_KEY).getValue());
			} catch (URISyntaxException e) {
				throw new ConfigurationException("Bad value returned from server", e);
			}
			io.on(Socket.EVENT_CONNECT,
					(Object... arg0) -> io.emit("authentication",
							toJson(new WebsocketMessage(fetchSystemToken(), PROTOCOL_VERSION))))
					.on(Socket.EVENT_CONNECT_ERROR, (Object... arg0) -> retVal.complete(false))
					.on("authentication", (Object... results) -> {
						LOCAL_LOGGER.debug("Authentication websocket event");
						JSONObject object = parseServerResult(results);
						checkErrorAndClose(object);
						initializingSocket.complete(true);
						initializingSocket = null;
					});
			registerAckEventHandlers();
			io.connect();
		} else if (io != null && io.connected()) {
			if (initializingSocket == null) {
				initializingSocket = new CompletableFuture<>();
			}
			retVal = initializingSocket;
			initializingSocket.complete(true);
			initializingSocket = null;
		} else {
			retVal = initializingSocket;
		}
		return retVal;
	}

	private void registerAckEventHandlers() {
		io.on("websocket_server_ack", (Object... args) -> {
			WebsocketMessageStatusDto sentStatus = checkWebsocketMessageStatus(parseServerResult(args));
			WebsocketMessageStatus savedStatus = checkMessageCorrelations(sentStatus);
			if (savedStatus.getSocketServerAck()) {
				LOCAL_LOGGER.warn("Message with id " + savedStatus.getId() + " was already notified");
			} else {
				LOCAL_LOGGER.debug("Websocket server ACKs the message");
				savedStatus.setSocketServerAck(true);
				websocketMessageBo.save(savedStatus);
				savedStatus.setUser(savedStatus.getUser().toBasicInformation());
				io.emit("deliver_to_client", mapper.convertValue(args[0], JSONObject.class));
			}
		});
		io.on("no_client_socket", (Object... args) -> {
			WebsocketMessageStatusDto sentStatus = checkWebsocketMessageStatus(parseServerResult(args));
			WebsocketMessageStatus savedStatus = checkMessageCorrelations(sentStatus);
			discardWebsocketMessage(savedStatus);
			if (savedStatus.getSocketServerAck()) {
				LOCAL_LOGGER.info("Message couldn't be sent, because no client is connected");
				savedStatus.setSocketNotFound(true);
				websocketMessageBo.save(savedStatus);
			} else {
				LOCAL_LOGGER.warn(
						"Unexpected message delivery flow error, should never invoke no_client_socket without having websocket_server_ack");
				discardWebsocketMessage(savedStatus);
			}
		});
		io.on("web_browser_ack", (Object... args) -> {
			WebsocketMessageStatusDto sentStatus = checkWebsocketMessageStatus(parseServerResult(args));
			WebsocketMessageStatus savedStatus = checkMessageCorrelations(sentStatus);
			if (savedStatus.getSocketServerAck()) {
				LOCAL_LOGGER.debug("The client browser is aware of the websocket message");
				savedStatus.setWebBrowserAck(true);
				websocketMessageBo.save(savedStatus);
			} else {
				LOCAL_LOGGER.warn(
						"Unexpected message delivery flow error, should never invoke web_browser_ack without having websocket_server_ack");
				discardWebsocketMessage(savedStatus);
			}
		});
		io.on("user_ack", (Object... args) -> {
			WebsocketMessageStatusDto sentStatus = checkWebsocketMessageStatus(parseServerResult(args));
			WebsocketMessageStatus savedStatus = checkMessageCorrelations(sentStatus);
			if (savedStatus.getSocketServerAck()) {
				LOCAL_LOGGER.debug("User acknowledges the message");
				DeliveryQueueEntry entry = findQueueEntry(savedStatus.getId());
				JSONObject clientData = findJsonEntryObject(parseServerResult(args), "clientData");
				try {
					entry.setClientData(toMap(clientData));
				} catch (JSONException e) {
					LOCAL_LOGGER.warn("Invalid JSON sent by client, data:" + clientData);
					LOCAL_LOGGER.debug("Will continue to deliver message, even on failed client JSON");
				}
				savedStatus.setUserAck(new Date());
				websocketMessageBo.save(savedStatus);
				completedDeliveryQueries.put(savedStatus.getId(), entry);
				entry.getRelatedPromise().complete(savedStatus);
			}
		});

	}

	/**
	 * Gets the authentication token for the system account
	 * 
	 * @return JWT encoded system token
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private String fetchSystemToken() {
		Map<String, String> response;
		try {
			response = mapper.readValue(
					post(configurationBo.findConfigurationParam(ConfigurationBo.ACCOUNT_ENDPOINT_KEY).getValue(),
							configurationBo.findConfigurationParam(ConfigurationBo.SYSTEM_EMAIL_KEY).getValue(),
							configurationBo.findConfigurationParam(ConfigurationBo.SYSTEM_SECRET_KEY).getValue()),
					new TypeReference<HashMap<String, String>>() {
					});
		} catch (IOException e) {
			throw new SgtBackendInvalidInputException("Account Server returned bad data", e);
		}
		return response.get("token");
	}

	private String post(String url, String systemEmail, String systemPassword) {
		HttpHeaders headers = new HttpHeaders();
		headers.set("Accept", "application/json");

		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url).queryParam("email", systemEmail)
				.queryParam("password", systemPassword);
		URI uri = builder.build().encode().toUri();

		HttpPost httpPost = new HttpPost(uri);
		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			CloseableHttpResponse response = httpClient.execute(httpPost);
			return new BasicResponseHandler().handleResponse(response);
		} catch (IOException e) {
			throw new CommonException("Couldn't retrieve token from Account server", e);
		}
	}

	private String toJson(Object inputObject) {
		try {
			return mapper.writeValueAsString(inputObject);
		} catch (JsonProcessingException e) {
			throw new CommonException("Could not convert object to JSON", e);
		}
	}

	/**
	 * Checks if the server response has an error,
	 * 
	 * @param response
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private boolean hasError(JSONObject response) {
		return findJsonEntry(response, MESSAGE_STATUS_JSON_KEY).equals("error");

	}

	/**
	 * Checks if there is an error, in case of true, will throw an exception,
	 * and will <b>close the socket</b>
	 * 
	 * @param response
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private void checkErrorAndClose(JSONObject response) {
		if (hasError(response)) {
			io.close();
			io = null;
			throw new SocketIoException(
					"Unexpected error sent by Websocket server: " + findJsonEntry(response, "value"));
		}
	}

	private String findJsonEntry(JSONObject source, String key) {
		try {
			return source.getString(key);
		} catch (JSONException e) {
			throw new SocketIoException("Could not decode JSON object", e);
		}
	}

	private JSONObject findJsonEntryObject(JSONObject source, String key) {
		try {
			return source.getJSONObject(key);
		} catch (JSONException e) {
			throw new SocketIoException("Chould not decode JSON object", e);
		}
	}

	private JSONObject parseServerResult(Object[] websocketServerResult) {
		return (JSONObject) websocketServerResult[0];
	}

	private <E> E convertJsonObjectToPojo(JSONObject input, Class<E> targetClass) {
		return mapper.convertValue(input, targetClass);
	}

	/**
	 * Detects suspicious differences between saved message status and sent one
	 * 
	 * @param sentStatus
	 *            Message sent by the socket server
	 * @return The found saved message
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private WebsocketMessageStatus checkMessageCorrelations(WebsocketMessageStatusDto sentStatus) {
		WebsocketMessageStatus savedStatus = websocketMessageBo.findById(sentStatus.getId());
		if (savedStatus == null) {
			throw new SocketServerProgrammingException("Message with id " + sentStatus.getId() + " is not registered");
		}
		return savedStatus;
	}

	private WebsocketMessageStatusDto checkWebsocketMessageStatus(JSONObject serverResult) {
		JSONObject statusJson = findJsonEntryObject(serverResult, MESSAGE_STATUS_JSON_KEY);
		if (statusJson == null) {
			throw new SocketServerProgrammingException("message doesn't contain a status object");
		}
		WebsocketMessageStatusDto status = convertJsonObjectToPojo(statusJson, WebsocketMessageStatusDto.class);
		if (status.getSocketServerAck() == null || status.getSocketNotFound() == null
				|| status.getWebBrowserAck() == null) {
			throw new SocketServerProgrammingException("message contain null values");
		}
		return status;
	}

	/**
	 * Converts a {@link JSONObject} to {@link HashMap}
	 * 
	 * @see https://stackoverflow.com/questions/21720759/convert-a-json-string-to-a-hashmap
	 * @param object
	 * @return
	 * @throws JSONException
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Object> toMap(JSONObject object) throws JSONException {
		Map<String, Object> map = new HashMap<>();

		Iterator<String> keysItr = object.keys();
		while (keysItr.hasNext()) {
			String key = keysItr.next();
			Object value = object.get(key);

			if (value instanceof JSONArray) {
				value = toList((JSONArray) value);
			}

			else if (value instanceof JSONObject) {
				value = toMap((JSONObject) value);
			}
			map.put(key, value);
		}
		return map;
	}

	/**
	 * Converts a {@link JSONObject} to {@link ArrayList}
	 * 
	 * @param array
	 * @return
	 * @throws JSONException
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private List<Object> toList(JSONArray array) throws JSONException {
		List<Object> list = new ArrayList<>();
		for (int i = 0; i < array.length(); i++) {
			Object value = array.get(i);
			if (value instanceof JSONArray) {
				value = toList((JSONArray) value);
			}

			else if (value instanceof JSONObject) {
				value = toMap((JSONObject) value);
			}
			list.add(value);
		}
		return list;
	}

	/**
	 * Discards a websocket message, so will never deliver it <br>
	 * 
	 * <b>NOTICE: Saves to database</b> <br>
	 * <b>Resolves the deliverMessage() promise </b>
	 * 
	 * @param status
	 *            target websocket message object
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private void discardWebsocketMessage(WebsocketMessageStatus status) {
		DeliveryQueueEntry entry = findQueueEntry(status.getId());
		status.setUnwhilingToDelivery(true);
		messageHandshakingStorage.remove(status.getId());
		websocketMessageBo.save(status);
		entry.getRelatedPromise().complete(status);
	}

	/**
	 * Returns the completed {@link DeliveryQueueEntry} and removes it from the
	 * completed storage <br>
	 * If the message was not send with success, will not be in the internal
	 * storage, in that case <b>this method is expected to return null</b>
	 * 
	 * @param status
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private DeliveryQueueEntry findCompletedAndRemove(WebsocketMessageStatus status) {
		DeliveryQueueEntry retVal = completedDeliveryQueries.get(status.getId());
		if (retVal != null) {
			completedDeliveryQueries.remove(status.getId());
		} else {
			LOCAL_LOGGER.debug("WebsocketMessage with id " + status.getId() + " was not succeed");
		}

		return retVal;
	}

	private WebsocketMessageStatusDto convertMessageStatusToDto(WebsocketMessageStatus status) {
		return dtoUtilService.dtoFromEntity(WebsocketMessageStatusDto.class, status);
	}
}
