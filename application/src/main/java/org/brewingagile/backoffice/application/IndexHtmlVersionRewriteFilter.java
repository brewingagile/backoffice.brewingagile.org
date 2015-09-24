package org.brewingagile.backoffice.application;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.hencjo.summer.security.api.AbstractFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.brewingagile.backoffice.utils.GitPropertiesDescribeVersionNumberProvider;
import org.brewingagile.backoffice.utils.URLEncoder;

public class IndexHtmlVersionRewriteFilter extends AbstractFilter {
	private final GitPropertiesDescribeVersionNumberProvider versionNumberProvider = Application.INSTANCE.versionNumberProvider();

	@Override
	public void doFilter(ServletRequest sreq, ServletResponse sres, FilterChain fc) throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest)sreq;
		HttpServletResponse res = (HttpServletResponse)sres;
		ServletContext servletContext = req.getSession().getServletContext();
		String html;
		try (InputStream is = servletContext.getResourceAsStream("/index.html")) {
			html = CharStreams.toString(new InputStreamReader(is, Charsets.UTF_8));
		}
		String updatedHtml = html.replaceAll("##version##", URLEncoder.encode(versionNumberProvider.softwareVersion()));
		res.addHeader("Content-Type", "text/html; charset=utf-8");
		res.addHeader("Cache-Control", "no-cache, no-store");
		res.setCharacterEncoding("UTF-8");
		res.getWriter().print(updatedHtml);
	}
}
