package com.test.httpserver.server;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.test.httpserver.network.HTTPRequest;
import com.test.httpserver.network.HTTPResponse;
import com.test.httpserver.util.HTTPResponseHandler;
import com.test.httpserver.util.HTTPStatus;

public abstract class MyHTTPServer implements HTTPResponseHandler
{
    public static final int SOCKET_READ_TIMEOUT = 5000;

    private String hostname ="127.0.0.1";
    private final int myPort;
    private ServerSocket httpServerSocket;
    private Thread httpServer;
    
    public static final String MIME_PLAINTEXT = "text/plain";
    public static final String MIME_HTML = "text/html";  
    
    ExecutorService executor = Executors.newFixedThreadPool(10);
    
    public MyHTTPServer(int port)
    {
        this(null,port);
    }
    
    public MyHTTPServer(String hostname,int port)
    {
        this.hostname = hostname;
        this.myPort = port;
    }
    
     /**
     * Start the server.
     * 
     * @throws IOException
     *             if the socket is in use.
     */
    public void start() throws IOException
    {
        System.out.println("============================================================");
        System.out.println("MyHTTPServer is starting up");
        httpServerSocket = new ServerSocket();
        httpServerSocket.bind((hostname != null) ? new InetSocketAddress(hostname, myPort) : new InetSocketAddress(myPort));

        httpServer = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                Runtime runtime = Runtime.getRuntime();
                runtime.addShutdownHook(new Thread() {
                    public void run(){
                        System.out.println("============================================================");
                        System.out.println("MyHTTPServer is shutting down");
                        HTTPServerUtils.closeAllConnections();
                        System.out.println("MyHTTPServer shut down completed");
                        System.out.println("============================================================");
                      }
                });
                while(true)
                {
                    try
                    {
                        final Socket finalAccept = httpServerSocket.accept();
                        HTTPServerUtils.registerConnection(finalAccept);
                        finalAccept.setSoTimeout(SOCKET_READ_TIMEOUT);
                        final InputStream inputStream = finalAccept.getInputStream();
                        if (inputStream == null)
                        {
                            HTTPServerUtils.safeClose(finalAccept);
                            HTTPServerUtils.unRegisterConnection(finalAccept);
                        }
                        else
                        {
                            processRequest(finalAccept);
                        }
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }                    
                }
            }
        });
        // TODO commented for testing; to be removed 
        //httpServer.setDaemon(true);
        httpServer.setName("Echo HTTP Server");
        httpServer.start();
        System.out.println("MyHTTPServer is ready and listening on "+ myPort);
        System.out.println("============================================================");
    }
    
    private void processRequest(final Socket finalAccept)
    {
        executor.submit(new Runnable()
        {
            @Override
            public void run()
            {
                OutputStream outputStream = null;
                HTTPResponse httpResponse = null;
                try
                {
                    outputStream = finalAccept.getOutputStream();
                    HTTPRequest httpRequest = new HTTPRequest(finalAccept.getInputStream(), new TempFileManager());
                    httpRequest.execute();
                    httpResponse = getResponse(httpRequest);
                    httpResponse.send(outputStream);
                }
                catch (ResponseException re)
                {
                    httpResponse= new HTTPResponse(re.getStatus(), MIME_PLAINTEXT, re.getMessage());
                    httpResponse.send(outputStream);
                }        
                catch (Exception ioe)
                {
                    httpResponse = new HTTPResponse(HTTPStatus.INTERNAL_ERROR, MIME_PLAINTEXT,"SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
                    httpResponse.send(outputStream);
                    ioe.printStackTrace();
                }
                finally
                {
                    HTTPServerUtils.safeClose(outputStream);
                }
            }
        });
    }

    private HTTPResponse getResponse(HTTPRequest httpRequest)
    {
        HTTPResponse httpResponse = null;
        switch (httpRequest.getMethod())
        {
        case GET:
            httpResponse = doGet(httpRequest);
            break;
        case POST:
            httpResponse = doPost(httpRequest);
            break;
        case PUT:
            httpResponse = doPut(httpRequest);
            break;
        case DELETE:
            httpResponse = doDelete(httpRequest);
            break;  
        case HEAD:
            httpResponse = doPut(httpRequest);
            break;
        case OPTIONS:
            httpResponse = doOptions(httpRequest);
            break;                
        }
        return httpResponse;
    }    
    
    
    /**
     * Stop the server.
     */
    public void stop()
    {
        try
        {
            HTTPServerUtils.safeClose(httpServerSocket);
            HTTPServerUtils.closeAllConnections();
            httpServer.join();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }   
    
    
    // utility methods
    
    public static final class ResponseException extends Exception
    {

        private final HTTPStatus status;

        public ResponseException(HTTPStatus status, String message)
        {
            super(message);
            this.status = status;
        }

        public ResponseException(HTTPStatus status, String message, Exception e)
        {
            super(message, e);
            this.status = status;
        }

        public HTTPStatus getStatus()
        {
            return status;
        }
    }    
    
    public static class HTTPServerUtils
    {
        private static Set<Socket> openConnections = new HashSet<Socket>();   
        
        public static final void safeClose(ServerSocket serverSocket)
        {
            if (serverSocket != null)
            {
                try
                {
                    serverSocket.close();
                }
                catch (IOException e)
                {
                }
            }
        }

        public static final void safeClose(Socket socket)
        {
            if (socket != null)
            {
                try
                {
                    socket.close();
                }
                catch (IOException e)
                {
                }
            }
        }

        public static final void safeClose(Closeable closeable)
        {
            if (closeable != null)
            {
                try
                {
                    closeable.close();
                }
                catch (IOException e)
                {
                }
            }
        }

        /**
         * Registers that a new connection has been set up.
         * 
         * @param socket
         *            the {@link Socket} for the connection.
         */
        public static synchronized void registerConnection(Socket socket)
        {
            openConnections.add(socket);
        }

        /**
         * Registers that a connection has been closed
         * 
         * @param socket
         *            the {@link Socket} for the connection.
         */
        public static synchronized void unRegisterConnection(Socket socket)
        {
            openConnections.remove(socket);
        }

        /**
         * Forcibly closes all connections that are open.
         */
        public static synchronized void closeAllConnections()
        {
            System.out.println("Closing all the connections!");
            for (Socket socket : openConnections)
            {
                safeClose(socket);
            }
        }
        
    }
    public class TempFileManager
    {
        private final String tmpdir;
        private final List<TempFile> tempFiles;

        public TempFileManager()
        {
            tmpdir = System.getProperty("java.io.tmpdir");
            tempFiles = new ArrayList<TempFile>();
        }

        public TempFile createTempFile() throws Exception
        {
            TempFile tempFile = new TempFile(tmpdir);
            tempFiles.add(tempFile);
            return tempFile;
        }

        public void clear()
        {
            for (TempFile file : tempFiles)
            {
                try
                {
                    file.delete();
                }
                catch (Exception ignored)
                {
                }
            }
            tempFiles.clear();
        }

        public class TempFile
        {
            private File file;
            private OutputStream fstream;

            public TempFile(String tempdir) throws IOException
            {
                file = File.createTempFile("myHTTPServer-", "", new File(tempdir));
                fstream = new FileOutputStream(file);
            }

            public OutputStream open() throws Exception
            {
                return fstream;
            }

            public void delete() throws Exception
            {
                HTTPServerUtils.safeClose(fstream);
                file.delete();
            }

            public String getName()
            {
                return file.getAbsolutePath();
            }
        }
    }    
    
    
}
