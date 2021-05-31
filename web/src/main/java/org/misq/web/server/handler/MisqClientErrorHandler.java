package org.misq.web.server.handler;

import org.misq.web.json.JsonTransform;
import ratpack.error.ClientErrorHandler;
import ratpack.handling.Context;

import java.util.Map;

public class MisqClientErrorHandler extends AbstractHandler implements ClientErrorHandler {

    public MisqClientErrorHandler(JsonTransform jsonTransform) {
        super(jsonTransform);
    }

    @Override
    public void error(Context context, int statusCode) {
        String whatNotFound = context.getRequest().getPath();
        Map<String, String> error = toMap("error", Integer.toString(statusCode));
        error.put("message", whatNotFound + " not found");
        context.getResponse().status(statusCode).send(toJson(error));
    }

    @Override
    public void handle(Context ctx) {
        // not implemented
    }
}
