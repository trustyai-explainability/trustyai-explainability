package org.kie.trustyai.service.validators;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.service.validators.serviceRequests.LocalServiceURLValidator;

public class LocalServiceURLValidatorTest {

    @Test
    public void testValidHttpUrls() {
        Assertions.assertTrue(LocalServiceURLValidator.isValidUrl("http://foo"));
        Assertions.assertTrue(LocalServiceURLValidator.isValidUrl("http://foo:8080"));
        Assertions.assertTrue(LocalServiceURLValidator.isValidUrl("http://bar"));
        Assertions.assertTrue(LocalServiceURLValidator.isValidUrl("http://bar:9090"));
    }

    @Test
    public void testValidHttpsUrls() {
        Assertions.assertTrue(LocalServiceURLValidator.isValidUrl("https://foo"));
        Assertions.assertTrue(LocalServiceURLValidator.isValidUrl("https://foo:8443"));
        Assertions.assertTrue(LocalServiceURLValidator.isValidUrl("https://bar"));
        Assertions.assertTrue(LocalServiceURLValidator.isValidUrl("https://bar:9443"));
    }

    @Test
    public void testValidNoProtocolUrls() {
        Assertions.assertTrue(LocalServiceURLValidator.isValidUrl("foo"));
        Assertions.assertTrue(LocalServiceURLValidator.isValidUrl("foo:8080"));
        Assertions.assertTrue(LocalServiceURLValidator.isValidUrl("bar"));
        Assertions.assertTrue(LocalServiceURLValidator.isValidUrl("bar:9090"));
    }

    @Test
    public void testInvalidUrlsWithNamespace() {
        Assertions.assertFalse(LocalServiceURLValidator.isValidUrl("http://foo.test.svc.cluster.local"));
        Assertions.assertFalse(LocalServiceURLValidator.isValidUrl("https://foo.test.svc.cluster.local"));
        Assertions.assertFalse(LocalServiceURLValidator.isValidUrl("foo.test.svc.cluster.local"));
    }

    @Test
    public void testInvalidUrlsWithPath() {
        Assertions.assertFalse(LocalServiceURLValidator.isValidUrl("http://foo/some/path"));
        Assertions.assertFalse(LocalServiceURLValidator.isValidUrl("https://foo/some/path"));
        Assertions.assertFalse(LocalServiceURLValidator.isValidUrl("foo/some/path"));
    }

    @Test
    public void testInvalidUrlsWithQuery() {
        Assertions.assertFalse(LocalServiceURLValidator.isValidUrl("http://foo?query=param"));
        Assertions.assertFalse(LocalServiceURLValidator.isValidUrl("https://foo?query=param"));
        Assertions.assertFalse(LocalServiceURLValidator.isValidUrl("foo?query=param"));
    }

    @Test
    public void testInvalidUrlsWithFragment() {
        Assertions.assertFalse(LocalServiceURLValidator.isValidUrl("http://foo#fragment"));
        Assertions.assertFalse(LocalServiceURLValidator.isValidUrl("https://foo#fragment"));
        Assertions.assertFalse(LocalServiceURLValidator.isValidUrl("foo#fragment"));
    }

    @Test
    public void testInvalidUrlsWithMultipleDots() {
        Assertions.assertFalse(LocalServiceURLValidator.isValidUrl("http://foo..bar"));
        Assertions.assertFalse(LocalServiceURLValidator.isValidUrl("https://foo..bar"));
        Assertions.assertFalse(LocalServiceURLValidator.isValidUrl("foo..bar"));
        Assertions.assertFalse(LocalServiceURLValidator.isValidUrl("http://foo.bar."));
        Assertions.assertFalse(LocalServiceURLValidator.isValidUrl("https://foo.bar."));
        Assertions.assertFalse(LocalServiceURLValidator.isValidUrl("foo.bar."));
    }

    @Test
    public void testInvalidUrlsWithNoHost() {
        Assertions.assertFalse(LocalServiceURLValidator.isValidUrl("http://:8080"));
        Assertions.assertFalse(LocalServiceURLValidator.isValidUrl("https://:8443"));
        Assertions.assertFalse(LocalServiceURLValidator.isValidUrl(":8080"));
    }

    @Test
    public void testInvalidMalformedUrls() {
        Assertions.assertFalse(LocalServiceURLValidator.isValidUrl("http:/foo"));
        Assertions.assertFalse(LocalServiceURLValidator.isValidUrl("https:/foo"));
        Assertions.assertFalse(LocalServiceURLValidator.isValidUrl("http://"));
        Assertions.assertFalse(LocalServiceURLValidator.isValidUrl("https://"));
        Assertions.assertFalse(LocalServiceURLValidator.isValidUrl("/foo"));
    }
}
