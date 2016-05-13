package presentation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class PathTemplate {

  private static final String DELIMITER = "/";
  private static final String PARAM = "param";
  private static final String PARAM_PATTERN = "\\{(\\w+)\\}";

  private String template;
  private Pattern paramPattern;

  private List<String> pathElements;
  private Map<String, Integer> params;

  PathTemplate(String template) {
    this.template = template;
    this.paramPattern = Pattern.compile(PARAM_PATTERN);
    this.params = new HashMap<String, Integer>();
    parse();
  }

  private void parse() {
    String[] elements = template.split(DELIMITER);
    pathElements = new ArrayList<String>(elements.length);
    for (int i = 0; i < elements.length; i++) {
      Matcher matcher = paramPattern.matcher(elements[i]);
      if (matcher.matches()) {
        pathElements.add(PARAM);
        String paramName = matcher.group(1);
        params.put(paramName, i);
      } else {
        pathElements.add(elements[i]);
      }
    }
  }

  boolean matches(String path) {
    String[] elements = path.split(DELIMITER);
    if (elements.length != pathElements.size()) {
      return false;
    }
    for (int i = 0; i < elements.length; i++) {
      if (pathElements.get(i).equals(PARAM)) {
        continue;
      }
      if (!pathElements.get(i).equalsIgnoreCase(elements[i])) {
        return false;
      }
    }
    return true;
  }

  String getParameter(String paramName, String path) {
    if (params.get(paramName) == null) {
      return null;
    }
    int position = params.get(paramName);
    String[] pathElements = path.split(DELIMITER);
    return pathElements[position];
  }

  @Override
  public String toString() {
    return template;
  }

}
