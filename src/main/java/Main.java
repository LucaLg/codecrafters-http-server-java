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
    String ok = "HTTP/1.1 200 OK\r\n\r\n";
    String notFound = "HTTP/1.1 404 Not Found\r\n\r\n";
    if (url.equals("/")) {
      return ok;
    } else {
      return notFound;
    }
  }
}
