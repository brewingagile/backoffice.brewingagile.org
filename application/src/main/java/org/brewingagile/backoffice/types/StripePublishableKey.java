package org.brewingagile.backoffice.types;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Objects;

@EqualsAndHashCode
@ToString(includeFieldNames = false)
public final class StripePublishableKey {
	public final String value;

	private StripePublishableKey(String x) {
		this.value = Objects.requireNonNull(x);
	}

	public static StripePublishableKey stripePublishableKey(String x) {
		return new StripePublishableKey(x);
	}
}
