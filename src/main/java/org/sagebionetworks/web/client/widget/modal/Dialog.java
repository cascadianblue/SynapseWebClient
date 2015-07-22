package org.sagebionetworks.web.client.widget.modal;

import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalSize;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

/**
 * Lightweight Bootstrap modal dialog, uses gwtbootstrap3.
 * There is also zero business logic in this class therefore it is 100% "view" with no 
 * presenter.
 * 
 * @author jhodgson
 * 
 */
public class Dialog extends Composite {
	public interface DialogUiBinder extends UiBinder<Widget, Dialog> {}
	
	private Callback callback;

	@UiField
	FlowPanel mainContent;
	@UiField
	Button primaryButton;
	@UiField
	Button defaultButton;
	
	boolean autoHide;
	
	@UiField
	Modal modal;

	/**
	 * Create a new Modal dialog.
	 */
	@Inject
	public Dialog(DialogUiBinder binder) {		
		initWidget(binder.createAndBindUi(this));
		primaryButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if (callback != null)
					callback.onPrimary();
				if (autoHide)
					hide();
			}
		});
		defaultButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if (callback != null)
					callback.onDefault();
				if (autoHide)
					hide();
			}
		});
	}
	
	public void setSize(ModalSize modalSize) {
		modal.setSize(modalSize);
	}
	
	/**
	 * @param title The text shown in the title bar.
	 * @param body This will be the main body of the dialog.  It can be any GWT widget.
	 * @param primaryButtonText The text for the primary button (i.e "Save").  The primary button is highlighted.
	 * @param defaultButtonText The text for the default button (i.e "Cancel").  The default button will not be highlighted.  If null, will hide default button.
	 * @param callback
	 * @param autoHide if true, will hide the dialog on primary or default button click.
	 */
	public void configure(String title, Widget body, String primaryButtonText, String defaultButtonText, Callback callback, boolean autoHide) {
		this.autoHide = autoHide;
		mainContent.clear();
		this.callback = callback;
		boolean isPrimaryButtonVisible = primaryButtonText != null;
		primaryButton.setVisible(isPrimaryButtonVisible);
		if (isPrimaryButtonVisible)
			primaryButton.setText(primaryButtonText);
		
		boolean isDefaultButtonVisible = defaultButtonText != null;
		defaultButton.setVisible(isDefaultButtonVisible);
		if (isDefaultButtonVisible)
			defaultButton.setText(defaultButtonText);
		mainContent.add(body);
		modal.setTitle(title);
		modal.setHideOtherModals(false);
	}
	
	/**
	 * The Callback handles events generated by this modal dialog.
	 * 
	 */
	public interface Callback {
		/**
		 * Called when the primary button is pressed.
		 */
		public void onPrimary();

		/**
		 * Called when the default button is pressed.
		 */
		public void onDefault();
	}
	
	public Button getPrimaryButton() {
		return primaryButton;
	}
	public Button getDefaultButton() {
		return defaultButton;
	}
	
	public void show() {
		modal.show();
	}
	public void hide() {
		modal.hide();
	}
	
	public void addStyleName(String style) {
		modal.addStyleName(style);
	}
}
