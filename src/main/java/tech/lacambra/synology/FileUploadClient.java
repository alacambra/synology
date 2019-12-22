package tech.lacambra.synology;

import org.jboss.resteasy.client.jaxrs.internal.ClientInvocationBuilder;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Properties;

//@Path("webapi/entry.cgi?api=SYNO.FileStation.Upload&method=upload&version=2")
public class FileUploadClient {

  private final String url;
  private WebTarget webTarget;

  public FileUploadClient(WebTarget webTarget, String sid) {
    this.webTarget = webTarget;
    this.url = "entry.cgi?api=SYNO.FileStation.Upload&method=upload&version=2&sid=" + sid;
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

    ClientInvocationBuilder request = (ClientInvocationBuilder) webTarget.path(url).request();
    Response r = request.post(Entity.entity(dataOutput, MediaType.MULTIPART_FORM_DATA));
    System.out.println(r.getStatus());
    System.out.println(r.readEntity(String.class));

  }


  public void nativeCall(Properties properties) {
    String boundary = Long.toHexString(System.currentTimeMillis());


    PrintWriter writer = null;
    try {
      URLConnection connection = new URL("https://{server-url}/webapi/".replaceAll("\\{server-url\\}", properties.getProperty("server-url")) + url + "a").openConnection();
      connection.setDoOutput(true);

      System.out.println(url);

//      try (InputStream inputStream = connection.getInputStream()) {
//
//      }

      connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
      writer = new PrintWriter(new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8));

      writer.println("path=%22%2Fdownloader-albert%22&filename=%22api.json%22&size=122653&overwrite=true&api=SYNO.FileStation.CheckPermission&method=write&version=3");
//      File file = new File(properties.getProperty("test-file", "-"));
//
//      addPart(writer, boundary, "form-data; name=\"mtime\"", String.valueOf(System.currentTimeMillis()));
//      addPart(writer, boundary, "form-data; name=\"overwrite\"", String.valueOf(true));
//      addPart(writer, boundary, "form-data; name=\"path\"", "/downloader-albert");
//      addPart(writer, boundary, "form-data; name=\"size\"", String.valueOf(file.length()));
//      addPart(writer, boundary, "form-data; name=\"" + file.getName() + "\"; filename=\"" + file.getName() + "\"", " text/xml; charset=UTF-8", file);
//
//      writer.println("--" + boundary + "--");

      int responseCode = ((HttpURLConnection) connection).getResponseCode();
      System.out.println("response: " + responseCode);
      System.out.println("response: " + ((HttpURLConnection) connection).getResponseMessage());

      try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
        for (String line; (line = reader.readLine()) != null; ) {
          System.out.println(line);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (writer != null) {
        writer.close();
      }
    }

  }

  private void addPart(PrintWriter writer, String boundary, String contentDisposition, String value) {
    writer.println(logAndReturn("--" + boundary));
    writer.println(logAndReturn("Content-Disposition: " + contentDisposition));
    writer.println(logAndReturn(value));
    writer.println(logAndReturn(""));
  }

  private String logAndReturn(String str) {
    System.out.println(str);
    return str;
  }

  private void addPart(PrintWriter writer, String boundary, String contentDisposition, String contentType, File file) throws IOException {
    writer.println(logAndReturn("--" + boundary));
    writer.println(logAndReturn("Content-Disposition: " + contentDisposition));
    writer.println(logAndReturn("Content-Type: " + contentType));
    writer.println(logAndReturn(""));

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
      for (String line; (line = reader.readLine()) != null; ) {
        writer.println(logAndReturn(line));
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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


    String auth = String.format("auth.cgi?api=SYNO.API.Auth&version=6&method=login&account=%s&passwd=%s&session=FileStation&format=sid",
        properties.getOrDefault("user", "-"),
        properties.getOrDefault("password", "-")

    );
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
//    UploadFormData uploadFormData = new UploadFormData(true, "/downloader-albert", "my-test-file.txt", new MediaType("text", "html"));
//    fileUploadClient.uploadFile(uploadFormData,"");
    fileUploadClient.nativeCall(properties);

  }

}
