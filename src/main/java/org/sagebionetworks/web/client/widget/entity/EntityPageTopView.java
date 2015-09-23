package org.sagebionetworks.web.client.widget.entity;

import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.web.client.SynapseView;
import org.sagebionetworks.web.client.events.EntityDeletedEvent;
import org.sagebionetworks.web.client.place.Synapse;
import org.sagebionetworks.web.client.place.Synapse.EntityArea;
import org.sagebionetworks.web.client.widget.table.TableRowHeader;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;

public interface EntityPageTopView extends IsWidget, SynapseView {

	/**
	 * Set the presenter.
	 * @param presenter
	 */
	public void setPresenter(Presenter presenter);

	public void setEntityBundle(EntityBundle bundle, UserProfile userProfile,
			String entityTypeDisplay, 
			Long versionNumber, Synapse.EntityArea area, String areaToken,
			EntityHeader projectHeader,
			String wikiPageId);

	/**
	 * Reconfigure project action menu when navigating to a different wiki.
	 * 
	 * @param bundle
	 * @param wikiPageId
	 */
	void configureProjectActionMenu(EntityBundle bundle, String wikiPageId);
	void setProjectMetadata(Widget w);
	void setTabs(Widget w);
	
	/**
	 * Presenter interface
	 */
	public interface Presenter {

		void refresh();

		/**
		 * Changes the current active area
		 * @param area
		 */
		void setArea(EntityArea area, String areaToken);

		void fireEntityUpdatedEvent();

		boolean isLoggedIn();

		String createEntityLink(String id, String version, String display);

		void entityDeleted(EntityDeletedEvent event);
	}
}
