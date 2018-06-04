package org.brewingagile.backoffice.types;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Objects;
import java.util.UUID;

@EqualsAndHashCode
@ToString(includeFieldNames = false)
public final class RegistrationId {
	public final UUID value;

	private RegistrationId(UUID x) {
		this.value = Objects.requireNonNull(x);
	}

	public static RegistrationId registrationId(String x) {
		return new RegistrationId(UUID.fromString(x));
	}

	public static RegistrationId registrationId(UUID x) {
		return new RegistrationId(x);
	}
}
