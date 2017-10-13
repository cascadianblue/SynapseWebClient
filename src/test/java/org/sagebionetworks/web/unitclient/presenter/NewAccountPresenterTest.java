package org.sagebionetworks.web.unitclient.presenter;

import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.principal.AccountCreationToken;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.EmailValidationSignedToken;
import org.sagebionetworks.web.client.*;
import org.sagebionetworks.web.client.place.LoginPlace;
import org.sagebionetworks.web.client.place.NewAccount;
import org.sagebionetworks.web.client.place.users.RegisterAccount;
import org.sagebionetworks.web.client.presenter.NewAccountPresenter;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.view.NewAccountView;
import org.sagebionetworks.web.client.widget.login.PasswordStrengthWidget;
import org.sagebionetworks.web.test.helper.AsyncMockStubber;

import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class NewAccountPresenterTest {
	
	NewAccountPresenter newAccountPresenter;
	NewAccountView mockView;
	UserAccountServiceAsync mockUserService;
	GlobalApplicationState mockGlobalApplicationState;
	@Mock
	GWTWrapper mockGWT;
	RegisterAccount place = Mockito.mock(RegisterAccount.class);
	SynapseClientAsync mockSynapseClient;
	AuthenticationController mockAuthController;
	PlaceChanger mockPlaceChanger;
	@Mock
	PasswordStrengthWidget mockPasswordStrengthWidget;
	
	String username = "Ms.Information";
	String firstName = "Hello";
	String lastName = "Goodbye";
	String testSessionToken = "1239381foobar";
	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		mockView = mock(NewAccountView.class);
		
		mockUserService = mock(UserAccountServiceAsync.class);
		mockGlobalApplicationState = mock(GlobalApplicationState.class);
		mockSynapseClient = mock(SynapseClientAsync.class);
		mockAuthController = mock(AuthenticationController.class);
		mockPlaceChanger = mock(PlaceChanger.class);
		newAccountPresenter = new NewAccountPresenter(mockView, mockSynapseClient, mockGlobalApplicationState, mockUserService, mockAuthController, mockGWT, mockPasswordStrengthWidget);
		verify(mockView).setPresenter(newAccountPresenter);
		
		AsyncMockStubber.callSuccessWith(true).when(mockSynapseClient).isAliasAvailable(anyString(), eq(AliasType.USER_NAME.toString()), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(true).when(mockSynapseClient).isAliasAvailable(anyString(), eq(AliasType.USER_EMAIL.toString()), any(AsyncCallback.class));
		when(mockGlobalApplicationState.getPlaceChanger()).thenReturn(mockPlaceChanger);
		
		AsyncMockStubber.callSuccessWith(testSessionToken).when(mockUserService).createUserStep2(anyString(), anyString(), anyString(), anyString(), anyString(), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(testSessionToken).when(mockUserService).createUserStep2(anyString(), anyString(), anyString(), anyString(), any(EmailValidationSignedToken.class), any(AsyncCallback.class));
	}
	
	
	@Test
	public void testSetPlace() {
		reset(mockView);
		NewAccount newPlace = Mockito.mock(NewAccount.class);
		when(newPlace.toToken()).thenReturn("");
		newAccountPresenter.setPlace(newPlace);
		verify(mockView).setPresenter(newAccountPresenter);
	}
	
	@Test
	public void testSetPlaceEncodedToken() {
		String token = "firstname=&amp;lastname=&amp;email=decode-test%40j.com&amp;timestamp=2016-08-11T22%3A21%3A16.111%2B0000&amp;domain=SYNAPSE&amp;mac=gLaTfdsfB0TSy6JMsfdsuA1k%3D";
		String decodedToken = "firstname=&lastname=&email=decode-test%40j.com&timestamp=2016-08-11T22%3A22%3A59.022%2B0000&domain=SYNAPSE&mac=EckukfdsGjPbzLVVaaaaLs%3D";
		NewAccount newPlace = Mockito.mock(NewAccount.class);
		when(newPlace.toToken()).thenReturn(token);
		when(mockGWT.decodeQueryString(token)).thenReturn(decodedToken);
		newAccountPresenter.setPlace(newPlace);
		assertEquals(decodedToken, newAccountPresenter.getEmailValidationToken());
	}

	@Test
	public void testIsUsernameAvailableTooSmall() {
		//should not check if too short
		newAccountPresenter.checkUsernameAvailable("abc");
		verify(mockSynapseClient, never()).isAliasAvailable(anyString(), eq(AliasType.USER_NAME.toString()), any(AsyncCallback.class));
	}
	
	@Test
	public void testIsUsernameAvailableTrue() {
		newAccountPresenter.checkUsernameAvailable("abcd");
		verify(mockSynapseClient).isAliasAvailable(anyString(), eq(AliasType.USER_NAME.toString()), any(AsyncCallback.class));
		verify(mockView, never()).markUsernameUnavailable();
	}
	
	@Test
	public void testIsUsernameAvailableFalse() {
		AsyncMockStubber.callSuccessWith(false).when(mockSynapseClient).isAliasAvailable(anyString(), eq(AliasType.USER_NAME.toString()), any(AsyncCallback.class));
		newAccountPresenter.checkUsernameAvailable("abcd");
		verify(mockSynapseClient).isAliasAvailable(anyString(), eq(AliasType.USER_NAME.toString()), any(AsyncCallback.class));
		verify(mockView).markUsernameUnavailable();
	}
	
	@Test
	public void testIsEmailAvailableTrue() {
		newAccountPresenter.checkEmailAvailable("abcd@efg.com");
		verify(mockSynapseClient).isAliasAvailable(anyString(), eq(AliasType.USER_EMAIL.toString()), any(AsyncCallback.class));
	}
	
	@Test
	public void testIsEmailAvailableFalse() {
		AsyncMockStubber.callSuccessWith(false).when(mockSynapseClient).isAliasAvailable(anyString(), eq(AliasType.USER_EMAIL.toString()), any(AsyncCallback.class));
		newAccountPresenter.checkEmailAvailable("abcd@efg.com");
		verify(mockSynapseClient).isAliasAvailable(anyString(), eq(AliasType.USER_EMAIL.toString()), any(AsyncCallback.class));
		//attempts to go to the last place
		verify(mockGlobalApplicationState).gotoLastPlace();
	}

	@Test
	public void testParseValidationToken() {
		newAccountPresenter = new NewAccountPresenter(mockView, mockSynapseClient, mockGlobalApplicationState, mockUserService, mockAuthController, new GWTStub(), mockPasswordStrengthWidget);
		String token = "firstname=&lastname=&email=unittest%40jayhodgson.com&timestamp=2014-09-03T23%3A45%3A57.788%2B0000&domain=SYNAPSE&mac=DyXg5wUR3aqDABpnvYE%3D";
		Map<String, String> result = newAccountPresenter.parseEmailValidationToken(token);
		assertEquals("unittest@jayhodgson.com", result.get("email"));

		result = newAccountPresenter.parseEmailValidationToken(null);
		assertTrue(result.isEmpty());

		result = newAccountPresenter.parseEmailValidationToken("");
		assertTrue(result.isEmpty());
	}

	@Test
	public void testCompleteRegistrationAccountCreationToken() {
		String firstName = "   Mara  ";
		String lastName = " Jade     ";
		String userName = "skywalker290";
		String password = "  farfaraway";
		final AccountCreationToken accountCreationToken = new AccountCreationToken();
		accountCreationToken.setEmailValidationSignedToken(new EmailValidationSignedToken());
		NewAccount newPlace = Mockito.mock(NewAccount.class);
		when(newPlace.toToken()).thenReturn("0123456789ABCDEF");
		AsyncMockStubber.callSuccessWith(accountCreationToken).when(mockSynapseClient).hexDecodeAndDeserializeAccountCreationToken(anyString(), any(AsyncCallback.class));
		newAccountPresenter.setPlace(newPlace);
		newAccountPresenter.completeRegistration(userName, firstName, lastName, password);
		verify(mockView).setLoading(true);
		verify(mockView).setLoading(false);
		verify(mockUserService).createUserStep2(eq(userName), eq(firstName.trim()), eq(lastName.trim()), eq(password), eq(accountCreationToken.getEmailValidationSignedToken()), any(AsyncCallback.class));

		//should go to the login place with the new session token
		ArgumentCaptor<Place> placeCaptor = new ArgumentCaptor<Place>();
		verify(mockPlaceChanger).goTo(placeCaptor.capture());
		assertEquals(testSessionToken, ((LoginPlace)placeCaptor.getValue()).toToken());
	}

	@Test
	public void testCompleteRegistrationAccountCreationTokenFailure() {
		String firstName = "   Mara  ";
		String lastName = " Jade     ";
		String userName = "skywalker290";
		String password = "  farfaraway";
		final AccountCreationToken accountCreationToken = new AccountCreationToken();
		accountCreationToken.setEmailValidationSignedToken(new EmailValidationSignedToken());
		NewAccount newPlace = Mockito.mock(NewAccount.class);
		when(newPlace.toToken()).thenReturn("0123456789ABCDEF");
		AsyncMockStubber.callSuccessWith(accountCreationToken).when(mockSynapseClient).hexDecodeAndDeserializeAccountCreationToken(anyString(), any(AsyncCallback.class));
		newAccountPresenter.setPlace(newPlace);
		String failureMessage = "test message";
		AsyncMockStubber.callFailureWith(new Throwable(failureMessage)).when(mockUserService).createUserStep2(
				eq(userName.trim()), eq(firstName.trim()), eq(lastName.trim()), eq(password), any(EmailValidationSignedToken.class), any(AsyncCallback.class));
		newAccountPresenter.completeRegistration(userName, firstName, lastName, password);
		verify(mockView).setLoading(false);
		verify(mockView).showErrorMessage(DisplayConstants.ACCOUNT_CREATION_FAILURE + failureMessage);
	}
}
