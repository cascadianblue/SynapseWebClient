package org.sagebionetworks.web.client.widget.header;

import java.util.List;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.file.DownloadList;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.web.client.ClientProperties;
import org.sagebionetworks.web.client.DateTimeUtilsImpl;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.PortalGinInjector;
import org.sagebionetworks.web.client.SynapseJSNIUtils;
import org.sagebionetworks.web.client.SynapseJavascriptClient;
import org.sagebionetworks.web.client.cookie.CookieKeys;
import org.sagebionetworks.web.client.cookie.CookieProvider;
import org.sagebionetworks.web.client.events.DownloadListUpdatedEvent;
import org.sagebionetworks.web.client.place.Home;
import org.sagebionetworks.web.client.place.LoginPlace;
import org.sagebionetworks.web.client.place.Trash;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.widget.entity.FavoriteWidget;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.binder.EventHandler;


public class Header implements HeaderView.Presenter, IsWidget {

	public static final String SYNAPSE = "SYNAPSE";
	public static final String N_A = "n/a";
	public static final String ANONYMOUS = "";
	public static final String SYNAPSE_ORG = "@synapse.org";
	public static final String GET_SATISFACTION_SUPPORT_SITE = "http://support.sagebase.org";
	public static final String WWW_SYNAPSE_ORG = "www.synapse.org";

	public static enum MenuItems {
		DATASETS, TOOLS, NETWORKS, PEOPLE, PROJECTS
	}

	private HeaderView view;
	private AuthenticationController authenticationController;
	private GlobalApplicationState globalApplicationState;
	private SynapseJavascriptClient jsClient;
	private FavoriteWidget favWidget;
	private SynapseJSNIUtils synapseJSNIUtils;
	PortalGinInjector portalGinInjector;
	CookieProvider cookies;
	public static boolean isShowingPortalAlert = false;
	public static JSONObjectAdapter portalAlertJson = null;

	@Inject
	public Header(HeaderView view, AuthenticationController authenticationController, GlobalApplicationState globalApplicationState, SynapseJavascriptClient jsClient, FavoriteWidget favWidget, SynapseJSNIUtils synapseJSNIUtils, PortalGinInjector portalGinInjector, EventBus eventBus, CookieProvider cookies, JSONObjectAdapter jsonObjectAdapter) {
		this.view = view;
		this.authenticationController = authenticationController;
		this.globalApplicationState = globalApplicationState;
		this.jsClient = jsClient;
		this.favWidget = favWidget;
		this.cookies = cookies;
		favWidget.setLoadingSize(16);
		this.synapseJSNIUtils = synapseJSNIUtils;
		this.portalGinInjector = portalGinInjector;
		view.clear();
		
		view.setPresenter(this);
		initStagingAlert();
		view.getEventBinder().bindEventHandlers(this, eventBus);
		if (cookies.getCookie(CookieKeys.COOKIES_ACCEPTED) == null) {
			view.setCookieNotificationVisible(true);
		} else {
			view.setCookieNotificationVisible(false);
		}
		// portal alert state sticks around for entire app session
		String portalAlertString = cookies.getCookie(CookieKeys.PORTAL_CONFIG);
		isShowingPortalAlert = portalAlertString != null;
		if (isShowingPortalAlert) {
			cookies.removeCookie(CookieKeys.PORTAL_CONFIG);
			try {
				portalAlertJson = jsonObjectAdapter.createNew(portalAlertString);
			} catch (JSONObjectAdapterException e) {
				synapseJSNIUtils.consoleError(e);
			}
		} else {
			portalAlertJson = null;
		}
		view.setPortalAlertVisible(isShowingPortalAlert, portalAlertJson);

	}

	public void initStagingAlert() {
		String hostName = synapseJSNIUtils.getCurrentHostName().toLowerCase();
		boolean visible = !hostName.contains(WWW_SYNAPSE_ORG);
		view.setStagingAlertVisible(visible);
	}

	public void configure() {
		view.setProjectHeaderText(SYNAPSE);
		view.setProjectHeaderAnchorTarget("#");
		view.hideProjectFavoriteWidget();
	}

	public void configure(EntityHeader projectHeader) {
		String projectId = projectHeader.getId();
		favWidget.configure(projectId);
		view.setProjectHeaderAnchorTarget("#!Synapse:" + projectId);
		view.setProjectHeaderText(projectHeader.getName());
		view.showProjectFavoriteWidget();
	}

	public Widget asWidget() {
		view.setPresenter(this);
		return view.asWidget();
	}

	public void setSearchVisible(boolean searchVisible) {
		view.setSearchVisible(searchVisible);
	}

	public void refresh() {
		view.setPortalAlertVisible(isShowingPortalAlert, portalAlertJson);
		UserProfile profile = authenticationController.getCurrentUserProfile();
		view.setUser(profile);
		view.refresh();
		view.setSearchVisible(true);
		view.setProjectFavoriteWidget(favWidget);
		if (authenticationController.isLoggedIn() && profile != null) {
			refreshFavorites();
			onDownloadListUpdatedEvent(null);
		} else {
			view.setDownloadListUIVisible(false);
		}
	}

	@Override
	public void onTrashClick() {
		globalApplicationState.getPlaceChanger().goTo(new Trash(ClientProperties.DEFAULT_PLACE_TOKEN));
	}

	@Override
	public void onLogoutClick() {
		// explicitly logging out, clear the current and last place.
		globalApplicationState.clearLastPlace();
		globalApplicationState.getPlaceChanger().goTo(new LoginPlace(LoginPlace.LOGOUT_TOKEN));
	}

	@Override
	public void onLogoClick() {
		globalApplicationState.getPlaceChanger().goTo(new Home(ClientProperties.DEFAULT_PLACE_TOKEN));
	}

	@Override
	public void onLoginClick() {
		globalApplicationState.getPlaceChanger().goTo(new LoginPlace(LoginPlace.LOGIN_TOKEN));
	}

	@Override
	public void refreshFavorites() {
		if (authenticationController.isLoggedIn()) {
			jsClient.getFavorites(new AsyncCallback<List<EntityHeader>>() {
				@Override
				public void onSuccess(List<EntityHeader> favorites) {
					view.clearFavorite();
					globalApplicationState.setFavorites(favorites);
					if (favorites == null || favorites.size() == 0) {
						view.setEmptyFavorite();
					} else {
						view.addFavorite(favorites);
					}
				}

				@Override
				public void onFailure(Throwable caught) {
					view.clearFavorite();
					view.setEmptyFavorite();
				}
			});
		}
	}

	@EventHandler
	public void onDownloadListUpdatedEvent(DownloadListUpdatedEvent event) {
		// update Download List count, show if > 0
		jsClient.getDownloadList(new AsyncCallback<DownloadList>() {
			@Override
			public void onFailure(Throwable caught) {
				view.setDownloadListUIVisible(false);
				synapseJSNIUtils.consoleError("Unable to get download list! " + caught.getMessage());
			}

			public void onSuccess(DownloadList downloadList) {
				int count = downloadList.getFilesToDownload().size();
				view.setDownloadListUIVisible(count > 0);
				view.setDownloadListFileCount(count);
			};
		});
	}

	@Override
	public void onCookieNotificationDismissed() {
		view.setCookieNotificationVisible(false);
		cookies.setCookie(CookieKeys.COOKIES_ACCEPTED, Boolean.TRUE.toString(), DateTimeUtilsImpl.getYearFromNow());
	}
}
