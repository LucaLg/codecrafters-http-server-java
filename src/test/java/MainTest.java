import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import org.junit.Test;

public class MainTest {
    @Test
    public void testBuildUrl() throws IOException {
        String input = "GET /index.html HTTP/1.1\r\n" +
                "Host: localhost:4221\r\n" +
                "User-Agent: curl/7.64.1\r\n" +
                "Accept: */*\r\n" +
                "\r\n" + "body";
        BufferedReader in = new BufferedReader(new StringReader(input));
        Url url = Main.buildUrl(in);
        String[] expectedHeaders = new String[] { "Host: localhost:4221", "User-Agent: curl/7.64.1", "Accept: */*" };
        assertEquals("GET", url.getMethod());
        assertEquals("/index.html", url.getRequestLine());
        assertArrayEquals(expectedHeaders, url.getHeaders());
        assertEquals("body", url.getBody());
    }
}
