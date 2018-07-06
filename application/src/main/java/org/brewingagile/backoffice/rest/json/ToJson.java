package org.brewingagile.backoffice.rest.json;

import argo.jdom.JsonNode;
import argo.jdom.JsonStringNode;
import fj.F;
import fj.data.Option;
import org.brewingagile.backoffice.types.*;

import java.time.Instant;

import static argo.jdom.JsonNodeFactories.nullNode;
import static argo.jdom.JsonNodeFactories.string;

public class ToJson {
	public static <A> JsonNode nullable(Option<A> oa, F<A,JsonNode> f) {
		return oa.map(f).orSome(nullNode());
	}
	public static JsonNode json(TicketName x) { return string(x.ticketName); }
	public static JsonNode account(Account x) { return string(x.value); }
	public static JsonNode ticketName(TicketName x) { return string(x.ticketName); }
	public static JsonNode accountSecret(AccountSecret x) { return string(x.value.toString()); }
	public static JsonNode accountSignupSecret(AccountSignupSecret x) { return string(x.value.toString()); }
	public static JsonNode json(ParticipantOrganisation x) { return string(x.value); }

	public static JsonStringNode participantEmail(ParticipantEmail participantEmail) { return string(participantEmail.value); }
	public static JsonStringNode participantName(ParticipantName participantName) { return string(participantName.value); }
	public static JsonStringNode registrationId(RegistrationId registrationId) { return string(registrationId.value.toString()); }

	public static JsonNode chargeId(ChargeId chargeId) { return string(chargeId.value); }
	public static JsonNode instant(Instant stripeChargeTimestamp) { return string(stripeChargeTimestamp.toString()); }
}
