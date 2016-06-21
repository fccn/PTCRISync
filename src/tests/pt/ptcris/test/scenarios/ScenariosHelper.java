package pt.ptcris.test.scenarios;

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.um.dsi.gavea.orcid.model.common.RelationshipType;
import org.um.dsi.gavea.orcid.model.work.ExternalIdentifier;
import org.um.dsi.gavea.orcid.model.work.ExternalIdentifierType;
import org.um.dsi.gavea.orcid.model.work.Work;
import org.um.dsi.gavea.orcid.model.work.WorkExternalIdentifiers;
import org.um.dsi.gavea.orcid.model.work.WorkSummary;
import org.um.dsi.gavea.orcid.model.work.WorkTitle;
import org.um.dsi.gavea.orcid.model.work.WorkType;

import pt.ptcris.ORCIDHelper;
import pt.ptcris.handlers.ProgressHandler;

public class ScenariosHelper {

	private static Tester progressHandler;
	
	static Work workDOI(BigInteger key, String meta, String doi) {
		Work work = new Work();
		WorkTitle title = new WorkTitle();
		title.setTitle(meta);
		work.setTitle(title);

		ExternalIdentifier e1 = new ExternalIdentifier();
		e1.setRelationship(RelationshipType.SELF);
		e1.setExternalIdentifierId(doi);
		e1.setExternalIdentifierType(ExternalIdentifierType.DOI);

		WorkExternalIdentifiers uids = new WorkExternalIdentifiers();

		uids.getWorkExternalIdentifier().add(e1);

		work.setExternalIdentifiers(uids);

		work.setType(WorkType.CONFERENCE_PAPER);

		work.setPutCode(key);

		return work;
	}

	static Work workHANDLE(BigInteger key, String meta, String handle) {
		Work work = new Work();
		WorkTitle title = new WorkTitle();
		title.setTitle(meta);
		work.setTitle(title);

		ExternalIdentifier e1 = new ExternalIdentifier();
		e1.setRelationship(RelationshipType.SELF);
		e1.setExternalIdentifierId(handle);
		e1.setExternalIdentifierType(ExternalIdentifierType.HANDLE);

		WorkExternalIdentifiers uids = new WorkExternalIdentifiers();

		uids.getWorkExternalIdentifier().add(e1);

		work.setExternalIdentifiers(uids);

		work.setType(WorkType.CONFERENCE_PAPER);

		work.setPutCode(key);

		return work;
	}

	static Work workDOIEID(BigInteger key, String meta, String doi, String eid) {
		Work work = new Work();
		WorkTitle title = new WorkTitle();
		title.setTitle(meta);
		work.setTitle(title);

		ExternalIdentifier e = new ExternalIdentifier();
		e.setRelationship(RelationshipType.SELF);
		e.setExternalIdentifierId(eid);
		e.setExternalIdentifierType(ExternalIdentifierType.EID);

		ExternalIdentifier e1 = new ExternalIdentifier();
		e1.setRelationship(RelationshipType.SELF);
		e1.setExternalIdentifierId(doi);
		e1.setExternalIdentifierType(ExternalIdentifierType.DOI);

		WorkExternalIdentifiers uids = new WorkExternalIdentifiers();

		uids.getWorkExternalIdentifier().add(e);
		uids.getWorkExternalIdentifier().add(e1);

		work.setExternalIdentifiers(uids);

		work.setType(WorkType.CONFERENCE_PAPER);

		work.setPutCode(key);

		return work;
	}

	static Work workDOIHANDLE(BigInteger key, String meta, String doi, String handle) {
		Work work = new Work();
		WorkTitle title = new WorkTitle();
		title.setTitle(meta);
		work.setTitle(title);

		ExternalIdentifier e = new ExternalIdentifier();
		e.setRelationship(RelationshipType.SELF);
		e.setExternalIdentifierId(handle);
		e.setExternalIdentifierType(ExternalIdentifierType.HANDLE);

		ExternalIdentifier e1 = new ExternalIdentifier();
		e1.setRelationship(RelationshipType.SELF);
		e1.setExternalIdentifierId(doi);
		e1.setExternalIdentifierType(ExternalIdentifierType.DOI);

		WorkExternalIdentifiers uids = new WorkExternalIdentifiers();

		uids.getWorkExternalIdentifier().add(e);
		uids.getWorkExternalIdentifier().add(e1);

		work.setExternalIdentifiers(uids);

		work.setType(WorkType.CONFERENCE_PAPER);

		work.setPutCode(key);

		return work;
	}

