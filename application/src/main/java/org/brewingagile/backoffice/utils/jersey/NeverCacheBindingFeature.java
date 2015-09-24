package org.brewingagile.backoffice.utils.jersey;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

@Provider
public class NeverCacheBindingFeature implements DynamicFeature {
	@Override
	public void configure(ResourceInfo resourceInfo, FeatureContext context) {
		if (classOrResourceAnnotatedWith(resourceInfo, NeverCache.class))
			context.register(NeverCacheFilter.class);
	}

	private static boolean classOrResourceAnnotatedWith(ResourceInfo resourceInfo, Class<NeverCache> class1) {
		return resourceInfo.getResourceClass().isAnnotationPresent(class1) || resourceInfo.getResourceMethod().isAnnotationPresent(class1);
	}
}