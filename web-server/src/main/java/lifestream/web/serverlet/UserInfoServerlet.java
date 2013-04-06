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
import java.util.UUID;

public class UserInfoServerlet extends HttpServlet {

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String id = request.getParameter("id");
		if (id == null || id.trim().equals("")) {
			throw new ServletException("id is required");
		}
		UserClient userClient = UserClient.getInstance();
		userClient.connect();
		userClient.getUser(UUID.fromString(id.trim()), new ResponseHandler(response));

		try {
			Thread.sleep(1000); // 1s timeout
		} catch (InterruptedException e) {
			System.out.println("Interrupted");
		} finally {
			userClient.close();
		}
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
			PrintWriter out = response.getWriter();
			out.println("Succeed: " + user.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void failed(String message, HttpServletResponse response) {
		try {
			PrintWriter out = response.getWriter();
			out.println("Failed: " + message);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
