package org.netmelody.cieye.server.response.resource;

import java.io.IOException;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.netmelody.cieye.core.logging.LogKeeper;
import org.netmelody.cieye.core.logging.Logbook;
import org.netmelody.cieye.server.response.CiEyeResponder;
import org.netmelody.cieye.server.response.CiEyeResponse;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.Status;
import org.simpleframework.http.resource.Resource;

public final class CiEyeResource implements Resource {

    private static final Logbook LOG = LogKeeper.logbookFor(CiEyeResource.class);
    
    private final CiEyeResponder responder;

    public CiEyeResource(CiEyeResponder responder) {
        this.responder = responder;
    }
    
    @Override
    public void handle(Request request, Response response) {
        try {
            //kalder LandscapeObservationResponder.respond();
            final CiEyeResponse result = responder.respond(request);
            response.set("Content-Type", result.contentType);
            response.set("Server", "CiEye/1.0 (Simple 4.0)");
            response.setDate("Date", result.date);
            response.setDate("Last-Modified", result.lastModified);
            response.setDate("Expires", result.expires);
            response.setContentLength(result.contentLength());
            response.setCode(result.status.getCode());
            response.setText(result.status.getDescription());
            //result.additionalStringHeaders.entrySet() full of nulls
            for (Entry<String, String> header : result.additionalStringHeaders.entrySet()) {
                response.set(header.getKey(), header.getValue());
            }
            IOUtils.copy(result.inputStream(), response.getOutputStream());
        }
        catch (Exception e) {
            e.printStackTrace(System.out);
            System.out.println(e);
            LOG.error("Failed to respond to request for resource " + request.getPath().getPath(), e);
            
            
            response.setCode(Status.NOT_FOUND.getCode());
            response.setText(Status.NOT_FOUND.getDescription());
        }
        finally {
            try {
                response.close();
            } catch (IOException e) {
                LOG.error("Failed to close response for resource " + request.getPath().getPath(), e);
            }
        }
    }
}
