package pt.ptcris;

import static org.orcid.core.api.OrcidApiConstants.ACTIVITIES;
import static org.orcid.core.api.OrcidApiConstants.PUTCODE;
import static org.orcid.core.api.OrcidApiConstants.VND_ORCID_XML;
import static org.orcid.core.api.OrcidApiConstants.WORK;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.orcid.api.common.OrcidClientHelper;
import org.orcid.jaxb.model.error_rc2.OrcidError;
import org.orcid.jaxb.model.record.summary_rc2.ActivitiesSummary;
import org.orcid.jaxb.model.record_rc2.Work;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;

public class ORCIDClientImpl implements ORCIDClient {

	private final OrcidClientHelper rest;
	private final String profile;
	private final String accessToken;

	public ORCIDClientImpl(String baseUri, String profile, String accessToken) throws URISyntaxException {
		this.profile = profile;
		this.accessToken = accessToken;
		rest = new OrcidClientHelper(new URI(baseUri), Client.create());
	}

	/**
	 * @see pt.ptcris.ORCIDClient#getFullWork(java.lang.Long)
	 */
	@Override
	public Work getWork(Long putCode) throws ORCIDException {
		URI uri = UriBuilder.fromPath(WORK + PUTCODE).build(profile, putCode);

		ClientResponse r = rest.getClientResponseWithToken(uri, VND_ORCID_XML, accessToken);

		if (r.getStatus() != Response.Status.OK.getStatusCode()) {
			OrcidError err = r.getEntity(OrcidError.class);
			throw new ORCIDException(err);
		}

		Work work = r.getEntity(Work.class);
		return work;
	}

	/**
	 * @see pt.ptcris.ORCIDClient#addWork(org.orcid.jaxb.model.record_rc2.Work)
	 */
	@Override
	public Long addWork(Work work) throws ORCIDException {
		URI uri = UriBuilder.fromPath(WORK).build(profile);
		ClientResponse r = rest.postClientResponseWithToken(uri, VND_ORCID_XML, work, accessToken);

		if (r.getStatus() != Response.Status.CREATED.getStatusCode()) {
			OrcidError err = r.getEntity(OrcidError.class);
			throw new ORCIDException(err);
		}

		String r_uri = r.getLocation().getPath();
		String r_putcode = r_uri.substring(r_uri.lastIndexOf("/") + 1);
		return Long.valueOf(r_putcode);
	}

	/**
	 * @see pt.ptcris.ORCIDClient#deleteWork(java.lang.Long)
	 */
	@Override
	public void deleteWork(Long putCode) throws ORCIDException {
		URI uri = UriBuilder.fromPath(WORK + PUTCODE).build(profile, putCode);
		ClientResponse r = rest.deleteClientResponseWithToken(uri, VND_ORCID_XML, accessToken);

		if (r.getStatus() != Response.Status.NO_CONTENT.getStatusCode()) {
			OrcidError err = r.getEntity(OrcidError.class);
			throw new ORCIDException(err);
		}

		// NOTE: according to the ORCID API, to delete a work, one must provide
		// the entire list of works in the ORCID profile minus the work(s) that
		// should be deleted. This means that this operation must be done in
		// three steps: first, retrieve the entire set of works; second, remove
		// the
		// work to be deleted from the list of works; and three, send the
		// updated list to the ORCID API.
	}

	/**
	 * @see pt.ptcris.ORCIDClient#updateWork(java.lang.Long,
	 *      org.orcid.jaxb.model.record_rc2.Work)
	 */
	@Override
	public Work updateWork(Long putCode, Work work) throws ORCIDException {
		URI uri = UriBuilder.fromPath(WORK + PUTCODE).build(profile, putCode);
		work.setPutCode(putCode);
		ClientResponse r = rest.putClientResponseWithToken(uri, VND_ORCID_XML, work, accessToken);

		if (r.getStatus() != Response.Status.OK.getStatusCode()) {
			OrcidError err = r.getEntity(OrcidError.class);
			throw new ORCIDException(err);
		}

		Work w = r.getEntity(Work.class);
		return w;

		// NOTE: according to the ORCID API, to update a work, one must provide
		// the entire list of works in the ORCID profile including the work(s)
		// that should be updated. This means that this operation must be done
		// in three steps: first, retrieve the entire set of works; second,
		// replace the work to be updated with the new record in the list of
		// works; and three, send the updated list to the ORCID API.
	}

	/**
	 * @see pt.ptcris.ORCIDClient#getActivitiesSummary()
	 */
	@Override
	public ActivitiesSummary getActivitiesSummary() throws ORCIDException {
		URI uri = UriBuilder.fromPath(ACTIVITIES).build(profile);
		ClientResponse r = rest.getClientResponseWithToken(uri, VND_ORCID_XML, accessToken);

		if (r.getStatus() != Response.Status.OK.getStatusCode()) {
			OrcidError err = r.getEntity(OrcidError.class);
			throw new ORCIDException(err);
		}

		ActivitiesSummary acts = r.getEntity(ActivitiesSummary.class);
		return acts;
	}

}
