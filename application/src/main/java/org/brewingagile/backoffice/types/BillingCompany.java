package org.brewingagile.backoffice.types;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Objects;

@EqualsAndHashCode
@ToString(includeFieldNames = false)
public final class BillingCompany {
	public final String value;

	public BillingCompany(String value) {
		this.value = Objects.requireNonNull(value);
	}
}
