package presentation.enums;

public enum HttpMethod {
  GET("GET", 1), POST("POST", 2), PUT("PUT", 3), DELETE("DELETE", 4);

  private String name;
  private int weight;

  private HttpMethod(String name, int weight) {
    this.name = name;
    this.weight = weight;
  }

  public int getWeight() {
    return this.weight;
  }

  @Override
  public String toString() {
    return this.name;
  }

}
