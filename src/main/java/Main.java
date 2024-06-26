import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
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
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

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
    final String directory = dir;
    // Create a server socket that listens on port 4221
    try (ServerSocket serverSocket = new ServerSocket(4221)) {
      serverSocket.setReuseAddress(true);
      // Accept incoming connections and handle them in a new thread
      while (true) {
        Socket clientSocket = serverSocket.accept();
        new Thread(() -> {
          try {
            System.out.println("Connection established");
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            OutputStream out = clientSocket.getOutputStream();
            Url url = buildUrl(in);
            if ("POST".equals(url.getMethod())) {
              String res = handlePOST(url.requestLine, url.getHeaders(), url.getBody(), directory, out);
              out.write(res.getBytes());
            }
            if ("GET".equals(url.getMethod())) {
              handleGET(url.requestLine, url.getHeaders(), url.body, directory, out);
            }
            out.flush();
            out.close();
            in.close();
          } catch (IOException e) {
            System.out.println("Error occurred");
            System.out.println("IOException: " + e.getMessage());
          }
        }).start();
      }
    } catch (IOException e) {
      System.out.println("Error occurred");
      System.out.println("IOException: " + e.getMessage());
    }
  }

  /***
   * Builds a Url object from the input stream
   * 
   * @param in
   * @return URL
   * @throws IOException
   */
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

  /***
   * Handles POST requests based on the request line and headers, writes the
   * response to the output stream
   * 
   * @param url
   * @param headers
   * @param body
   * @param dir
   * @param out
   * @return
   */
  private static String handlePOST(String url, String[] headers, String body, String dir, OutputStream out) {
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
        out.write("HTTP/1.1 201 Created\r\n\r\n".getBytes());
        return "HTTP/1.1 201 Created\r\n\r\n";
      } catch (Exception e) {
        System.out.println("IOException: " + e.getMessage());
      }
    }
    return responseBuilder("201 Server Error", "", "");
  }

  /***
   * Handles GET requests based on the request line and headers, writes the
   * response to the output stream
   * 
   * @param url
   * @param headers
   * @param body
   * @param dir
   * @param out
   * @return
   * @throws IOException
   */
  private static String handleGET(String url, String[] headers, String body, String dir, OutputStream out)
      throws IOException {
    String res = "";
    if (url.equals("/")) {
      res = responseBuilder("200 OK", "", "");
      out.write(res.getBytes());
    }
    if (url.startsWith("/echo/", 0)) {
      String inpuString = url.substring(6);
      byte[][] encodingArr = handleEncodding(headers, inpuString);
      byte[] reqLine = "HTTP/1.1 200 OK".getBytes();
      out.write(reqLine);
      out.write(encodingArr[0]);
      out.write(encodingArr[1]);
      return "";
    }
    if (url.startsWith("/files/")) {
      String fileName = url.substring(7);
      res = handleFile(fileName, dir);
      out.write(res.getBytes());
    }
    if (url.equals("/user-agent")) {
      res = handleUserAgent(headers);
      out.write(res.getBytes());
      return res;
    } else {
      res = responseBuilder("404 Not Found", "", "");
      out.write(res.getBytes());
    }
    return res;
  }

  /**
   * Takes in a string and returns a byte array of the compressed string with gzip
   * 
   * @param inputString
   * @return
   * @throws IOException
   */
  private static byte[] encodeString(String inputString) throws IOException {
    ByteArrayOutputStream bais = new ByteArrayOutputStream();
    try (GZIPOutputStream gos = new GZIPOutputStream(bais)) {
      gos.write(inputString.getBytes());
    }
    return bais.toByteArray();
  }

  /**
   * Takes in headers and inputString and returns a byte[][] for response header
   * and body based on the given encoding header and methods
   * 
   * @param headers
   * @param inputString
   * @return byte[][] for response header and body
   */
  private static byte[][] handleEncodding(String[] headers, String inputString) {
    String resHeader = String.format("\r\nContent-Type: text/plain\r\nContent-Length: %d\r\n\r\n",
        inputString.length());
    byte[][] resArr = new byte[][] { resHeader.getBytes(), inputString.getBytes() };
    try {
      Stream<String> headersStream = Arrays.stream(headers);
      Optional<String> encodingHeader = headersStream.filter(h -> h.startsWith("Accept-Encoding: ")).findAny();
      encodingHeader.ifPresent(h -> {
        Stream<String> encodings = Arrays.stream(h.split(":")[1].split(","));
        encodings.filter(e -> e.trim().equals("gzip")).findAny().ifPresent(e -> {
          try {
            byte[] zip = encodeString(inputString);
            resArr[0] = String
                .format("\r\nContent-Encoding: gzip\r\nContent-Type: text/plain\r\nContent-Length: %d\r\n\r\n",
                    zip.length)
                .getBytes();
            resArr[1] = zip;
          } catch (IOException e1) {
            e1.printStackTrace();
          }
        });
      });
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
    return resArr;
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

  /**
   * Handles file requests based on the file name and directory, writes the
   * response to the output stream
   * 
   * @param fileName
   * @param dir
   * @return
   */
  private static String handleFile(String fileName, String dir) {
    try {
      String path = dir.concat(fileName);
      FileReader f = new FileReader(path);
      CharBuffer buf = CharBuffer.allocate(1024);
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

  /**
   * Response Builder helper function
   * 
   * @param response
   * @param header
   * @param input
   * @return
   */
  private static String responseBuilder(String response, String header, String input) {
    return String.format("%s%s%s%s%s", "HTTP/1.1 ", response, header, "\r\n\r\n", input);
  }
}
