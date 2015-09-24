package org.brewingagile.backoffice.utils.jersey;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

public class NeverCacheFilter implements ContainerResponseFilter {
	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext context) throws IOException {
		context.getHeaders().putSingle("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1
		context.getHeaders().putSingle("Pragma", "no-cache"); // HTTP 1.0
		context.getHeaders().putSingle("Expires", "0"); // Proxies
	}
}