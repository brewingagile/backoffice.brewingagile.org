package org.brewingagile.backoffice.types;

import fj.data.Option;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Objects;
import java.util.UUID;

@EqualsAndHashCode
@ToString(includeFieldNames = false)
public final class StripePrivateKey {
	public final String value;

	private StripePrivateKey(String x) {
		this.value = Objects.requireNonNull(x);
	}

	public static StripePrivateKey stripePrivateKey(String x) {
		return new StripePrivateKey(x);
	}
}
