import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.CharBuffer;
import java.util.ArrayList;

public class Main {
  static String dir;

  public static void main(String[] args) {
    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("--directory")) {
        if (i + 1 >= args.length) {
          System.out.println("Please provide a directory");
          System.exit(1);
        }
        dir = args[i + 1];
      }
    }
    ServerSocket serverSocket = null;
    Socket clientSocket = null;
    try {
      serverSocket = new ServerSocket(4221);
      serverSocket.setReuseAddress(true);
      while (true) {
        clientSocket = serverSocket.accept(); // Wait for connection from client.
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        Url url = buildUrl("", in);
        String res = handleURLCheck(url.requestLine.split(" ")[1], url.headers, url.body);
        clientSocket.getOutputStream().write(res.getBytes());
        clientSocket.getOutputStream().flush();
        clientSocket.close();
      }
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }
  }

  private static Url buildUrl(String request, BufferedReader in) throws IOException {
    int track = 0;
    Url url = new Url();
    ArrayList<String> headers = new ArrayList<String>();
    ArrayList<String> body = new ArrayList<String>();
    String r = in.readLine();
    url.setRequestLine(r);
    while (r != null && !r.isEmpty()) {
      if (track == 0) {
        headers.add(r);
        if (r.equals("\r\n")) {
          track++;
        }
      }
      if (track == 1) {
        body.add(r);
      }
      r = in.readLine();
    }
    String[] h = headers.toArray(String[]::new);
    String[] b = body.toArray(String[]::new);
    url.setHeaders(h);
    url.setBody(b);
    return url;
  }

  private static String handleURLCheck(String url, String[] headers, String[] body) {
    System.out.println(url);
    if (url.equals("/")) {
      return responseBuilder("200 OK", "", "");
    }
    if (url.startsWith("/echo/", 0)) {
      String inpuString = url.substring(6);
      String header = String.format("\r\nContent-Type: text/plain\r\nContent-Length: %d", inpuString.length());
      return responseBuilder("200 OK", header, inpuString);
    }
    if (url.startsWith("/files/")) {
      String fileName = url.substring(7);
      return handleFile(fileName);
    }
    if (url.equals("/user-agent")) {
      String res = handleUserAgent(headers);
      return res;
    } else {
      return responseBuilder("404 Not Found", "", "");
    }
  }

  private static String responseBuilder(String response, String header, String input) {
    return String.format("%s%s%s%s%s", "HTTP/1.1 ", response, header, "\r\n\r\n", input);
  }

  private static String handleUserAgent(String[] headers) {
    String userAgeString = "";
    for (String header : headers) {
      if (header.startsWith("User-Agent: ")) {
        userAgeString = header.split(":")[1].trim();
        break;
      }
    }
    int l = userAgeString.length();
    String resHeader = "\r\nContent-Type: text/plain\r\nContent-Length: " + String.valueOf(l);
    return responseBuilder("200 OK", resHeader, userAgeString);
  }

  private static String handleFile(String fileName) {
    try {
      String path = dir.concat(fileName);
      FileReader f = new FileReader(path);
      CharBuffer buf = CharBuffer.allocate(1024); // Replace Buffer with ByteBuffer
      int i = f.read(buf);
      String resHeader = "\r\nContent-Type: application/octet-stream\r\nContent-Length: " + String.valueOf(i);
      String fileString = "";
      for (char c : buf.array()) {
        fileString += c;
      }
      f.close();
      return responseBuilder("200 OK", resHeader, fileString);

    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
      return responseBuilder("404 Not Found", "", "");
    }
  }
}
