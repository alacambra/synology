package tech.lacambra.synology;

import org.jboss.resteasy.client.jaxrs.internal.ClientInvocationBuilder;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

//@Path("webapi/entry.cgi?api=SYNO.FileStation.Upload&method=upload&version=2")
public class FileUploadClient {

  private static final Logger LOGGER = Logger.getLogger("a");
  private final String uploadFileUrl;
  private final String sid;
  private final Object checkPermisions;
  private WebTarget webTarget;

  static {
    Logger.getLogger("sun.net.www.protocol.http.HttpURLConnection").setLevel(Level.ALL);
  }

  public FileUploadClient(WebTarget webTarget, String sid) {
    this.webTarget = webTarget;
    this.sid = sid;
    this.uploadFileUrl = "entry.cgi?" +
        "api=SYNO.FileStation.Upload&method=upload&version=2&" +
        "_sid=" + sid;
    this.checkPermisions = "entry.cgi?api=SYNO.FileStation.Upload&method=upload&version=2&_sid=" + sid;
  }

  public static class UploadFormData {
    boolean overwrite;
    String path;
    String fileName;
    MediaType contentType;

    public UploadFormData(boolean overwrite, String path, String fileName, MediaType contentType) {
      this.overwrite = overwrite;
      this.path = path;
      this.fileName = fileName;
      this.contentType = contentType;
    }

    public boolean isOverwrite() {
      return overwrite;
    }

    public String getPath() {
      return path;
    }

    public String getFileName() {
      return fileName;
    }

    public MediaType getContentType() {
      return contentType;
    }
  }


  public void uploadFile(UploadFormData uploadFormData, String filePath) throws IOException {

    MultipartFormDataOutput dataOutput = new MultipartFormDataOutput();
    dataOutput.addFormData("mtime", System.currentTimeMillis() + "", MediaType.MULTIPART_FORM_DATA_TYPE);
    dataOutput.addFormData("overwrite", uploadFormData.isOverwrite() + "", MediaType.MULTIPART_FORM_DATA_TYPE);
    dataOutput.addFormData("path", uploadFormData.getPath(), MediaType.MULTIPART_FORM_DATA_TYPE);


    File file = Paths.get(filePath).toFile();

    dataOutput.addFormData("size", file.length() + "", new MediaType("", ""));

    String fileKey = String.format("%s\"; filename=\"%s", "file", uploadFormData.getFileName());
    dataOutput.addFormData(fileKey, file, MediaType.APPLICATION_XML_TYPE);

    ClientInvocationBuilder request = (ClientInvocationBuilder) webTarget.path(uploadFileUrl).request();
    Response r = request.post(Entity.entity(dataOutput, MediaType.MULTIPART_FORM_DATA));
    System.out.println(r.getStatus());
    System.out.println(r.readEntity(String.class));

  }

  public void checkPermissions() throws IOException {

    Form form = new Form();
    form.param("path", "'/downloader-albert'")
        .param("filename", "pom.xml")
        .param("overwrite", "true");

    form = SynoAPIResource.UploadFileCheckWritePermissions.appendToForm(form);

    Response r = webTarget.path("entry.cgi")
        .queryParam("_sid", sid)
        .request()
        .header("Referer", "https://lacambra.de/file/")
        .post(
            Entity.entity(
//                form,
                "path=%22%2Fdownloader-albert%22&filename=%22pom.xml%22&size=1816&overwrite=true&api=SYNO.FileStation.CheckPermission&method=write&version=3&_sid=" + sid,
                "application/x-www-form-urlencoded; charset=UTF-8"
            )
        );

    System.out.println(r.getStatus());
    System.out.println(r.readEntity(String.class));

  }


