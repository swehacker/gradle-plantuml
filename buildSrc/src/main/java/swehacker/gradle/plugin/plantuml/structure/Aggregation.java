package swehacker.gradle.plugin.plantuml.structure;

public class Aggregation implements Relation {
    private static final String RELATION_TYPE_AGGREGATION = " o-left- ";
    private final String toFieldName;
    private final Class<?> from;
    private final String to;
    private final String toCardinal;

    public Aggregation(Class<?> from, String to, String toCardinal, String toFieldName) {
        this.from = from;
        this.to = to;
        this.toCardinal = toCardinal;
        this.toFieldName = toFieldName;
    }

    public Class<?> getFromType() {
        return from;
    }

    public String getToType() {
        return to;
    }

    public String getMessage() {
        return toFieldName;
    }

    public String getFromCardinal() {
        return "1";
    }

    @Override
    public String generate() {
        String fname = null == getMessage() ? "" : " : " + getMessage();
        return String.format("%s \"%s\" %s \"%s\" %s %s",
                from.getName(), getFromCardinal(), RELATION_TYPE_AGGREGATION, toCardinal, to, fname
        );
    }
}
