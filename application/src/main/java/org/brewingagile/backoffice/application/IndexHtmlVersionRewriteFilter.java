package org.brewingagile.backoffice.application;

import com.hencjo.summer.migration.util.Charsets;
import com.hencjo.summer.security.api.AbstractFilter;
import org.brewingagile.backoffice.utils.URLEncoder;
import org.eclipse.jetty.util.IO;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class IndexHtmlVersionRewriteFilter extends AbstractFilter {
	private final String version;

	public IndexHtmlVersionRewriteFilter(String version) {
		this.version = version;
	}

	@Override
	public void doFilter(ServletRequest sreq, ServletResponse sres, FilterChain fc) throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest)sreq;
		HttpServletResponse res = (HttpServletResponse)sres;

		String html;
		try (
			InputStream resource = req.getSession().getServletContext().getResourceAsStream("/index.html");
			BufferedReader br = new BufferedReader(new InputStreamReader(resource, Charsets.UTF8))
		) {
			html = IO.toString(br);
		}

		String updatedHtml = html.replaceAll("##version##", URLEncoder.encode(version));
		res.addHeader("Content-Type", "text/html; charset=utf-8");
		res.addHeader("Cache-Control", "no-cache, no-store");
		res.setCharacterEncoding("UTF-8");
		res.getWriter().print(updatedHtml);
	}
}
