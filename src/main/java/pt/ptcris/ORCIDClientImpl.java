/*
 * Copyright (c) 2016, 2017 PTCRIS - FCT|FCCN and others.
 * Licensed under MIT License
 * http://ptcris.pt
 *
 * This copyright and license information (including a link to the full license)
 * shall be included in its entirety in all copies or substantial portion of
 * the software.
 */
package pt.ptcris;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.um.dsi.gavea.orcid.client.OrcidAccessToken;
import org.um.dsi.gavea.orcid.client.OrcidOAuthClient;
import org.um.dsi.gavea.orcid.client.exception.OrcidClientException;
import org.um.dsi.gavea.orcid.model.activities.ActivitiesSummary;
import org.um.dsi.gavea.orcid.model.activities.Works;
import org.um.dsi.gavea.orcid.model.bulk.Bulk;
import org.um.dsi.gavea.orcid.model.work.Work;
import org.um.dsi.gavea.orcid.model.work.WorkSummary;
import org.um.dsi.gavea.orcid.model.error.Error;

import pt.ptcris.utils.ORCIDHelper;

/**
 * An implementation of the ORCID client interface built over the
 * {@link org.um.dsi.gavea.orcid.client.OrcidOAuthClient Degois client}.
 * 
 * Besides the tokens to use the ORCID Member API, it also store the tokens
 * to access a particular ORCID user profile.
 * 
 * @see ORCIDClient
 */
public class ORCIDClientImpl implements ORCIDClient {

	private final OrcidAccessToken orcidToken;
	private final OrcidOAuthClient orcidClient;
	private final String clientId;
	private final int threads;

	/**
	 * Instantiates an ORCID client to communicate with the ORCID API.
	 *
	 * @param loginUri
	 *            the login URI of the ORCID service
	 * @param apiUri
	 *            the URI of the ORCID API
	 * @param clientId
	 *            the id of the ORCID Member API client
	 * @param clientSecret
	 *            the secret of the ORCID Member API client
	 * @param redirectUri
	 *            the redirect URI for requesting the access token
	 * @param orcidToken
	 *            the access token to the user ORCID profile
	 * @param threads
	 *            the number of ORCID worker threads
	 */
	public ORCIDClientImpl(String loginUri, String apiUri, String clientId, String clientSecret, String redirectUri,
			OrcidAccessToken orcidToken, int threads) {
		this(loginUri, apiUri, clientId, clientSecret, redirectUri, orcidToken, false, threads);
	}
	
	
	/**
	 * Instantiates an ORCID client to communicate with the ORCID API.
	 *
	 * @param loginUri
	 *            the login URI of the ORCID service
	 * @param apiUri
	 *            the URI of the ORCID API
	 * @param clientId
	 *            the id of the ORCID Member API client
	 * @param clientSecret
	 *            the secret of the ORCID Member API client
	 * @param redirectUri
	 *            the redirect URI for requesting the access token
	 * @param orcidToken
	 *            the access token to the user ORCID profile
	 */
	public ORCIDClientImpl(String loginUri, String apiUri, String clientId, String clientSecret, String redirectUri,
			OrcidAccessToken orcidToken) {
		this(loginUri, apiUri, clientId, clientSecret, redirectUri, orcidToken, false, Runtime.getRuntime().availableProcessors() + 2);
	}
	
	/**
	 * Instantiates an ORCID client to communicate with the ORCID API.
	 *
	 * @param loginUri
	 *            the login URI of the ORCID service
	 * @param apiUri
	 *            the URI of the ORCID API
	 * @param clientId
	 *            the id of the ORCID Member API client
	 * @param clientSecret
	 *            the secret of the ORCID Member API client
	 * @param redirectUri
	 *            the redirect URI for requesting the access token
	 * @param orcidToken
	 *            the access token to the user ORCID profile
	 * @param debugMode
	 *            Enter debug mode
	 * @param threads
	 *            the number of ORCID worker threads
	 */
	public ORCIDClientImpl(String loginUri, String apiUri, String clientId, String clientSecret, String redirectUri,
			OrcidAccessToken orcidToken, boolean debugMode) {
		this(loginUri, apiUri, clientId, clientSecret, redirectUri, orcidToken, debugMode, Runtime.getRuntime().availableProcessors() + 2);
	}		
	
	
	/**
	 * Instantiates an ORCID client to communicate with the ORCID API.
	 *
	 * @param loginUri
	 *            the login URI of the ORCID service
	 * @param apiUri
	 *            the URI of the ORCID API
	 * @param clientId
	 *            the id of the ORCID Member API client
	 * @param clientSecret
	 *            the secret of the ORCID Member API client
	 * @param redirectUri
	 *            the redirect URI for requesting the access token
	 * @param orcidToken
	 *            the access token to the user ORCID profile
	 * @param debugMode
	 *            Enter debug mode
	 * @param threads
	 *            the number of ORCID worker threads
	 */
	public ORCIDClientImpl(String loginUri, String apiUri, String clientId, String clientSecret, String redirectUri,
			OrcidAccessToken orcidToken, boolean debugMode, int threads) {
		this.orcidToken = orcidToken;
		this.clientId = clientId;
		this.threads = threads;
		this.orcidClient = new OrcidOAuthClient(loginUri, apiUri, clientId, clientSecret, redirectUri, debugMode);
	}	

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getClientId() {
		return clientId;
	}

