package pt.ptcris.test;

import java.math.BigInteger;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.um.dsi.gavea.orcid.model.common.ExternalId;
import org.um.dsi.gavea.orcid.model.common.ExternalIds;
import org.um.dsi.gavea.orcid.model.common.FuzzyDate;
import org.um.dsi.gavea.orcid.model.common.FuzzyDate.Year;
import org.um.dsi.gavea.orcid.model.common.RelationshipType;
import org.um.dsi.gavea.orcid.model.work.Work;
import org.um.dsi.gavea.orcid.model.work.WorkTitle;
import org.um.dsi.gavea.orcid.model.work.WorkType;

import pt.ptcris.handlers.ProgressHandler;
import pt.ptcris.utils.ORCIDHelper;

public class TestHelper {

	private static Tester progressHandler;

	public static Work work(BigInteger key, String meta) {
		Work work = new Work();

		ExternalIds uids = new ExternalIds();
		work.setExternalIds(uids);

		work.setPutCode(key);

		if (meta != null) {
			WorkTitle title = new WorkTitle();
			title.setTitle("Meta-data " + meta);
			work.setTitle(title);

			if (meta.equals("0"))
				work.setType(WorkType.JOURNAL_ARTICLE);
			else
				work.setType(WorkType.CONFERENCE_PAPER);

			FuzzyDate date = new FuzzyDate(new Year("201" + meta), null, null);
			work.setPublicationDate(date);
		}

		return work;
	}

	public static Work workDOI(BigInteger key, String meta, String doi) {
		Work work = work(key, meta);

		ExternalId e1 = new ExternalId();
		e1.setExternalIdRelationship(RelationshipType.SELF);
		e1.setExternalIdValue(doi);
		e1.setExternalIdType("doi");

		work.getExternalIds().getExternalId().add(e1);

		return work;
	}

	public static Work workHANDLE(BigInteger key, String meta, String handle) {
		Work work = work(key, meta);

		ExternalId e1 = new ExternalId();
		e1.setExternalIdRelationship(RelationshipType.SELF);
		e1.setExternalIdValue(handle);
		e1.setExternalIdType("handle");

		work.getExternalIds().getExternalId().add(e1);

		return work;
	}

	public static Work workDOIEID(BigInteger key, String meta, String doi, String eid) {
		Work work = workDOI(key, meta, doi);

		ExternalId e = new ExternalId();
		e.setExternalIdRelationship(RelationshipType.SELF);
		e.setExternalIdValue(eid);
		e.setExternalIdType("eid");
		
		work.getExternalIds().getExternalId().add(e);
		return work;
	}

	public static Work workDOIHANDLE(BigInteger key, String meta, String doi, String handle) {
		Work work = workDOI(key, meta, doi);

		ExternalId e = new ExternalId();
		e.setExternalIdRelationship(RelationshipType.SELF);
		e.setExternalIdValue(handle);
		e.setExternalIdType("handle");
		
		work.getExternalIds().getExternalId().add(e);

		return work;
	}

	public static Work workEIDHANDLE(BigInteger key, String meta, String eid, String handle) {
		Work work = workHANDLE(key, meta, handle);

		ExternalId e = new ExternalId();
		e.setExternalIdRelationship(RelationshipType.SELF);
		e.setExternalIdValue(eid);
		e.setExternalIdType("eid");

		work.getExternalIds().getExternalId().add(e);

		return work;
	}

	public static Work workDOIEIDHANDLE(BigInteger key, String meta, String doi, String eid, String handle) {
		Work work = workDOIEID(key, meta, doi, eid);

		ExternalId e2 = new ExternalId();
		e2.setExternalIdRelationship(RelationshipType.SELF);
		e2.setExternalIdValue(handle);
		e2.setExternalIdType("handle");

		work.getExternalIds().getExternalId().add(e2);

		return work;
	}

	public static Work workDOIDOIEIDHANDLE(BigInteger key, String meta, String doi1, String doi2, String eid, String handle) {
		Work work = workDOIEIDHANDLE(key, meta, doi1, eid, handle);

		ExternalId e1 = new ExternalId();
		e1.setExternalIdRelationship(RelationshipType.SELF);
		e1.setExternalIdValue(doi2);
		e1.setExternalIdType("doi");

		work.getExternalIds().getExternalId().add(e1);

		return work;
	}

	public static ProgressHandler handler() {
		if (progressHandler == null) {
			ConsoleHandler handler = new ConsoleHandler();
			handler.setFormatter(new SimpleFormatter());
			handler.setLevel(Level.ALL);
			Logger.getLogger(Tester.class.getName()).setLevel(Level.ALL);
			Logger.getLogger(Tester.class.getName()).addHandler(handler);
			progressHandler = new Tester();
		}
		return progressHandler;
	}

	public static void cleanUp(ORCIDHelper helper) throws Exception {
		helper.deleteAllSourcedWorks();
	}

}
