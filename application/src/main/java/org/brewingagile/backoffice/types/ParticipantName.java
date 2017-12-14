package org.brewingagile.backoffice.types;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Objects;

@EqualsAndHashCode
@ToString(includeFieldNames = false)
public final class ParticipantName {
	public final String value;

	private ParticipantName(String x) {
		this.value = Objects.requireNonNull(x);
	}

	public static ParticipantName participantName(String x) {
		return new ParticipantName(x);
	}

	public static fj.Ord<ParticipantName> Ord = fj.Ord.ord(l -> r -> fj.Ord.stringOrd.compare(l.value, r.value));
}
