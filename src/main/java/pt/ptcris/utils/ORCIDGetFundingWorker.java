/*
 * Copyright (c) 2016, 2017 PTCRIS - FCT|FCCN and others.
 * Licensed under MIT License
 * http://ptcris.pt
 *
 * This copyright and license information (including a link to the full license)
 * shall be included in its entirety in all copies or substantial portion of
 * the software.
 */
package pt.ptcris.utils;

import java.math.BigInteger;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.MDC;
import org.um.dsi.gavea.orcid.model.funding.Funding;
import org.um.dsi.gavea.orcid.model.funding.FundingSummary;

import pt.ptcris.ORCIDClient;
import pt.ptcris.PTCRISyncResult;

/**
 * A worker thread that can be used to GET funding entries from ORCID.
 *
 * @see ORCIDWorker
 */
final class ORCIDGetFundingWorker extends ORCIDWorker<Funding> {

	private final FundingSummary funding;

	/**
	 * A threaded worker that can be launched in parallel to GET funding
	 * activities with the ORCID API. The provided {@link ORCIDClient client}
	 * defines the communication channel.
	 * 
	 * @see ORCIDHelper#readWorker(org.um.dsi.gavea.orcid.model.common.ElementSummary,
	 *      Map)
	 *
	 * @param funding
	 *            the summary specifying the full funding to be retrieved
	 * @param client
	 *            the ORCID communication client
	 * @param cb
	 *            the callback object to return results
	 * @param log
	 *            a logger
	 */
	public ORCIDGetFundingWorker(FundingSummary funding, ORCIDClient client, Map<BigInteger, PTCRISyncResult<Funding>> cb, Logger log) {
		super(client, cb, log);
		assert funding != null;
		assert funding.getPutCode() != null;
				
		this.funding = funding;
	}

	/**
	 * Retrieves a full funding activity from an ORCID profile.
	 */
	@Override
	public void run() {
		try {
			MDC.setContextMap(mdcCtxMap);
		} catch (Exception e) {} // if the context is empty
		
		_log.debug("[getFullFunding] "+funding.getPutCode());
		
		final PTCRISyncResult<Funding> full = client.getFunding(funding);

		callback(funding.getPutCode(), full);
	}

}
