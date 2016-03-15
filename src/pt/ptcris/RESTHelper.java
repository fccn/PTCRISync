package pt.ptcris;

import java.net.URI;
import java.net.URISyntaxException;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class RESTHelper {

	private final Client client;
	private final URI baseUri;

	public RESTHelper(String baseUri) throws URISyntaxException {
		this.client = Client.create();
		if (baseUri.toString().endsWith("/")) {
			String s = baseUri.toString();
			this.baseUri = new URI(s.substring(0, s.length() - 1));
		} else {
			this.baseUri = new URI(baseUri);
		}
	}

	public ClientResponse getClientResponseWithToken(URI restPath, String accept, String oauthToken) {
		return setupRequestCommonParams(restPath, accept, oauthToken).get(ClientResponse.class);
	}

	public ClientResponse postClientResponseWithToken(URI restPath, String accept, Object jaxbRootElement, String oauthToken) {
		return setupRequestCommonParams(restPath, accept, oauthToken).post(ClientResponse.class, jaxbRootElement);
	}

	private WebResource.Builder setupRequestCommonParams(URI restpath, String accept, String oauthToken) {
		WebResource rootResource = createRootResource(restpath);
		WebResource.Builder built = addOauthHeader(rootResource, oauthToken).accept(accept).type(accept);
		return built;
	}

	private WebResource.Builder addOauthHeader(WebResource webResource, String oAuthToken) {
		return webResource.header("Authorization", "Bearer " + oAuthToken);
	}

	private URI resolveUri(URI uri) {
		try {
			if(uri.getHost() != null){
				return uri;
			}
			return new URI(baseUri.toString().concat(uri.toString()));
		} catch (URISyntaxException e) {
			throw new RuntimeException("Calculated URI is invalid. Please check the settings.", e);
		}
	}

	private WebResource createRootResource(URI uri) {
		return (client.resource(resolveUri(uri)));
	}

}
