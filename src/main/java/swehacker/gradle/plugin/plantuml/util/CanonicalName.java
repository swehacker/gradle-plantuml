package swehacker.gradle.plugin.plantuml.util;

/**
 * Recover class names for arrays.
 */
public enum CanonicalName {
    Z("boolean", "[Z"),
    B("byte", "[B"),
    C("char", "[C"),
    L("class", "[L"),
    D("double", "[D"),
    F("float", "[F"),
    I("int", "[I"),
    J("long", "[J"),
    S("short", "[S");

    private String className;
    private String code;

    CanonicalName(String className, String code) {
        this.className = className;
        this.code = code;
    }

    public static CanonicalName forCode(String code) {
        for (CanonicalName c : CanonicalName.values()) {
            if (code.startsWith(c.code)) {
                return c;
            }
        }
        throw new IllegalArgumentException("Not an enum constant name: " + code);
    }

    public static String getClassName(String code) {
        CanonicalName cn;
        try {
            cn = forCode(code);
        } catch (IllegalArgumentException e) {
            return code;
        }

        if (cn.equals(CanonicalName.L)) {
            return code.replace(cn.code, "").replace(";", "");
        }

        return cn.className;
    }
}
