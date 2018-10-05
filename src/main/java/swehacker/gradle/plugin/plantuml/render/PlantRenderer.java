package swehacker.gradle.plugin.plantuml.render;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import swehacker.gradle.plugin.plantuml.structure.Relation;
import swehacker.gradle.plugin.plantuml.structure.Use;
import swehacker.gradle.plugin.plantuml.util.ClassHelper;

public class PlantRenderer {

  private static final Logger logger = Logger.getLogger("Main");
  private static final Map<Class<? extends Member>, MemberPrinter> memberPrinters = new HashMap<>();

  static {
    MethodPrinter mp = new MethodPrinter();
    memberPrinters.put(Field.class, new FieldPrinter());
    memberPrinters.put(Constructor.class, mp);
    memberPrinters.put(Method.class, mp);
  }

  private final Set<Class<?>> types;
  private final Set<Relation> relations;
  private final String packageName;
  private final ClassLoader classLoader;

  public PlantRenderer(String packageName, Set<Class<?>> types, Set<Relation> relations, ClassLoader classLoader) {
    this.types = types;
    this.relations = relations;
    this.packageName = packageName;
    this.classLoader = classLoader;
  }

  public String render() {
    return "@startuml\n" +
        "left to right direction\n" +
        "' Participants \n\n" +
        addClasses() +
        "\n" +
        "' Relations \n\n" +
        addRelations() +
        "@enduml\n";
  }


  private String addRelations() {
    StringBuilder sb = new StringBuilder();
    ArrayList<Relation> relationsList = new ArrayList<>(this.relations);
    sortRelations(relationsList);
    for (Relation r : relationsList) {
      addRelation(sb, r);
    }

    return sb.toString();
  }

  private void sortRelations(ArrayList<Relation> relations) {
    relations.sort((o1, o2) -> o1.getClass().equals(o2.getClass()) ? o1.getFromType().getName()
        .compareTo(o1.getFromType().getName()) : o1.getClass().getName().compareTo(o2.getClass().getName()));
  }

  private void addRelation(StringBuilder sb, Relation relation) {
    if (relation instanceof Use && isToTypeInAggregations(relation)) {
      return;
    }

    if (!relation.getToType().startsWith(packageName)) {
      return;
    }
    sb.append(relation.generate()).append("\n");
  }

  private boolean isToTypeInAggregations(Relation relation) {
    Class<?> toType = ClassHelper.loadClass(relation.getToType(), classLoader);
    Class<?> origin = relation.getFromType();
    for (Field f : origin.getDeclaredFields()) {
      if (f.getType().equals(toType)) {
        return true;
      }
    }
    return false;
  }

  private String addClasses() {
    StringBuilder sb = new StringBuilder();
    for (Class<?> c : types) {
      addClass(sb, c);
    }

    return sb.toString();
  }

  private void addClass(StringBuilder sb, Class<?> aClass) {
    if (aClass.getCanonicalName() != null && !aClass.getCanonicalName().contains(packageName)) {
      return;
    }
    String classDeclaration = aClass.isEnum() ? "enum " + aClass.getName() : aClass.toString();
    sb.append(classDeclaration);
    addClassTypeParams(sb, aClass);
    sb.append(" {\n");
    renderClassMembers(sb, aClass);
    sb.append("\n}\n");
  }

  private void addClassTypeParams(StringBuilder sb, Class<?> aClass) {
    List<String> typeParams = new ArrayList<>();
    for (TypeVariable t : aClass.getTypeParameters()) {
      Type[] bounds = t.getBounds();
      String jointBounds = ClassHelper.getSimpleName(
          String.join("&", Arrays.stream(bounds).map(bound -> bound.getTypeName()).collect(Collectors.toList())));
      typeParams.add(t.getName() + " extends " + jointBounds);
    }

    if (typeParams.isEmpty()) {
      sb.append(" <").append(String.join(", ", typeParams)).append(">");
    }
  }

  private void renderClassMembers(StringBuilder sb, Class<?> aClass) {
    List<String> fields = new ArrayList<>();
    List<String> methods = new ArrayList<>();
    List<String> constructors = new ArrayList<>();

    addMembers(aClass.getDeclaredFields(), fields);
    addMembers(aClass.getDeclaredConstructors(), constructors);
    addMembers(aClass.getDeclaredMethods(), methods);

    Collections.sort(fields);
    Collections.sort(methods);
    Collections.sort(constructors);

    for (String field : fields) {
      sb.append(field);
      sb.append("\n");
    }
    sb.append("--\n");
    for (String constructor : constructors) {
      sb.append(constructor);
      sb.append("\n");
    }
    for (String method : methods) {
      sb.append(method);
      sb.append("\n");
    }
  }

  private void addMembers(Member[] declaredMembers, List<String> plantMembers) {
    for (Member m : declaredMembers) {
      memberPrinters.get(m.getClass()).addMember(m, plantMembers);
    }
  }

  private enum Modifiers {
    PUBLIC("+"),
    PROTECTED("#"),
    PRIVATE("-"),
    DEFAULT("~");

    private String prefix;

    Modifiers(String prefix) {
      this.prefix = prefix;
    }

    public static Modifiers forModifier(int memberModifier) {
      if (Modifier.isPrivate(memberModifier)) {
        return PRIVATE;
      } else if (Modifier.isProtected(memberModifier)) {
        return PROTECTED;
      } else if (Modifier.isPublic(memberModifier)) {
        return PUBLIC;
      } else {
        return DEFAULT;
      }
    }

    @Override
    public String toString() {
      return prefix + " ";
    }
  }

  interface MemberPrinter {

    void addMember(Member m, List<String> plantMembers);
  }

  static class FieldPrinter implements MemberPrinter {

    @Override
    public void addMember(Member m, List<String> plantMembers) {
      Field f = (Field) m;
      if (f.isSynthetic()) {
        return;
      }

      String msg = String.format("%s %s : %s",
          Modifiers.forModifier(f.getModifiers()),
          f.getName(),
          ClassHelper.getSimpleName(f.getGenericType().toString()));
      plantMembers.add(msg);
    }
  }

  static class MethodPrinter implements MemberPrinter {

    @Override
    public void addMember(Member m, List<String> plantMembers) {
      if (m.isSynthetic()) {
        logger.log(Level.FINE, "Skipping synthetic member: " + m.getName());
        return;
      }

      String name = ClassHelper.getSimpleName(m.getName());
      String modifiers = Modifiers.forModifier(m.getModifiers()).toString();
      String returnType =
          (m instanceof Method) ? " : " + ClassHelper.getSimpleName(((Method) m).getReturnType().getName()) : "";
      String params = buildParams(m);
      plantMembers.add(String.format("%s %s(%s) %s", modifiers, name, params, returnType));
    }

    private String buildParams(Member m) {
      StringBuilder params = new StringBuilder();
      Type[] paramClasses = m instanceof Method ? ((Method) m).getGenericParameterTypes()
          : ((Constructor<?>) m).getGenericParameterTypes();
      for (Type type : paramClasses) {
        params.append(ClassHelper.getSimpleName(type.toString()))
            .append(", ");
      }

      if (params.length() == 0) {
        return "";
      }
      return params.toString().substring(0, params.length() - 2);
    }
  }
}
