package swehacker.gradle.plugin.plantuml.structure;

public class Extension implements Relation {
    private static final String RELATION_TYPE_EXTENSION = " -up|> ";
    private final Class<?> from;
    private final String to;

    public Extension(Class<?> from, String to) {
        this.from = from;
        this.to = to;
    }

    public Class<?> getFromType() {
        return from;
    }

    public String getToType() {
        return to;
    }

    public String getMessage() {
        return null;
    }

    public String getFromCardinal() {
        return null;
    }

    @Override
    public String generate() {
        return String.format("%s %s %s", from.getName(), RELATION_TYPE_EXTENSION, to);
    }
}
