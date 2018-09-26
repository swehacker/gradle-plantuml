package swehacker.gradle.plugin.plantuml.structure;

public class Use implements Relation {
    private static final String RELATION_TYPE_USE = " .down.> ";
    private final String msg;
    private final Class<?> from;
    private final String to;

    public Use(Class<?> from, String to, String msg) {
        this.from = from;
        this.to = to;
        this.msg = msg;
    }

    public Class<?> getFromType() {
        return from;
    }

    public String getToType() {
        return to;
    }
    public String getMessage() {
        return msg;
    }

    public String getFromCardinal() {
        return null;
    }

    @Override
    public String generate() {
        String fname = null == getMessage() ? "" : " : " + getMessage();
        return String.format("%s %s %s %s", from.getName(), RELATION_TYPE_USE, to, fname);
    }
}
