/*
 * Copyright (c) 2016, 2017 PTCRIS - FCT|FCCN and others.
 * Licensed under MIT License
 * http://ptcris.pt
 *
 * This copyright and license information (including a link to the full license)
 * shall be included in its entirety in all copies or substantial portion of
 * the software.
 */
package pt.ptcris.test;

import java.math.BigInteger;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.um.dsi.gavea.orcid.model.common.ExternalId;
import org.um.dsi.gavea.orcid.model.common.ExternalIds;
import org.um.dsi.gavea.orcid.model.common.FuzzyDate;
import org.um.dsi.gavea.orcid.model.common.Iso3166Country;
import org.um.dsi.gavea.orcid.model.common.OrganizationAddress;
import org.um.dsi.gavea.orcid.model.common.FuzzyDate.Year;
import org.um.dsi.gavea.orcid.model.common.Organization;
import org.um.dsi.gavea.orcid.model.common.RelationshipType;
import org.um.dsi.gavea.orcid.model.funding.Funding;
import org.um.dsi.gavea.orcid.model.funding.FundingTitle;
import org.um.dsi.gavea.orcid.model.funding.FundingType;
import org.um.dsi.gavea.orcid.model.work.Work;
import org.um.dsi.gavea.orcid.model.work.WorkTitle;
import org.um.dsi.gavea.orcid.model.work.WorkType;

import pt.ptcris.handlers.ProgressHandler;
import pt.ptcris.utils.ORCIDHelper;
import pt.ptcris.utils.ORCIDHelper.EIdType;

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

			FuzzyDate date = new FuzzyDate(new Year("201" + meta.charAt(meta.length()-1)), null, null);
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
	
	public static Work workUnk(BigInteger key, String meta, String doi) {
		Work work = work(key, meta);

		ExternalId e1 = new ExternalId();
		e1.setExternalIdRelationship(RelationshipType.SELF);
		e1.setExternalIdValue(doi);
		e1.setExternalIdType("ukn");

		work.getExternalIds().getExternalId().add(e1);

		return work;
	}
	
	public static Work workDOIUnk(BigInteger key, String meta, String doi, String eid) {
		Work work = workDOI(key, meta, doi);

		ExternalId e = new ExternalId();
		e.setExternalIdRelationship(RelationshipType.SELF);
		e.setExternalIdValue(eid);
		e.setExternalIdType("wosuid-");
		
		work.getExternalIds().getExternalId().add(e);
		return work;
	}
	
	public static Work workOtherOtherDOI(BigInteger key, String meta, String doi, String eid, String eid2) {
		Work work = workDOI(key, meta, doi);

		ExternalId e = new ExternalId();
		e.setExternalIdRelationship(RelationshipType.SELF);
		e.setExternalIdValue(eid);
		e.setExternalIdType(EIdType.OTHER_ID.value);
		
		work.getExternalIds().getExternalId().add(e);

		ExternalId e1 = new ExternalId();
		e1.setExternalIdRelationship(RelationshipType.SELF);
		e1.setExternalIdValue(eid2);
		e1.setExternalIdType(EIdType.OTHER_ID.value);
		
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

		
//		ExternalId e2 = new ExternalId();
//		e2.setExternalIdRelationship(RelationshipType.PART_OF);
//		e2.setExternalIdValue("11111");
//		e2.setExternalIdType("isbn");
//
//		work.getExternalIds().getExternalId().add(e2);
		
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

		ExternalId e2 = new ExternalId();
		e2.setExternalIdRelationship(RelationshipType.PART_OF);
		e2.setExternalIdValue("11111");
		e2.setExternalIdType("isbn");

		work.getExternalIds().getExternalId().add(e2);
		
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

		ExternalId e3 = new ExternalId();
		e3.setExternalIdRelationship(RelationshipType.PART_OF);
		e3.setExternalIdValue("11111");
		e3.setExternalIdType("isbn");
		work.getExternalIds().getExternalId().add(e3);

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
	
	public static Funding funding(BigInteger key, String meta) {
		Funding work = new Funding();

		ExternalIds uids = new ExternalIds();
		work.setExternalIds(uids);

		work.setPutCode(key);

		if (meta != null) {
			FundingTitle title = new FundingTitle();
			title.setTitle("Meta-data " + meta);
			work.setTitle(title);

			work.setOrganization(new Organization("Agency", new OrganizationAddress("Braga",null,Iso3166Country.PT), null));
			
			if (meta.equals("0"))
				work.setType(FundingType.AWARD);
			else
				work.setType(FundingType.GRANT);

			FuzzyDate date = new FuzzyDate(new Year("201" + meta.charAt(meta.length()-1)), null, null);
			work.setStartDate(date);

		}

		return work;
	}

	public static Funding fundingNmb(BigInteger key, String meta, String doi) {
		Funding work = funding(key, meta);

		ExternalId e1 = new ExternalId();
		e1.setExternalIdRelationship(RelationshipType.SELF);
		e1.setExternalIdValue(doi);
		e1.setExternalIdType("grant_number");

		work.getExternalIds().getExternalId().add(e1);

		return work;
	}
	
	public static Funding fundingNmbNmb(BigInteger key, String meta, String doi1, String doi2) {
		Funding work = fundingNmb(key, meta, doi1);

		ExternalId e1 = new ExternalId();
		e1.setExternalIdRelationship(RelationshipType.SELF);
		e1.setExternalIdValue(doi2);
		e1.setExternalIdType("grant_number");

		work.getExternalIds().getExternalId().add(e1);

		return work;
	}
	
	public static Funding fundingNmbNmbNmb(BigInteger key, String meta, String doi1, String doi2, String doi3) {
		Funding work = fundingNmbNmb(key, meta, doi1, doi2);

		ExternalId e1 = new ExternalId();
		e1.setExternalIdRelationship(RelationshipType.SELF);
		e1.setExternalIdValue(doi3);
		e1.setExternalIdType("grant_number");

		work.getExternalIds().getExternalId().add(e1);

		return work;
	}
	
	public static Funding fundingNmbNmbNmbNmb(BigInteger key, String meta, String doi1, String doi2, String doi3, String doi4) {
		Funding work = fundingNmbNmbNmb(key, meta, doi1, doi2, doi3);

		ExternalId e1 = new ExternalId();
		e1.setExternalIdRelationship(RelationshipType.SELF);
		e1.setExternalIdValue(doi4);
		e1.setExternalIdType("grant_number");

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
		helper.deleteAllSourcedFundings();
	}

}
