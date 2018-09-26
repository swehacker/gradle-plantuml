package swehacker.gradle.plugin.plantuml.structure;

public class Implementation extends Extension {
    private static final String RELATION_TYPE_IMPLEMENTATION = " ..up|> ";

    public Implementation(Class<?> from, String to) {
        super(from, to);
    }

    @Override
    public String generate() {
        return String.format("%s %s %s", getFromType().getName(), RELATION_TYPE_IMPLEMENTATION, getToType());
    }
}
