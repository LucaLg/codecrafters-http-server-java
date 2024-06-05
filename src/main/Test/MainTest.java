import org.junit.Test;

public class MainTest {
    @Test
    public void testHandleRequestLine() throws Exception {
        String got = "GET /user-agent HTTP/1.1\r\n";
        String[] want = { "GET", "/user-agent", "HTTP/1.1" };

        Method method = Main.class.getDeclaredMethod("handleRequestLine", String.class);
        method.setAccessible(true);
        String[] output = (String[]) method.invoke(null, got);

        assertArrayEquals(want, output);
    }

    @Test
    public void testBuildUrl() throws Exception {
        String request = "GET /user-agent HTTP/1.1\r\nHost: localhost:4221\r\nUser-Agent: curl/7.68.0\r\nAccept: */*\r\n\r\n";
        BufferedReader in = new BufferedReader(new StringReader(request));
        Url url = new Url();
        url.setRequestLine("GET /user-agent HTTP/1.1");
        url.setHeaders(new String[] { "Host: localhost:4221", "User-Agent: curl/7.68.0", "Accept: */*", "\r\n" });
        url.setBody(new String[] {});

        Method method = Main.class.getDeclaredMethod("buildUrl", String.class, BufferedReader.class);
        method.setAccessible(true);
        Url output = (Url) method.invoke(null, "", in);

        assertEquals(url, output);
    }

}