	/**
	 * {@inheritDoc}
	 */	
	@Override
	public PTCRISyncResult getWork(WorkSummary putcode) {
		PTCRISyncResult res;
		try {
			Work work = orcidClient.readWork(orcidToken, putcode.getPutCode().toString());
			ORCIDHelper.finalizeGet(work, putcode);
			res = PTCRISyncResult.got(putcode.getPutCode(), work);
		} catch (OrcidClientException e) {
			res = PTCRISyncResult.fail(e);
		}
		return res;
	}

	/**
	 * {@inheritDoc}
	 */	
	@Override
	public Map<BigInteger,PTCRISyncResult> getWorks(List<WorkSummary> summaries) {
		List<String> pcs = new ArrayList<String>();
		for (WorkSummary i : summaries)
			pcs.add(i.getPutCode().toString());
		Map<BigInteger,PTCRISyncResult> res = new HashMap<BigInteger,PTCRISyncResult>();
		try {
			List<Serializable> bulk = orcidClient.readWorks(orcidToken, pcs).getWorkOrError();
			for (int i = 0; i < summaries.size(); i++) {
				Serializable w = bulk.get(i);
				if (w instanceof Work) {
					ORCIDHelper.finalizeGet((Work) w, summaries.get(i));
					res.put(summaries.get(i).getPutCode(),PTCRISyncResult.got(summaries.get(i).getPutCode(),(Work) w));
				}
				else {
					Error err = (Error) w;
					OrcidClientException e = new OrcidClientException(err.getResponseCode(), 
							err.getUserMessage(),
							err.getErrorCode(),
							err.getDeveloperMessage());	
					res.put(summaries.get(i).getPutCode(),PTCRISyncResult.fail(e));
				}
			}
		} catch (OrcidClientException e1) {
			for (int i = 0; i < summaries.size(); i++) 
				res.put(summaries.get(i).getPutCode(),PTCRISyncResult.fail(e1));
		}
		return res;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public PTCRISyncResult addWork(Work work) {
		PTCRISyncResult res;
		try {
			BigInteger putcode = new BigInteger(orcidClient.addWork(orcidToken, work));
			res = PTCRISyncResult.ok(putcode);
		} catch (OrcidClientException e) {
			return PTCRISyncResult.fail(e);
		}
		return res;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<PTCRISyncResult> addWorks(List<Work> works) {
		Bulk bulk = new Bulk();
		List<PTCRISyncResult> res = new ArrayList<PTCRISyncResult>();
		for (Work work : works)
			bulk.getWorkOrError().add(work);
		try {
			Bulk res_bulk = orcidClient.addWorks(orcidToken, bulk);
			for (Serializable r : res_bulk.getWorkOrError()) {
				if (r instanceof Work)
					res.add(PTCRISyncResult.ok(((Work) r).getPutCode()));
				else {
					Error err = (Error) r;
					OrcidClientException e = new OrcidClientException(err.getResponseCode(), 
																	  err.getUserMessage(),
																	  err.getErrorCode(),
																	  err.getDeveloperMessage());
					res.add(PTCRISyncResult.fail(e));
				}
			}
		} catch (OrcidClientException e) {
			for (int i=0;i<works.size();i++)
				res.add(PTCRISyncResult.fail(e));
		}
		return res; 
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PTCRISyncResult deleteWork(BigInteger putcode) {
		try {
			orcidClient.deleteWork(orcidToken, putcode.toString());
			return PTCRISyncResult.OK_DEL_RESULT;
		} catch (OrcidClientException e) {
			return PTCRISyncResult.fail(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PTCRISyncResult updateWork(BigInteger putcode, Work work) {
		PTCRISyncResult res;
		try {
			orcidClient.updateWork(orcidToken, putcode.toString(), work);
			res = PTCRISyncResult.OK_UPD_RESULT;
		} catch (OrcidClientException e) {
			return PTCRISyncResult.fail(e);
		}
		return res;
		
		
		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ActivitiesSummary getActivitiesSummary() throws OrcidClientException {
		return orcidClient.readActivitiesSummary(orcidToken);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Works getWorksSummary() throws OrcidClientException {
		return orcidClient.readWorksSummary(orcidToken);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int threads() {
		return threads;
	}

}
