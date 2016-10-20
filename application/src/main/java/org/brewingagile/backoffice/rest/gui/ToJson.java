package org.brewingagile.backoffice.rest.gui;

import argo.jdom.JsonNode;
import fj.F;
import fj.data.Option;

import static argo.jdom.JsonNodeFactories.nullNode;

public class ToJson {
	public static <A> JsonNode nullable(Option<A> oa, F<A,JsonNode> f) {
		return oa.map(f).orSome(nullNode());
	}
}