  public void nativeCall(Properties properties) {
    String boundary = Long.toHexString(System.currentTimeMillis());


    PrintWriter writer = null;
    try {

      StringWriter stringWriter = new StringWriter();
      writer = new PrintWriter(stringWriter);
      File file = new File(properties.getProperty("test-file", "-"));
      createAndWriteMessage(writer, boundary, file);
      writer.close();

      String url = "https://{server-url}/webapi/".replaceAll("\\{server-url\\}", properties.getProperty("server-url")) + uploadFileUrl;
      System.out.println(url);
      System.out.println(stringWriter.toString());
      HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();

      connection.setDoOutput(true);
//      connection.setDoInput(true);
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
      connection.getOutputStream().write(stringWriter.toString().getBytes());
      int responseCode = connection.getResponseCode();
      System.out.println("response: " + responseCode);
      System.out.println("response: " + connection.getResponseMessage());

//      try (InputStream stream = connection.getInputStream()) {
//        int b;
//        List<Byte> bytes = new ArrayList<>();
//        try {
//          while ((b = stream.read()) != -1) {
//            ByteBuffer bs = ByteBuffer.allocate(4).putInt(b);
//            bytes.add(bs.get(0));
//            bytes.add(bs.get(1));
//            bytes.add(bs.get(2));
//            bytes.add(bs.get(3));
//          }
//        } catch (Exception e) {
//
//        }
//        Byte[] f = new Byte[bytes.size()];
//        ByteBuffer buff = ByteBuffer.allocate(f.length);
//        for (Byte aByte : bytes) {
//          buff.put(aByte);
//        }
//        f = bytes.toArray(f);
//        System.out.println(new String(buff.array()));
//      }
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
        for (String line; (line = reader.readLine()) != null; ) {
          System.out.println(line);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (writer != null) {
        writer.close();
      }
    }

  }


  /**
   * true
   * -----------------------------400214434485209834926272379
   * Content-Disposition: form-data; name=\"path\"
   * <p>
   * /home/albert
   *
   * @param writer
   * @param boundary
   * @param file
   * @throws IOException
   */
  private void createAndWriteMessage(PrintWriter writer, String boundary, File file) throws IOException {
    addPart(writer, boundary, "form-data; name=\\\"api\\\"", SynoAPIResource.UploadFileCheckWritePermissions.getApi());
    addPart(writer, boundary, "form-data; name=\\\"method\\\"", SynoAPIResource.UploadFileCheckWritePermissions.getMethod());
    addPart(writer, boundary, "form-data; name=\\\"version\\\"", SynoAPIResource.UploadFileCheckWritePermissions.getVersion() + "");

    addPart(writer, boundary, "form-data; name=\\\"mtime\\\"", String.valueOf(System.currentTimeMillis()));
    addPart(writer, boundary, "form-data; name=\\\"overwrite\\\"", String.valueOf(true));
    addPart(writer, boundary, "form-data; name=\\\"path\\\"", "/downloader-albert");
    addPart(writer, boundary, "form-data; name=\\\"size\\\"", String.valueOf(file.length()));
    addPart(writer, boundary, "form-data; name=\\\"" + file.getName() + "\"; filename=\"" + file.getName() + "\\\"", " text/xml; charset=UTF-8", file);
  }

  private int addPart(PrintWriter writer, String boundary, String contentDisposition, String value) {

    int size = 0;
    String s = logAndReturn("--" + boundary);
    size += s.getBytes().length;
    writer.println(s);

    s = logAndReturn("Content-Disposition: " + contentDisposition);
    size += s.getBytes().length;
    writer.println(s);

    s = logAndReturn(value);
    size += s.getBytes().length;
    writer.println(s);


    return size;
  }

  private String logAndReturn(String str) {
//    System.out.println(str);
    return str;
  }

  private long addPart(PrintWriter writer, String boundary, String contentDisposition, String contentType, File file) throws IOException {

    int size = addPart(writer, boundary, contentDisposition, "Content-Type: " + contentType);

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
      for (String line; (line = reader.readLine()) != null; ) {
        writer.println(logAndReturn(line));
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return size + file.length();
  }

  public static void r() {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("http://openjdk.java.net/"))
        .build();
    client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
        .thenApply(HttpResponse::body)
        .thenAccept(System.out::println)
        .join();
  }

  public static FileUploadClient connect(Properties properties) {

    WebTarget webTarget = ClientBuilder.newBuilder().build().target("https://{server-url}/webapi/".replaceAll("\\{server-url\\}", properties.getProperty("server-url")));
    String body = webTarget.path("auth.cgi")
        .queryParam("api", "SYNO.API.Auth")
        .queryParam("version", "6")
        .queryParam("method", "login")
        .queryParam("account", properties.getOrDefault("user", "-"))
        .queryParam("passwd", properties.getOrDefault("password", "-"))
        .queryParam("session", "FileStation")
        .queryParam("format", "sid")
        .request()
        .get().readEntity(String.class);


    JsonObject jsonObject = Json.createReader(new InputStreamReader(new ByteArrayInputStream(body.getBytes()))).readObject();

    if (jsonObject.getBoolean("success")) {
      String sid = jsonObject.getJsonObject("data").getString("sid");
      return new FileUploadClient(webTarget, sid);
    }

    throw new RuntimeException("Error: " + jsonObject.toString());
  }


  public static void main(String[] args) throws Exception {

    Properties properties = new Properties();
    try {
      properties.load(FileUploadClient.class.getClassLoader().getResourceAsStream("config.properties"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    FileUploadClient fileUploadClient = FileUploadClient.connect(properties);
    UploadFormData uploadFormData = new UploadFormData(true, "/downloader-albert", "my-test-file.txt", new MediaType("text", "html"));
//    fileUploadClient.uploadFile(uploadFormData,properties.getProperty("test-file"));
    fileUploadClient.checkPermissions();
    fileUploadClient.nativeCall(properties);

  }

}
