package org.sagebionetworks.web.client.widget.team;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.model.MembershipInvtnSubmission;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.SynapseClientAsync;
import org.sagebionetworks.web.client.utils.Callback;
import org.sagebionetworks.web.client.widget.entity.controller.SynapseAlert;
import org.sagebionetworks.web.shared.OpenTeamInvitationBundle;
import org.sagebionetworks.web.shared.exceptions.NotFoundException;

import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class OpenUserInvitationsWidget implements OpenUserInvitationsWidgetView.Presenter {

	public static final Integer INVITATION_BATCH_LIMIT = 10;
	private OpenUserInvitationsWidgetView view;
	private GlobalApplicationState globalApplicationState;
	private SynapseClientAsync synapseClient;
	private String teamId;
	private Callback teamRefreshCallback;
	private Integer currentOffset;
	private List<UserProfile> profiles;
	private List<MembershipInvtnSubmission> invitations;
	private SynapseAlert synAlert;
	@Inject
	public OpenUserInvitationsWidget(OpenUserInvitationsWidgetView view, 
			SynapseClientAsync synapseClient, 
			GlobalApplicationState globalApplicationState,
			SynapseAlert synAlert) {
		this.view = view;
		this.synAlert = synAlert;
		view.setPresenter(this);
		view.setSynAlert(synAlert);
		this.synapseClient = synapseClient;
		this.globalApplicationState = globalApplicationState;
	}
	
	public void clear() {
		view.clear();
	}

	@Override
	public void removeInvitation(String invitationId) {
		synAlert.clear();
		synapseClient.deleteMembershipInvitation(invitationId, new AsyncCallback<Void>() {
			@Override
			public void onSuccess(Void result) {
				teamRefreshCallback.invoke();
			}
			
			@Override
			public void onFailure(Throwable caught) {
				synAlert.handleException(caught);
			}
		});
	}
	
	public void configure(String teamId, Callback teamRefreshCallback) {
		this.teamId = teamId;
		this.teamRefreshCallback = teamRefreshCallback;
		profiles = new ArrayList<UserProfile>();
		invitations = new ArrayList<MembershipInvtnSubmission>();
		currentOffset = 0;
		getNextBatch();
	};

	@Override
	public void getNextBatch() {
		synAlert.clear();
		//using the given team, try to show all pending membership requests (or nothing if empty)
		synapseClient.getOpenTeamInvitations(teamId, INVITATION_BATCH_LIMIT, currentOffset, new AsyncCallback<ArrayList<OpenTeamInvitationBundle>>() {
			@Override
			public void onSuccess(ArrayList<OpenTeamInvitationBundle> result) {
				currentOffset += result.size();
				
				//create the associated object list, and pass to the view to render
				for (OpenTeamInvitationBundle b : result) {
					invitations.add(b.getMembershipInvtnSubmission());
					profiles.add(b.getUserProfile());
				}
				view.configure(profiles, invitations);
				
				//show the more button if we maxed out the return results
				view.setMoreResultsVisible(result.size() == INVITATION_BATCH_LIMIT);
			}
			
			@Override
			public void onFailure(Throwable caught) {
				if (caught instanceof NotFoundException) {
					view.setMoreResultsVisible(false);
				} else {
					synAlert.handleException(caught);
				} 
			}
		});
	}
	
	@Override
	public void goTo(Place place) {
		globalApplicationState.getPlaceChanger().goTo(place);
	}
	
	public Widget asWidget() {
		return view.asWidget();
	}
	
}
