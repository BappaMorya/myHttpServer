package com.test.httpserver.util;

import com.test.httpserver.network.HTTPRequest;
import com.test.httpserver.network.HTTPResponse;

public interface HTTPResponseHandler
{
    public HTTPResponse doGet(HTTPRequest request);
    public HTTPResponse doPost(HTTPRequest request);
    public HTTPResponse doPut(HTTPRequest request);
    public HTTPResponse doDelete(HTTPRequest request);
    public HTTPResponse doHead(HTTPRequest request);
    public HTTPResponse doOptions(HTTPRequest request);
}