	static Work workEIDHANDLE(BigInteger key, String meta, String eid, String handle) {
		Work work = new Work();
		WorkTitle title = new WorkTitle();
		title.setTitle(meta);
		work.setTitle(title);

		ExternalIdentifier e = new ExternalIdentifier();
		e.setRelationship(RelationshipType.SELF);
		e.setExternalIdentifierId(eid);
		e.setExternalIdentifierType(ExternalIdentifierType.EID);

		ExternalIdentifier e1 = new ExternalIdentifier();
		e1.setRelationship(RelationshipType.SELF);
		e1.setExternalIdentifierId(handle);
		e1.setExternalIdentifierType(ExternalIdentifierType.HANDLE);

		WorkExternalIdentifiers uids = new WorkExternalIdentifiers();

		uids.getWorkExternalIdentifier().add(e);
		uids.getWorkExternalIdentifier().add(e1);

		work.setExternalIdentifiers(uids);

		work.setType(WorkType.CONFERENCE_PAPER);

		work.setPutCode(key);

		return work;
	}


	static Work workDOIEIDHANDLE(BigInteger key, String meta, String doi, String eid, String handle) {
		Work work = new Work();
		WorkTitle title = new WorkTitle();
		title.setTitle(meta);
		work.setTitle(title);

		ExternalIdentifier e = new ExternalIdentifier();
		e.setRelationship(RelationshipType.SELF);
		e.setExternalIdentifierId(eid);
		e.setExternalIdentifierType(ExternalIdentifierType.EID);

		ExternalIdentifier e1 = new ExternalIdentifier();
		e1.setRelationship(RelationshipType.SELF);
		e1.setExternalIdentifierId(doi);
		e1.setExternalIdentifierType(ExternalIdentifierType.DOI);

		ExternalIdentifier e2 = new ExternalIdentifier();
		e2.setRelationship(RelationshipType.SELF);
		e2.setExternalIdentifierId(handle);
		e2.setExternalIdentifierType(ExternalIdentifierType.HANDLE);

		WorkExternalIdentifiers uids = new WorkExternalIdentifiers();

		uids.getWorkExternalIdentifier().add(e);
		uids.getWorkExternalIdentifier().add(e1);
		uids.getWorkExternalIdentifier().add(e2);

		work.setExternalIdentifiers(uids);

		work.setType(WorkType.CONFERENCE_PAPER);

		work.setPutCode(key);

		return work;
	}

	static Work workDOIDOIEIDHANDLE(BigInteger key, String meta, String doi1, String doi2, String eid, String handle) {
		Work work = workDOIEIDHANDLE(key, meta, doi1, eid, handle);

		ExternalIdentifier e1 = new ExternalIdentifier();
		e1.setRelationship(RelationshipType.SELF);
		e1.setExternalIdentifierId(doi2);
		e1.setExternalIdentifierType(ExternalIdentifierType.DOI);

		work.getExternalIdentifiers().getWorkExternalIdentifier().add(e1);

		work.setPutCode(key);

		return work;
	}

	static ProgressHandler handler() {
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

	static boolean correctImports(Collection<Work> works1, Collection<Work> works2) {
		Set<Work> ws1 = new HashSet<Work>(works1);
		Set<Work> ws2 = new HashSet<Work>(works2);

		for (Work work1 : works1) {
			Set<ExternalIdentifier> uids1 = new HashSet<ExternalIdentifier>(work1.getExternalIdentifiers()
					.getWorkExternalIdentifier());
			Iterator<Work> it = ws2.iterator();
			boolean found = false;
			while (it.hasNext() && !found) {
				Work work2 = it.next();
				Set<ExternalIdentifier> uids2 = new HashSet<ExternalIdentifier>(work2.getExternalIdentifiers()
						.getWorkExternalIdentifier());
				if (ORCIDHelper.equalsUIDs(uids1, uids2)
						&& ((work1.getPutCode() == null && work2.getPutCode() == null) || (work1.getPutCode()
								.equals(work2.getPutCode())))) {
					ws1.remove(work1);
					ws2.remove(work2);
					found = true;
				}
			}
		}
		return ws1.isEmpty() && ws2.isEmpty();
	}

	static boolean correctExport(Collection<Work> works1, Collection<WorkSummary> works2) {
		Set<Work> ws1 = new HashSet<Work>(works1);
		Set<WorkSummary> ws2 = new HashSet<WorkSummary>(works2);

		for (Work work1 : works1) {
			Set<ExternalIdentifier> uids1 = new HashSet<ExternalIdentifier>(work1.getExternalIdentifiers()
					.getWorkExternalIdentifier());
			Iterator<WorkSummary> it = works2.iterator();
			boolean found = false;
			while (it.hasNext() && !found) {
				WorkSummary work2 = it.next();
				Set<ExternalIdentifier> uids2 = new HashSet<ExternalIdentifier>(work2.getExternalIdentifiers()
						.getWorkExternalIdentifier());
				if (ORCIDHelper.equalsUIDs(uids1, uids2)
						&& ORCIDHelper.getWorkTitle(work1).equals(ORCIDHelper.getWorkTitle(work2))) {
					ws1.remove(work1);
					ws2.remove(work2);
					found = true;
				}
			}
		}
		return ws1.isEmpty() && ws2.isEmpty();
	}

	static void cleanUp(ORCIDHelper helper) throws Exception {
		for (WorkSummary work : helper.getSourcedWorkSummaries())
			helper.deleteWork(work.getPutCode());

	}
}
