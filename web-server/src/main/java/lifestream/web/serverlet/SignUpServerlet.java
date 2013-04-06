package lifestream.web.serverlet;

import lifestream.user.bean.UserEntity;
import lifestream.user.client.UserClient;
import lifestream.user.data.UserMessage;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.MessageDigest;

public class SignUpServerlet extends HttpServlet {
	final UserClient userClient = UserClient.getInstance();

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String email = request.getParameter("email");
		String username = request.getParameter("username");
		String password = request.getParameter("password");

		if (email == null || username == null || password == null) {
			throw new ServletException("email, username, password is required");
		}

		UserEntity user = new UserEntity(username.trim(), email.trim(), sha256(password.trim()));

		userClient.connect();
		userClient.addUser(user, new ResponseHandler(response)).awaitUninterruptibly();
		synchronized (userClient) {
			try {
				userClient.wait(1000); // timeout
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		userClient.close();
	}

	class ResponseHandler extends UserClient.UserRequestResponseHandler {
		HttpServletResponse response;

		public ResponseHandler(HttpServletResponse response) {
			this.response = response;
		}

		@Override
		public void receivedUser(UserEntity user) {
			succeed(user, response);
		}

		@Override
		public void receivedError(UserMessage.Response.ResultCode code, String message) {
			failed(message, response);
		}
	}

	public void succeed(UserEntity user, HttpServletResponse response) {
		try {
			response.sendRedirect("user?id=" + user.getId());
		} catch (IOException e) {
			e.printStackTrace();
		}
		notifyClient();
	}

	public void failed(String message, HttpServletResponse response) {
		try {
			PrintWriter out = response.getWriter();
			out.println("Failed: " + message);
		} catch (IOException e) {
			e.printStackTrace();
		}
		notifyClient();
	}

	private void notifyClient() {
		synchronized (userClient) {
			userClient.notifyAll();
		}
	}

	public static String sha256(String base) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(base.getBytes("UTF-8"));
			StringBuilder hexString = new StringBuilder();

			for (byte aHash : hash) {
				String hex = Integer.toHexString(0xff & aHash);
				if (hex.length() == 1) hexString.append('0');
				hexString.append(hex);
			}

			return hexString.toString();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
}
