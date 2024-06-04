import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
  public static void main(String[] args) {

    ServerSocket serverSocket = null;
    Socket clientSocket = null;

    try {
      serverSocket = new ServerSocket(4221);
      serverSocket.setReuseAddress(true);
      clientSocket = serverSocket.accept(); // Wait for connection from client.
      BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

      String url = handleRequestLine(in.readLine())[1];
      String res = handleURLCheck(url);
      clientSocket.getOutputStream().write(res.getBytes());
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }
  }

  public static String[] handleRequest(String request) {
    return request.split("\r\n");
  }

  private static String[] handleRequestLine(String requestLine) {
    return requestLine.split(" ");
  }

  private static String handleURLCheck(String url) {
    if (url.equals("/")) {
      return responseBuilder("200 OK", "", "");

    } else if (url.startsWith("/echo/", 0)) {
      String inpuString = url.substring(6);
      String header = String.format("\r\nContent-Type: text/plain\r\nContent-Length: %d", inpuString.length());
      return responseBuilder("200 OK", header, inpuString);
    } else {
      return responseBuilder("404 Not Found", "", "");
    }
  }

  private static String responseBuilder(String response, String header, String input) {
    return String.format("%s%s%s%s%s", "HTTP/1.1 ", response, header, "\r\n\r\n", input);
  }
}
