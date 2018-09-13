package swehacker.gradle.plugin.plantuml.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Strings {
  private Strings() {
    // Util class
  }

  public static String join(String[] values, String joinCharacters) {
    return Arrays.stream(values).collect(Collectors.joining(joinCharacters));
  }

  public static String join(List<String> values, String joinCharacters) {
    return values.stream().collect(Collectors.joining(joinCharacters));
  }
}
