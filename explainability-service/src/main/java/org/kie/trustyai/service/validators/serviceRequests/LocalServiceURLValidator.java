package org.kie.trustyai.service.validators.serviceRequests;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

/**
 * Validator class to allow only local Kubernetes Service URLs.
 */
public class LocalServiceURLValidator {
    private static final Pattern URL_PATTERN = Pattern.compile("^(http|https)://[a-zA-Z0-9-]+(:[0-9]{1,5})?$|^[a-zA-Z0-9-]+(:[0-9]{1,5})?$");

    /**
     * Method to validate local Kubernetes Service URLs.
     * http[s]://service is allowed, but FQN, such as http[s]://service.namespace.svc.cluster.local are not.
     * URLs without protocol are also allowed.
     *
     * @param url The service's location
     * @return Whether the service has a local URL or not
     */
    public static boolean isValidUrl(String url) {
        String formattedUrl = url;
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            formattedUrl = "http://" + url;
        }

        try {
            final URL parsedUrl = new URL(formattedUrl);
            final String host = parsedUrl.getHost();

            return URL_PATTERN.matcher(url).matches() && host != null && host.indexOf('.') == -1;
        } catch (MalformedURLException e) {
            return false;
        }
    }
}
