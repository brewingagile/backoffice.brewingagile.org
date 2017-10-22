package org.brewingagile.backoffice.pure;

import fj.data.List;
import org.brewingagile.backoffice.db.operations.BundlesSql;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper;

public class BundleLogic {
	public static Total logic(List<BundlesSql.BucketSummary> bundles, RegistrationsSqlMapper.Individuals individuals) {
		Total2 actual = new Total2(
			bundles.foldLeft((l,r) -> l + r.actualConference, 0),
			bundles.foldLeft((l,r) -> l + r.actualWorkshop1, 0),
			bundles.foldLeft((l,r) -> l + r.actualWorkshop2, 0)
		);
		Total2 planned = new Total2(
			bundles.foldLeft((l,r) -> l + r.bucket.conference, 0),
			bundles.foldLeft((l,r) -> l + r.bucket.workshop1, 0),
			bundles.foldLeft((l,r) -> l + r.bucket.workshop2, 0)
		);
		Total2 individuals1 = new Total2(individuals.conference, individuals.workshop1, individuals.workshop2);
		return new Total(actual, planned, individuals1,
			new Total2(
				planned.conference + individuals.conference,
				planned.workshop1 + individuals.workshop1,
				planned.workshop2 + individuals.workshop2
			)
		);
	}

	public static final class Total {
		public final Total2 bundlesActual;
		public final Total2 bundlesPlanned;
		public final Total2 individuals;
		public final Total2 total;

		public Total(Total2 bundlesActual, Total2 bundlesPlanned, Total2 individuals, Total2 total) {
			this.bundlesActual = bundlesActual;
			this.bundlesPlanned = bundlesPlanned;
			this.individuals = individuals;
			this.total = total;
		}
	}

	public static final class Total2 {
		public final int conference;
		public final int workshop1;
		public final int workshop2;

		public Total2(int conference, int workshop1, int workshop2) {
			this.conference = conference;
			this.workshop1 = workshop1;
			this.workshop2 = workshop2;
		}
	}
}
