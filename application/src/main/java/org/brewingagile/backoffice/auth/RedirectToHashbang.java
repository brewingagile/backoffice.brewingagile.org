package org.brewingagile.backoffice.auth;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.common.base.Strings;
import com.hencjo.summer.security.api.Responder;

public final class RedirectToHashbang implements Responder {
	@Override
	public ContinueOrRespond respond(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String hash = request.getParameter("hash");
		if (Strings.isNullOrEmpty(hash)) hash = "#";
		if (!hash.startsWith("#")) throw new IOException("Expected a request parameter \"hash\" to start with '#'");
		response.sendRedirect(request.getContextPath() + "/" + hash);
		return ContinueOrRespond.RESPOND;
	}

	@Override
	public String describer() {
		return "RedirectToHashbang";
	}
}