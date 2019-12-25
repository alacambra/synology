package tech.lacambra.synology;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;

public class SynoAPIResource {

  //      "entry.cgi?api=SYNO.FileStation.Upload&method=upload&version=2;
  private String api;
  private String method;
  private int version;

  public SynoAPIResource(String api, String method, int version) {
    this.api = api;
    this.method = method;
    this.version = version;
  }

  public String getApi() {
    return api;
  }

  public String getMethod() {
    return method;
  }

  public int getVersion() {
    return version;
  }

  public WebTarget appendToWebTarget(WebTarget webTarget) {
    return webTarget.queryParam("api", getApi()).queryParam("method", getMethod()).queryParam("version", getVersion());
  }

  public Form appendToForm(Form form) {
    return form.param("api", getApi()).param("method", getMethod()).param("version", String.valueOf(getVersion()));
  }

  public static SynoAPIResource UploadFile = new SynoAPIResource("SYNO.FileStation.Upload", "upload", 2);
  public static SynoAPIResource UploadFileCheckWritePermissions = new SynoAPIResource("SYNO.FileStation.CheckPermission", "write", 3);


}
