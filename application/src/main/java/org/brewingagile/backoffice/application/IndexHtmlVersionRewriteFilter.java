package org.brewingagile.backoffice.application;

import com.hencjo.summer.migration.util.Charsets;
import com.hencjo.summer.security.api.AbstractFilter;
import org.brewingagile.backoffice.utils.GitPropertiesDescribeVersionNumberProvider;
import org.brewingagile.backoffice.utils.URLEncoder;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

public class IndexHtmlVersionRewriteFilter extends AbstractFilter {
	private final GitPropertiesDescribeVersionNumberProvider versionNumberProvider = Application.INSTANCE.versionNumberProvider();

	@Override
	public void doFilter(ServletRequest sreq, ServletResponse sres, FilterChain fc) throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest)sreq;
		HttpServletResponse res = (HttpServletResponse)sres;
		ServletContext servletContext = req.getSession().getServletContext();
		URL resource = servletContext.getResource("/index.html");
		String html;
		try {
			html = new String(Files.readAllBytes(Paths.get(resource.toURI())), Charsets.UTF8);
		} catch (URISyntaxException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		String updatedHtml = html.replaceAll("##version##", URLEncoder.encode(versionNumberProvider.softwareVersion()));
		res.addHeader("Content-Type", "text/html; charset=utf-8");
		res.addHeader("Cache-Control", "no-cache, no-store");
		res.setCharacterEncoding("UTF-8");
		res.getWriter().print(updatedHtml);
	}
}
