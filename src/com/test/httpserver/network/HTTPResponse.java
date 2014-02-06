package com.test.httpserver.network;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import com.test.httpserver.server.MyHTTPServer.HTTPServerUtils;
import com.test.httpserver.util.HTTPStatus;
import com.test.httpserver.util.Method;

public class HTTPResponse
{
    
    /**
     * Common mime type for dynamic content: plain text
     */
    public static final String MIME_PLAINTEXT = "text/plain";
    /**
     * Common mime type for dynamic content: html
     */
    public static final String MIME_HTML = "text/html";

    /**
     * HTTP status code after processing, e.g. "200 OK", HTTP_OK
     */
    private HTTPStatus status;
    /**
     * MIME type of content, e.g. "text/html"
     */
    private String mimeType;
    /**
     * Data of the response, may be null.
     */
    private InputStream data;
    /**
     * Headers for the HTTP response. Use addHeader() to add lines.
     */
    private Map<String, String> header = new HashMap<String, String>();
    /**
     * The request method that spawned this response.
     */
    private Method requestMethod;
    /**
     * Use chunkedTransfer
     */
    private boolean chunkedTransfer;

    /**
     * Default constructor: response = HTTP_OK, mime = MIME_HTML and your supplied message
     */
    public HTTPResponse(String msg)
    {
        this(HTTPStatus.OK, MIME_HTML, msg);
    }

    /**
     * Basic constructor.
     */
    public HTTPResponse(HTTPStatus status, String mimeType, InputStream data)
    {
        this.status = status;
        this.mimeType = mimeType;
        this.data = data;
    }

    /**
     * Convenience method that makes an InputStream out of given text.
     */
    public HTTPResponse(HTTPStatus status, String mimeType, String txt)
    {
        this.status = status;
        this.mimeType = mimeType;
        try
        {
            this.data = txt != null ? new ByteArrayInputStream(txt.getBytes("UTF-8")) : null;
        }
        catch (java.io.UnsupportedEncodingException uee)
        {
            uee.printStackTrace();
        }
    }

    /**
     * Adds given line to the header.
     */
    public void addHeader(String name, String value)
    {
        header.put(name, value);
    }

    /**
     * Sends given response to the socket.
     */
    public void send(OutputStream outputStream)
    {
        String mime = mimeType;
        SimpleDateFormat gmtFrmt = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));

        try
        {
            if (status == null)
            {
                throw new Error("sendResponse(): Status can't be null.");
            }
            PrintWriter pw = new PrintWriter(outputStream);
            pw.print("HTTP/1.1 " + status.getDescription() + " \r\n");

            if (mime != null)
            {
                pw.print("Content-Type: " + mime + "\r\n");
            }

            if (header == null || header.get("Date") == null)
            {
                pw.print("Date: " + gmtFrmt.format(new Date()) + "\r\n");
            }

            if (header != null)
            {
                for (String key : header.keySet())
                {
                    String value = header.get(key);
                    pw.print(key + ": " + value + "\r\n");
                }
            }

            // Not required as per the assignment
            // pw.print("Connection: keep-alive\r\n");

            if (requestMethod != Method.HEAD && chunkedTransfer)
            {
                sendAsChunked(outputStream, pw);
            }
            else
            {
                sendAsFixedLength(outputStream, pw);
            }
            outputStream.flush();
            //close the stream
            HTTPServerUtils.safeClose(data);
        }
        catch (IOException ioe)
        {
            // Couldn't write? No can do.
        }
    }

    private void sendAsChunked(OutputStream outputStream, PrintWriter pw) throws IOException
    {
        pw.print("Transfer-Encoding: chunked\r\n");
        pw.print("\r\n");
        pw.flush();
        int BUFFER_SIZE = 16 * 1024;
        byte[] CRLF = "\r\n".getBytes();
        byte[] buff = new byte[BUFFER_SIZE];
        int read;
        while ((read = data.read(buff)) > 0)
        {
            outputStream.write(String.format("%x\r\n", read).getBytes());
            outputStream.write(buff, 0, read);
            outputStream.write(CRLF);
        }
        outputStream.write(String.format("0\r\n\r\n").getBytes());
    }

    private void sendAsFixedLength(OutputStream outputStream, PrintWriter pw) throws IOException
    {
        int pending = data != null ? data.available() : 0; // This is to support partial sends, see
                                                           // serveFile()
        pw.print("Content-Length: " + pending + "\r\n");

        pw.print("\r\n");
        pw.flush();

        if (requestMethod != Method.HEAD && data != null)
        {
            int BUFFER_SIZE = 16 * 1024;
            byte[] buff = new byte[BUFFER_SIZE];
            while (pending > 0)
            {
                int read = data.read(buff, 0, ((pending > BUFFER_SIZE) ? BUFFER_SIZE : pending));
                if (read <= 0)
                {
                    break;
                }
                outputStream.write(buff, 0, read);

                pending -= read;
            }
        }
    }

    public HTTPStatus getStatus()
    {
        return status;
    }

    public void setStatus(HTTPStatus status)
    {
        this.status = status;
    }

    public String getMimeType()
    {
        return mimeType;
    }

    public void setMimeType(String mimeType)
    {
        this.mimeType = mimeType;
    }

    public InputStream getData()
    {
        return data;
    }

    public void setData(InputStream data)
    {
        this.data = data;
    }

    public Method getRequestMethod()
    {
        return requestMethod;
    }

    public void setRequestMethod(Method requestMethod)
    {
        this.requestMethod = requestMethod;
    }

    public void setChunkedTransfer(boolean chunkedTransfer)
    {
        this.chunkedTransfer = chunkedTransfer;
    }

}
