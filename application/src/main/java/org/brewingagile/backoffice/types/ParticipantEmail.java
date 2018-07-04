package org.brewingagile.backoffice.types;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Objects;

@EqualsAndHashCode
@ToString(includeFieldNames = false)
public final class ParticipantEmail {
	public final String value;

	private ParticipantEmail(String x) {
		this.value = Objects.requireNonNull(x);
	}

	public static ParticipantEmail participantEmail(String x) {
		return new ParticipantEmail(x);
	}

	public static fj.Ord<ParticipantEmail> Ord = fj.Ord.ord(l -> r -> fj.Ord.stringOrd.compare(l.value, r.value));
}
