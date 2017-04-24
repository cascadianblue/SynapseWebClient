package org.sagebionetworks.web.client.widget.entity.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.utils.CallbackP;
import org.sagebionetworks.web.client.widget.WidgetEditorPresenter;
import org.sagebionetworks.web.client.widget.entity.WikiAttachments;
import org.sagebionetworks.web.client.widget.entity.dialog.DialogCallback;
import org.sagebionetworks.web.client.widget.upload.FileUpload;
import org.sagebionetworks.web.client.widget.upload.ImageUploadWidget;
import org.sagebionetworks.web.shared.WidgetConstants;
import org.sagebionetworks.web.shared.WikiPageKey;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class ImageConfigEditor implements ImageConfigView.Presenter, WidgetEditorPresenter {
	
	private ImageConfigView view;
	private Map<String, String> descriptor;
	private FileUpload file;
	private ImageUploadWidget fileInputWidget;
	private DialogCallback dialogCallback;
	private WikiAttachments wikiAttachments;
	private List<String> fileHandleIds;
	
	@Inject
	public ImageConfigEditor(ImageConfigView view, ImageUploadWidget fileInputWidget, WikiAttachments wikiAttachments) {
		this.view = view;
		view.setPresenter(this);
		this.fileInputWidget = fileInputWidget;
		this.wikiAttachments = wikiAttachments;
		view.setFileInputWidget(fileInputWidget.asWidget());
		view.setWikiAttachmentsWidget(wikiAttachments.asWidget());
	}
	
	@Override
	public void configure(WikiPageKey wikiKey, Map<String, String> widgetDescriptor, final DialogCallback dialogCallback) {
		fileHandleIds = new ArrayList<String>();
		descriptor = widgetDescriptor;
		this.dialogCallback = dialogCallback;
		this.file = null;
		view.initView();
		fileInputWidget.reset();
		fileInputWidget.configure(new CallbackP<FileUpload>() {
			@Override
			public void invoke(FileUpload fileUpload) {
				view.showUploadSuccessUI();
				//enable the ok button
				dialogCallback.setPrimaryEnabled(true);
				file = fileUpload;
				fileHandleIds.add(file.getFileHandleId());
			}
		});
		if (wikiKey != null) {
			wikiAttachments.configure(wikiKey);
		}
		
		if (descriptor.containsKey(WidgetConstants.IMAGE_WIDGET_FILE_NAME_KEY)) {
			// existing attachment
			view.setUploadTabVisible(false);
			view.showWikiAttachmentsTab();
			wikiAttachments.setSelectedFilename(descriptor.get(WidgetConstants.IMAGE_WIDGET_FILE_NAME_KEY));
		} else if (descriptor.containsKey(WidgetConstants.IMAGE_WIDGET_SYNAPSE_ID_KEY)) {
			// existing synapse id
			view.setUploadTabVisible(false);
			view.showSynapseTab();
			view.setSynapseId(descriptor.get(WidgetConstants.IMAGE_WIDGET_SYNAPSE_ID_KEY));
		} else {
			view.showUploadTab();
		}
		
		if (descriptor.containsKey(WidgetConstants.IMAGE_WIDGET_ALIGNMENT_KEY)) {
			view.setAlignment(descriptor.get(WidgetConstants.IMAGE_WIDGET_ALIGNMENT_KEY));
		}
		if (descriptor.containsKey(WidgetConstants.IMAGE_WIDGET_SCALE_KEY)) {
			view.setScale(Integer.parseInt(descriptor.get(WidgetConstants.IMAGE_WIDGET_SCALE_KEY)));
		}
	}

	public void configureWithoutUpload(WikiPageKey wikiKey, Map<String, String> widgetDescriptor,
			DialogCallback dialogCallback) {
		configure(wikiKey, widgetDescriptor, dialogCallback);
		view.setUploadTabVisible(false);
		view.setWikiAttachmentsTabVisible(false);
		if (!descriptor.containsKey(WidgetConstants.IMAGE_WIDGET_SYNAPSE_ID_KEY)) {
			view.showExternalTab();
		}
	}

	public void clearState() {
		view.clear();
	}

	@Override
	public Widget asWidget() {
		return view.asWidget();
	}

	@Override
	public void updateDescriptorFromView() {
		descriptor.clear();
		view.checkParams();
		if (!view.isExternal()) {
			if (view.isSynapseEntity()) {
				descriptor.put(WidgetConstants.IMAGE_WIDGET_SYNAPSE_ID_KEY, view.getSynapseId());
			} else if (view.isFromAttachments()) {
				if (!wikiAttachments.isValid()) {
					throw new IllegalArgumentException(DisplayConstants.ERROR_SELECT_ATTACHMENT_MESSAGE);
				}
				descriptor.put(WidgetConstants.IMAGE_WIDGET_FILE_NAME_KEY, wikiAttachments.getSelectedFilename());
			} else {
				if (file == null) {
					throw new IllegalArgumentException(DisplayConstants.IMAGE_CONFIG_UPLOAD_FIRST_MESSAGE);
				}
				descriptor.put(WidgetConstants.IMAGE_WIDGET_FILE_NAME_KEY, file.getFileMeta().getFileName());	
			}
				
			descriptor.put(WidgetConstants.IMAGE_WIDGET_ALIGNMENT_KEY, view.getAlignment());
			descriptor.put(WidgetConstants.IMAGE_WIDGET_SCALE_KEY, view.getScale().toString());
			descriptor.put(WidgetConstants.IMAGE_WIDGET_RESPONSIVE_KEY, Boolean.TRUE.toString());
		}
	}
	
	@Override
	public String getTextToInsert() {
		if (view.isExternal())
			return "!["+view.getAltText()+"]("+view.getImageUrl()+")";
		else return null;
	}

	@Override
	public List<String> getDeletedFileHandleIds() {
		if (view.isFromAttachments()) {
			return wikiAttachments.getFilesHandlesToDelete();
		} else {
			return null;
		}
	}

	@Override
	public List<String> getNewFileHandleIds() {
		return fileHandleIds;
	}

	/*
	 * Private Methods
	 */
}
