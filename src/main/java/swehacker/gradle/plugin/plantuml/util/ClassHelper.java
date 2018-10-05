package swehacker.gradle.plugin.plantuml.util;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClassHelper {
    private static final Logger logger = Logger.getLogger("Main");
    private static final String REGEX_FOR_PACKAGE = "((([ice])(nterface|lass|num))? ?([\\w\\[][_\\w\\d]+\\.)+)";

    private ClassHelper() {
        // Util/Helper class
    }

    public static String getSimpleName(String fqcn) {
        return fqcn.replaceAll(REGEX_FOR_PACKAGE, "$3 ");
    }

    public static Class<?> loadClass(String fqnClass, ClassLoader classLoader) {
        try {
            return Class.forName(fqnClass, true, classLoader);
        } catch (ClassNotFoundException | NoClassDefFoundError | ExceptionInInitializerError | UnsatisfiedLinkError e) {
            logger.log(Level.ALL, "Couldn't load the class: " + fqnClass, e);
        }
        return null;
    }

    /**
     * Splits the packages list into a List of individual relevant packages or Classes.
     * Expects a comma separated list of packages/class names.
     *
     * @param packages as a String
     * @return The list of relevant packages or Classes
     */
    public static List<String> splitPackages(String packages) {
        return Arrays.asList(packages.trim().split("\\s*,\\s*"));
    }
}
