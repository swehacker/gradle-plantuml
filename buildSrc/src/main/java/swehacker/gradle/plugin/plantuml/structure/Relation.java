package swehacker.gradle.plugin.plantuml.structure;

public interface Relation {
    Class<?> getFromType();

    String getToType();

    String getMessage();

    String getFromCardinal();

    /**
     * Prints a plant UML relation line, like:
     * <code>
     * callerClass --> SomeOtherClass : message()
     * subClass -|> ParentClass
     * usingClass ..> UsedClass
     * </code>
     *
     * @return String
     */
    String generate();
}
