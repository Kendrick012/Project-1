import java.io.*;
import java.net.*;
import java.util.*;
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
                processURL(url);
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

            
            
        }catch (Exception e) {
            System.out.pritnln("Status: Network Error");
            }
    }

    private static void fetchReferencedImages(String html, URL baseURL) {

    }
