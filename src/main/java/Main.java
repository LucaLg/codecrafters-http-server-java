import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.CharBuffer;
import java.util.ArrayList;

public class Main {

  public static void main(String[] args) {
    String dir = "";
    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("--directory")) {
        if (i + 1 >= args.length) {
          System.out.println("Please provide a directory");
          System.exit(1);
        }
        dir = args[i + 1];
      }
    }
    Socket clientSocket = null;
    try (ServerSocket serverSocket = new ServerSocket(4221)) {
      serverSocket.setReuseAddress(true);
      while (true) {
        clientSocket = serverSocket.accept(); // Wait for connection from client.
        System.out.println("Connection established");
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        OutputStream out = clientSocket.getOutputStream();
        Url url = buildUrl(in);
        if ("POST".equals(url.getMethod())) {
          String res = handlePOST(url.requestLine, url.getHeaders(), url.getBody(), dir);
          out.write(res.getBytes());
        }
        if ("GET".equals(url.getMethod())) {
          String res = handleGET(url.requestLine, url.getHeaders(), url.body, dir);
          out.write(res.getBytes());
        }
        out.flush();
        out.close();
        in.close();
      }
    } catch (IOException e) {
      System.out.println("Error occurred");
      System.out.println("IOException: " + e.getMessage());
    }
  }

  public static Url buildUrl(BufferedReader in) throws IOException {
    Url url = new Url();
    ArrayList<String> headers = new ArrayList<String>();
    String r = in.readLine();
    String[] s = r.split(" ");
    url.setMethod(s[0]);
    url.setRequestLine(s[1]);
    r = in.readLine();
    while (r != null && !r.isEmpty()) {
      headers.add(r);
      r = in.readLine();
    }
    CharBuffer buff = CharBuffer.allocate(1024);
    if (in.ready()) {
      int l = in.read(buff);
      if (l == -1) {
        l = 0;
      }
      String body = "";
      for (int i = 0; i < l; i++) {
        body += buff.get(i);
      }
      url.setBody(body);
    }
    String[] h = headers.toArray(new String[0]);
    url.setHeaders(h);
    return url;
  }

  private static String handlePOST(String url, String[] headers, String body, String dir) {
    System.out.println(url);
    if (url.startsWith("/files/")) {
      String fileName = url.substring(7);
      File file = new File(dir, fileName);
      try {
        file.getParentFile().mkdirs();
        if (file.createNewFile()) {
          FileWriter myWriter = new FileWriter(file);
          myWriter.write(body);
          myWriter.close();
        }

        return "HTTP/1.1 201 Created\r\n\r\n";
      } catch (Exception e) {
        System.out.println("IOException: " + e.getMessage());
      }
    }
    return responseBuilder("201 Server Error", "", "");
  }

  private static String handleGET(String url, String[] headers, String body, String dir) {
    if (url.equals("/")) {
      return responseBuilder("200 OK", "", "");
    }
    if (url.startsWith("/echo/", 0)) {
      String inpuString = url.substring(6);
      String header = String.format("\r\nContent-Type: text/plain\r\nContent-Length: %d", inpuString.length());
      for (String h : headers) {
        if (h.equals("Accept-Encoding: gzip")) {
          header = "\r\nContent-Encoding: gzip" + header;
          break;
        }
      }
      return responseBuilder("200 OK", header, inpuString);
    }
    if (url.startsWith("/files/")) {
      String fileName = url.substring(7);
      return handleFile(fileName, dir);
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

  private static String handleFile(String fileName, String dir) {
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

    } catch (Exception e) {
      System.out.println(e.getMessage());
      return responseBuilder("404 Not Found", "", "");
    }
  }
}
