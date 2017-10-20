package org.brewingagile.backoffice.types;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Objects;

@EqualsAndHashCode
@ToString(includeFieldNames = false)
public final class Badge {
	public final String badge;

	public Badge(String badge) {
		this.badge = Objects.requireNonNull(badge);
	}
}
