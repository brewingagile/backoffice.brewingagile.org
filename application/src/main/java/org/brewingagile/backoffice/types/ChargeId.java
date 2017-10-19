package org.brewingagile.backoffice.types;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Objects;

@EqualsAndHashCode
@ToString(includeFieldNames = false)
public final class ChargeId {
	public final String value;

	private ChargeId(String x) {
		this.value = Objects.requireNonNull(x);
	}

	public static ChargeId chargeId(String x) {
		return new ChargeId(x);
	}
}
