package org.sagebionetworks.web.unitserver.servlet;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.UnknownSynapseServerException;
import org.sagebionetworks.web.client.StackEndpoints;
import org.sagebionetworks.web.client.cookie.CookieKeys;
import org.sagebionetworks.web.server.servlet.SynapseProvider;
import org.sagebionetworks.web.server.servlet.UserProfileAttachmentServlet;
import org.sagebionetworks.web.shared.WebConstants;

public class UserProfileAttachmentServletTest {
	
	@Mock
	HttpServletRequest mockRequest;
	@Mock
	HttpServletResponse mockResponse;
	@Mock
	SynapseClient mockClient;
	@Mock
	SynapseProvider mockSynapseProvider;
	@Mock
	ServletOutputStream mockOutStream;
	Cookie sessionCookie;
	
	String userId;
	String fileId;
	String preview;
	String applied;
	
	URL rawFileHandleUrl;
	URL userImageUrl;
	URL userImagePreveiwUrl;
	
	UserProfileAttachmentServlet servlet;
	
	@Before
	public void before() throws IOException, SynapseException{
		MockitoAnnotations.initMocks(this);
		when(mockSynapseProvider.createNewClient()).thenReturn(mockClient);
		servlet = new UserProfileAttachmentServlet();
		servlet.setSynapseProvider(mockSynapseProvider);
		System.setProperty(StackEndpoints.REPO_ENDPOINT_KEY, "repo");
		when(mockResponse.getOutputStream()).thenReturn(mockOutStream);
		sessionCookie = new Cookie(CookieKeys.USER_LOGIN_TOKEN, "token");
		when(mockRequest.getCookies()).thenReturn(new Cookie[]{sessionCookie});
		
		userId = "007";
		fileId = "444";
		preview = "true";
		applied = "true";
		
		rawFileHandleUrl = new URL("https://aws.com/filehandle"+fileId);
		userImageUrl = new URL("https://aws.com/"+userId);
		userImagePreveiwUrl = new URL("https://aws.com/"+userId+"/preview");
		
		when(mockClient.getFileHandleTemporaryUrl(fileId)).thenReturn(rawFileHandleUrl);
		when(mockClient.getUserProfilePictureUrl(userId)).thenReturn(userImageUrl);
		when(mockClient.getUserProfilePicturePreviewUrl(userId)).thenReturn(userImagePreveiwUrl);
		//no longer a redirect, it writes resulting url data to the output stream.  manually testing this functionality.
	}
	
	@Test
	public void testGetUserProfileSynapseException() throws ServletException, IOException, SynapseException{
		int statusCode = 404;
		String errorMessage = "a custom error";
		when(mockClient.getUserProfilePictureUrl(Matchers.anyString())).thenThrow(new UnknownSynapseServerException(statusCode, errorMessage));
		preview = "false";
		servlet.setPreviewTimeoutMs(1);
		// setup the parameters
		when(mockRequest.getParameter(WebConstants.USER_PROFILE_USER_ID)).thenReturn(userId);
		when(mockRequest.getParameter(WebConstants.USER_PROFILE_IMAGE_ID)).thenReturn(fileId);
		when(mockRequest.getParameter(WebConstants.USER_PROFILE_PREVIEW)).thenReturn(preview);
		when(mockRequest.getParameter(WebConstants.USER_PROFILE_APPLIED)).thenReturn(applied);
		servlet.doGet(mockRequest, mockResponse);
		verify(mockResponse).setStatus(statusCode);
	}
}
