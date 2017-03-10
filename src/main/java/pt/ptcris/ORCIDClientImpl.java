package pt.ptcris;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.orcid.ns.bulk.Bulk;
import org.um.dsi.gavea.orcid.client.OrcidAccessToken;
import org.um.dsi.gavea.orcid.client.OrcidOAuthClient;
import org.um.dsi.gavea.orcid.client.exception.OrcidClientException;
import org.um.dsi.gavea.orcid.model.activities.ActivitiesSummary;
import org.um.dsi.gavea.orcid.model.work.Work;
import org.um.dsi.gavea.orcid.model.error.Error;

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
	public Work getWork(BigInteger putcode) throws OrcidClientException {
		return orcidClient.readWork(orcidToken, putcode.toString());
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
	public void deleteWork(BigInteger putcode) throws OrcidClientException {
		orcidClient.deleteWork(orcidToken, putcode.toString());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void updateWork(BigInteger putcode, Work work) throws OrcidClientException {
		orcidClient.updateWork(orcidToken, putcode.toString(), work);
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
	public int threads() {
		return threads;
	}

}
