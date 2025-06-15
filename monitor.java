import java.io.*;
import java.net.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.*;

public class monitor {

    public static void main(String[] args) {
        // Accept file from args or default to "urls.txt"
        String fileName = args.length == 1 ? args[0] : "urls.txt";

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
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
            int port = (url.getPort() == -1) ? (protocol.equals("https") ? 443 : 80) : url.getPort();
            String path = url.getPath().isEmpty() ? "/" : url.getPath();

            Socket socket;
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

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
            writer.write("GET " + path + " HTTP/1.1\r\n");
            writer.write("Host: " + host + "\r\n");
            writer.write("Connection: close\r\n\r\n");
            writer.flush();

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
                if ((statusCode == 301 || statusCode == 302) && line.toLowerCase().startsWith("location:")) {
                    redirectURL = line.substring(9).trim();
                }
                html.append(line).append("\n");
            }

            socket.close();

            // Handle 3XX redirect
            if (redirectURL != null) {
                System.out.println("Redirected URL: " + redirectURL);
                processURL(redirectURL); // recursively follow redirect
            }

            // Handle 2XX: fetch referenced images
            if (statusCode >= 200 && statusCode < 300) {
                if (html.toString().toLowerCase().contains("<img")) {
                    fetchReferencedImages(html.toString(), url);
                }
            }

            System.out.println(); // blank line between URLs

        } catch (Exception e) {
            System.out.println("Status: Network Error\n");
        }
    }

    private static void fetchReferencedImages(String html, URL baseURL) {
        try {
            Pattern imgPattern = Pattern.compile("<img[^>]*src=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
            Matcher matcher = imgPattern.matcher(html);

            while (matcher.find()) {
                String imgSrc = matcher.group(1);
                String imgUrl;

                if (imgSrc.startsWith("http")) {
                    imgUrl = imgSrc;
                } else {
                    // Handle relative paths
                    imgUrl = baseURL.getProtocol() + "://" + baseURL.getHost();
                    if (baseURL.getPort() != -1) {
                        imgUrl += ":" + baseURL.getPort();
                    }
                    if (!imgSrc.startsWith("/")) {
                        String basePath = baseURL.getPath();
                        int lastSlash = basePath.lastIndexOf('/');
                        if (lastSlash != -1) {
                            imgUrl += basePath.substring(0, lastSlash + 1) + imgSrc;
                        } else {
                            imgUrl += "/" + imgSrc;
                        }
                    } else {
                        imgUrl += imgSrc;
                    }
                }

                try {
                    URL imageUrl = new URL(imgUrl);
                    String protocol = imageUrl.getProtocol();
                    String host = imageUrl.getHost();
                    int port = (imageUrl.getPort() == -1) ? (protocol.equals("https") ? 443 : 80) : imageUrl.getPort();
                    String path = imageUrl.getPath();

                    Socket socket;
                    if (protocol.equals("https")) {
                        SSLSocketFactory sslFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                        socket = sslFactory.createSocket(host, port);
                    } else {
                        socket = new Socket();
                        socket.connect(new InetSocketAddress(host, port), 5000);
                    }

                    PrintWriter writer = new PrintWriter(socket.getOutputStream());
                    writer.print("GET " + path + " HTTP/1.1\r\n");
                    writer.print("Host: " + host + "\r\n");
                    writer.print("Connection: close\r\n\r\n");
                    writer.flush();

                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String statusLine = reader.readLine();
                    String[] statusParts = statusLine != null ? statusLine.split(" ", 3) : new String[]{"", "000", "Network Error"};
                    String statusCode = statusParts.length > 1 ? statusParts[1] : "000";
                    String statusText = statusParts.length > 2 ? statusParts[2] : "Network Error";

                    System.out.println("Referenced URL: " + imgUrl);
                    System.out.println("Status: " + statusCode + " " + statusText);

                    socket.close();
                } catch (Exception ex) {
                    System.out.println("Referenced URL: " + imgUrl);
                    System.out.println("Status: Network Error");
                }
            }
        } catch (Exception e) {
            System.out.println("Error parsing images: " + e.getMessage());
        }
    }
}
