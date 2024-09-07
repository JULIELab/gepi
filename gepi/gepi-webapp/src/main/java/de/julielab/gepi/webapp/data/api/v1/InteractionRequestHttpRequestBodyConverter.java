package de.julielab.gepi.webapp.data.api.v1;

import org.apache.tapestry5.http.services.HttpRequestBodyConverter;

import javax.servlet.http.HttpServletRequest;

public class InteractionRequestHttpRequestBodyConverter implements HttpRequestBodyConverter {

    @Override
    public <T> T convert(HttpServletRequest request, Class<T> type) {
        return null;
    }
}
