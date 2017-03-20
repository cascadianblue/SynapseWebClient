package org.sagebionetworks.web.client.widget.accessrequirements.createaccessrequirement;

import com.google.gwt.user.client.ui.IsWidget;

/**
 * View shows the first step of the wizard
 * 
 * @author Jay
 *
 */
public interface CreateAccessRequirementStep1View extends IsWidget {
	
	void setSubjects(IsWidget w);
	boolean isACTAccessRequirementType(); 
	boolean isTermsOfUseAccessRequirementType();
	void setPresenter(Presenter p);
	String getEntityIds();
	String getTeamIds();
	void setAccessRequirementTypeSelectionVisible(boolean visible);
	
	/**
	 * Presenter interface
	 */
	public interface Presenter {
		void onSetEntities();
		void onSetTeams();
	}
}
