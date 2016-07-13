package pt.ptcris.test.scenarios;

import java.math.BigInteger;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.um.dsi.gavea.orcid.model.common.FuzzyDate;
import org.um.dsi.gavea.orcid.model.common.FuzzyDate.Year;
import org.um.dsi.gavea.orcid.model.common.RelationshipType;
import org.um.dsi.gavea.orcid.model.work.ExternalIdentifier;
import org.um.dsi.gavea.orcid.model.work.ExternalIdentifierType;
import org.um.dsi.gavea.orcid.model.work.Work;
import org.um.dsi.gavea.orcid.model.work.WorkExternalIdentifiers;
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

		FuzzyDate date = new FuzzyDate(new Year("2016"), null, null);
		work.setPublicationDate(date);
		
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

		FuzzyDate date = new FuzzyDate(new Year("2016"), null, null);
		work.setPublicationDate(date);
		
		work.setPutCode(key);

		return work;
	}

	static Work workDOIEID(BigInteger key, String meta, String doi, String eid) {
		Work work = workDOI(key,meta,doi);

		ExternalIdentifier e = new ExternalIdentifier();
		e.setRelationship(RelationshipType.SELF);
		e.setExternalIdentifierId(eid);
		e.setExternalIdentifierType(ExternalIdentifierType.EID);

		work.getExternalIdentifiers().getWorkExternalIdentifier().add(e);

		return work;
	}

	static Work workDOIHANDLE(BigInteger key, String meta, String doi, String handle) {
		Work work = workDOI(key,meta,doi);

		ExternalIdentifier e = new ExternalIdentifier();
		e.setRelationship(RelationshipType.SELF);
		e.setExternalIdentifierId(handle);
		e.setExternalIdentifierType(ExternalIdentifierType.HANDLE);

		work.getExternalIdentifiers().getWorkExternalIdentifier().add(e);

		return work;
	}

	static Work workEIDHANDLE(BigInteger key, String meta, String eid, String handle) {
		Work work = workHANDLE(key,meta,handle);

		ExternalIdentifier e = new ExternalIdentifier();
		e.setRelationship(RelationshipType.SELF);
		e.setExternalIdentifierId(eid);
		e.setExternalIdentifierType(ExternalIdentifierType.EID);

		work.getExternalIdentifiers().getWorkExternalIdentifier().add(e);

		return work;
	}


	static Work workDOIEIDHANDLE(BigInteger key, String meta, String doi, String eid, String handle) {
		Work work = workDOIEID(key,meta,doi,eid);

		ExternalIdentifier e2 = new ExternalIdentifier();
		e2.setRelationship(RelationshipType.SELF);
		e2.setExternalIdentifierId(handle);
		e2.setExternalIdentifierType(ExternalIdentifierType.HANDLE);

		work.getExternalIdentifiers().getWorkExternalIdentifier().add(e2);

		return work;
	}
	
	static Work workDOIDOIEIDHANDLE(BigInteger key, String meta, String doi1, String doi2, String eid, String handle) {
		Work work = workDOIEIDHANDLE(key, meta, doi1, eid, handle);

		ExternalIdentifier e1 = new ExternalIdentifier();
		e1.setRelationship(RelationshipType.SELF);
		e1.setExternalIdentifierId(doi2);
		e1.setExternalIdentifierType(ExternalIdentifierType.DOI);

		work.getExternalIdentifiers().getWorkExternalIdentifier().add(e1);

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



	static void cleanUp(ORCIDHelper helper) throws Exception {
		helper.deleteAllSourcedWorks();
	}

}
