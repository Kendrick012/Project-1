import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import javax.net.ssl.*;

public class monitor {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java monitor urls-file");
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(args[0]))) {
            String url;
            while ((url = br.readLine()) != null) {
                url = url.trim();
                if (!url.isEmpty()) {
                    processURL(url);
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading URL file: " + e.getMessage());
        }
    }
    private static void processURL(String urlStr) {
        try {
            System.out.println("URL: " + urlStr);
            URL url = new URL(urlStr);
            String protocol = url.getProtocol();
            String host = url.getHost();
            int port = url.getPort() == -1 ? (protocol.equals("https") ? 443 : 80) : url.getPort();
            String path = url.getPath().isEmpty() ? "/" : url.getPath();

            Socket sockets;
            if (protocol.equals("https")) {
                SSLSocketFactory sslFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                socket = sslFactory.createSocket(host, port);
            } else if (protocol.equals("http")) {
                socket = new Socket();
                socket.connect(new InetSocketAddress(host, port), 5000);
            } else {
                System.out.println("Status: Unsupported Protocol");
                return;
            }

            //request
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
            writer.write("GET " + path + " HTTP/1.1\r\n");
            writer.write("Host: " + host + "\r\n");
            writer.write("Connection: close\r\n\r\n");
            writer.flush();

            //responce
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String statusLine = reader.readLine();
            if (statusLine == null || !statusLine.startsWith("HTTP")) {
                System.out.println("Status: Invalid HTTP Response");
                socket.close();
                return;
            }
            int statusCode = Integer.parseInt(statusLine.split(" ")[1]);
            String statusMsg = statusLine.substring(statusLine.indexOf(" ") + 1);
            System.out.println("Status: " + statusMsg);

            String line;
            StringBuilder html = new StringBuilder();
            String redirectURL = null;

            while ((line = reader.readLine()) != null) {
                if (statusCode == 301 || statusCode == 302) {
                    if (line.toLowerCase().startsWith("location:")) {
                        redirectURL = line.substring(9).trim();
                    }
                }
                html.append(line).append("\n");
            }

            socket.close();
            //follow
            if (redirectURL != null) {
                System.out.println("Redirected URL: " + redirectURL);
                processURL(redirectURL);
            }

            //fetch
            if (statusCode == 200 && html.toString().contains("<img")) {
                fetchReferencedImages(html.toString(), url);
            }
            
        }catch (Exception e) {
            System.out.pritnln("Status: Network Error");
            }
    }

    private static void fetchReferencedImages(String html, URL baseURL) {

    }
