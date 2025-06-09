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
