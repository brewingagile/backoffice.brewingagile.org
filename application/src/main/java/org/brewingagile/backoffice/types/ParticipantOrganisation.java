package org.brewingagile.backoffice.types;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Objects;

@EqualsAndHashCode
@ToString(includeFieldNames = false)
public final class ParticipantOrganisation {
	public final String value;

	private ParticipantOrganisation(String x) {
		this.value = Objects.requireNonNull(x);
	}

	public static ParticipantOrganisation participantOrganisation(String x) {
		return new ParticipantOrganisation(x);
	}
}
