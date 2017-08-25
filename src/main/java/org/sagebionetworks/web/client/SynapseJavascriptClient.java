package org.sagebionetworks.web.client;

import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.utils.CallbackP;
import org.sagebionetworks.web.shared.WebConstants;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;

public class SynapseJavascriptClient {
	RequestBuilderWrapper requestBuilderForGet;
	AuthenticationController authController;
	JSONObjectAdapter jsonObjectAdapter;
	GlobalApplicationState globalAppState;
	
	private static final String ENTITY_URI_PATH = "/entity";
	private static final String ENTITY_BUNDLE_PATH = "/bundle?mask=";
	private static final String CONTENT_TYPE = "Content-Type";
	private static final String ACCEPT = "Accept";
	private static final String SESSION_TOKEN_HEADER = "sessionToken";
	private static final String USER_AGENT = "User-Agent";
	private static final String SYNAPSE_ENCODING_CHARSET = "UTF-8";
	private static final String APPLICATION_JSON_CHARSET_UTF8 = "application/json; charset="+SYNAPSE_ENCODING_CHARSET;
	public String repoServiceUrl; 
	@Inject
	public SynapseJavascriptClient(
			RequestBuilderWrapper requestBuilder,
			AuthenticationController authController,
			JSONObjectAdapter jsonObjectAdapter,
			GlobalApplicationState globalAppState) {
		this.requestBuilderForGet = requestBuilder;
		this.authController = authController;
		this.jsonObjectAdapter = jsonObjectAdapter;
		repoServiceUrl = globalAppState.getSynapseProperty(WebConstants.REPO_SERVICE_URL_KEY);
	}
	
	private void doGet(String url, final AsyncCallback<JSONObjectAdapter> callback) {
		requestBuilderForGet.configure(RequestBuilder.GET, url);
		requestBuilderForGet.setHeader(ACCEPT, APPLICATION_JSON_CHARSET_UTF8);
		if (authController.isLoggedIn()) {
			requestBuilderForGet.setHeader(SESSION_TOKEN_HEADER, authController.getCurrentUserSessionToken());
		}
		
		try {
			requestBuilderForGet.sendRequest(null, new RequestCallback() {
				@Override
				public void onResponseReceived(Request request,
						Response response) {
					int statusCode = response.getStatusCode();
					if (statusCode == Response.SC_OK) {
						try {
							JSONObjectAdapter jsonObject = jsonObjectAdapter.createNew(response.getText());
							callback.onSuccess(jsonObject);
						} catch (JSONObjectAdapterException e) {
							onError(null, e);
						}
					} else {
						onError(request, new IllegalArgumentException("Unable to make call. Reason: " + response.getStatusText()));
					}
				}

				@Override
				public void onError(Request request, Throwable exception) {
					callback.onFailure(exception);
				}
			});
		} catch (final Exception e) {
			callback.onFailure(e);
		}
	}
	
	public AsyncCallback<JSONObjectAdapter> wrapCallback(final CallbackP<JSONObjectAdapter> constructCallback, final AsyncCallback callback) {
		return  new AsyncCallback<JSONObjectAdapter>() {
			@Override
			public void onFailure(Throwable caught) {
				callback.onFailure(caught);
			}
			@Override
			public void onSuccess(JSONObjectAdapter json) {
				constructCallback.invoke(json);
			}
		};
	}
	
	public void getEntityBundle(String entityId, int partsMask, final AsyncCallback<EntityBundle> callback) {
		String url = repoServiceUrl + ENTITY_URI_PATH + "/"+ entityId + ENTITY_BUNDLE_PATH + partsMask;
		CallbackP<JSONObjectAdapter> constructCallback = new CallbackP<JSONObjectAdapter>() {
			@Override
			public void invoke(JSONObjectAdapter json) {
				try {
					callback.onSuccess(new EntityBundle(json));
				} catch (JSONObjectAdapterException e) {
					callback.onFailure(e);
				}
			}
		};
		doGet(url, wrapCallback(constructCallback, callback));
	}
}
