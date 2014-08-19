package org.sagebionetworks.web.client.widget.entity;

import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.schema.adapter.AdapterFactory;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.PortalGinInjector;
import org.sagebionetworks.web.client.SynapseClientAsync;
import org.sagebionetworks.web.client.cache.ClientCache;
import org.sagebionetworks.web.client.widget.SynapseWidgetPresenter;

import com.google.gwt.user.client.ui.IsTreeItem;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class EntityTreeItem implements IsTreeItem, SynapseWidgetPresenter {

	private TreeItem treeItem;
	//private EntityHeader entityHeader;
	private EntityBadge entityBadge;
	PortalGinInjector ginInjector;
	
	@Inject
	public EntityTreeItem(PortalGinInjector ginInjector) { 
//			EntityIconsCache iconsCache,
//			SynapseClientAsync synapseClient,
//			AdapterFactory adapterFactory,
//			GlobalApplicationState globalAppState,
//			ClientCache clientCache) {
		//entityBadge = new EntityBadge(view, iconsCache, synapseClient, adapterFactory, globalAppState, clientCache);
		this.ginInjector = ginInjector;
		entityBadge = ginInjector.getEntityBadgeWidget();
	}
	
	public void configure(EntityHeader header, boolean isRootItem) {
		entityBadge.configure(header);
		treeItem = new TreeItem(asWidget());
		if (!isRootItem)
			treeItem.addStyleName("entityTreeItem padding-bottom-4-imp");
		else
			treeItem.addStyleName("entityTreeItem padding-bottom-4-imp padding-left-0-imp");
		//entityHeader = header;
	}

	@Override
	public Widget asWidget() {
		return entityBadge.asWidget();
	}

	@Override
	public TreeItem asTreeItem() {
		return treeItem;
	}
	
	public void setMakeLinks() {
		
	}
	
	public void showLoadingIcon() {
		entityBadge.showLoadingIcon();
	}
	
	public void showTypeIcon() {
		entityBadge.hideLoadingIcon();
	}
	
	public EntityHeader getHeader() {
		return entityBadge.getHeader();
	}
	
	public void setMakeLinks(boolean makeLinks) {
		entityBadge.setMakeLinks(makeLinks);
	}

}
