package com.test.httpserver.examples;

import com.test.httpserver.network.HTTPRequest;
import com.test.httpserver.network.HTTPResponse;
import com.test.httpserver.server.MyHTTPServer;
import com.test.httpserver.util.HTTPStatus;

public class MyEchoServer extends MyHTTPServer
{

    public MyEchoServer(int port)
    {
        super("127.0.0.1",port);
    }

    public static void main(String[] args)
    {
        int port = 8080;
        if(args.length<1 )
        {
            usage();
        }
        else
        {
            try
            {
                port = Integer.valueOf(args[0]);
            }
            catch (NumberFormatException e)
            {
                System.out.println("You specified invalid port; server will be started on default port  "+port);
            }
            MyEchoServer server = new MyEchoServer(port);
            try
            {
                server.start();
            }
            catch (Exception e)
            {
                System.out.println("Server start failed with error "+e.getMessage());
                e.printStackTrace();
            }
            
        }
    }

    private static void usage()
    {
        System.out.println("Please run Server from commandline as instructed below.");
        System.out.println("============================================================");
        System.out.println("java -jar /path/to/MyHTTPServer.jar intPortNumber");
        System.out.println();
        System.out.println("Note: Make sure you give port > 1024");
        System.out.println("============================================================");
        
    }
    @Override
    public HTTPResponse doGet(HTTPRequest request)
    {
        HTTPResponse response = new HTTPResponse(HTTPStatus.METHOD_NOT_ALLOWED, MIME_PLAINTEXT,"GET NOT IMPLEMENTED: ");
        return response;
    }

    @Override
    public HTTPResponse doPost(HTTPRequest request)
    {
        // returns back with same payload and content-type
        HTTPResponse response = new HTTPResponse(HTTPStatus.OK,request.getHeaders().get("content-type"),new String(request.getPayload()));
        return response;
    }

    @Override
    public HTTPResponse doPut(HTTPRequest request)
    {
        HTTPResponse response = new HTTPResponse(HTTPStatus.METHOD_NOT_ALLOWED, MIME_PLAINTEXT,"PUT NOT IMPLEMENTED: ");
        return response;
    }

    @Override
    public HTTPResponse doDelete(HTTPRequest request)
    {
        HTTPResponse response = new HTTPResponse(HTTPStatus.METHOD_NOT_ALLOWED, MIME_PLAINTEXT,"DELETE NOT IMPLEMENTED: ");
        return response;
    }

    @Override
    public HTTPResponse doHead(HTTPRequest request)
    {
        HTTPResponse response = new HTTPResponse(HTTPStatus.METHOD_NOT_ALLOWED, MIME_PLAINTEXT,"HEAD NOT IMPLEMENTED: ");
        return response;
    }

    @Override
    public HTTPResponse doOptions(HTTPRequest request)
    {
        HTTPResponse response = new HTTPResponse(HTTPStatus.METHOD_NOT_ALLOWED, MIME_PLAINTEXT,"OPTIONS NOT IMPLEMENTED: ");
        return response;
    }

}
