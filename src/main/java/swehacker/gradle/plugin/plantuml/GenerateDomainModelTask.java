package swehacker.gradle.plugin.plantuml;

import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.*;
import org.gradle.api.tasks.Optional;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeElementsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import swehacker.gradle.plugin.plantuml.render.PlantRenderer;
import swehacker.gradle.plugin.plantuml.structure.*;
import swehacker.gradle.plugin.plantuml.util.CanonicalName;
import swehacker.gradle.plugin.plantuml.util.ClassHelper;

import java.lang.reflect.*;
import java.util.*;

class GenerateDomainModelTask extends DefaultTask {

  @Optional
  @Input
  private String docsDir;

  @Optional
  @Input
  private String outputFile;

  @Input
  private String packageStructure;

  @Internal
  private ClassLoader classLoader;

  @InputFiles
  private Configuration classpath;

  void setDocumentationDir(String docsDir) {
    this.docsDir = docsDir;
  }

  void setOutputFile(String outputFile) {
    this.outputFile = outputFile;
  }

  void setClassLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  @TaskAction
  void generateDomain() { //NOSONAR
    System.out.println(parse(packageStructure, classLoader));
    System.out.println(classpath);
    classpath.forEach(System.out::println);
  }

  private static String parse(String packageToParse, ClassLoader classLoader) {
    Set<Relation> relations = new HashSet<>();
    Set<Class<?>> classes = getTypes(packageToParse, classLoader);
    for (Class<?> aClass : classes) {
      addFromTypeRelations(relations, aClass);
    }
    return new PlantRenderer(packageToParse, classes, relations, classLoader).render();
  }

  private static Set<Class<?>> getTypes(String packageToParse, ClassLoader classLoader) {
    Set<Class<?>> classes = new HashSet<>();
    for (String aPackage : ClassHelper.splitPackages(packageToParse)) {
      classes.addAll(getPackageTypes(aPackage, classLoader));
    }
    addSuperClassesAndInterfaces(classes);
    return classes;
  }

  private static void addSuperClassesAndInterfaces(Set<Class<?>> classes) {
    Set<Class<?>> newClasses = new HashSet<>();
    for (Class<?> c : classes) {
      addSuperClass(c, newClasses);
      addInterfaces(c, newClasses);
    }
    classes.addAll(newClasses);
  }

  private static void addInterfaces(Class<?> c, Set<Class<?>> newClasses) {
    Class<?>[] interfaces = c.getInterfaces();
    for (Class<?> i : interfaces) {
      newClasses.add(i);
      addInterfaces(i, newClasses);
    }
  }

  private static void addSuperClass(Class<?> c, Set<Class<?>> newClasses) {
    Class<?> superclass = c.getSuperclass();
    if (null == superclass || Object.class.equals(superclass)) {
      return;
    }
    newClasses.add(superclass);
    addSuperClass(superclass, newClasses);
    addInterfaces(superclass, newClasses);
  }

  /**
   * Get the types from the packages but exclude Object classes.
   */
  private static Collection<? extends Class<?>> getPackageTypes(String packageToParse, ClassLoader classLoader) {
    Set<Class<?>> classes = new HashSet<>();
    Reflections reflections = new Reflections(new ConfigurationBuilder()
        .setScanners(new SubTypesScanner(false), new ResourcesScanner(), new TypeElementsScanner())
        .setUrls(ClasspathHelper.forClassLoader(classLoader))
        .filterInputsBy(new FilterBuilder().include(FilterBuilder.prefix(packageToParse)).exclude("java.*")));

    for (String type : reflections.getStore().get("TypeElementsScanner").keySet()) {
      Class<?> aClass = ClassHelper.loadClass(type, classLoader);
      boolean wantedElement = type.startsWith(packageToParse);
      if (null != aClass && wantedElement) {
        classes.add(aClass);
      }
    }
    return classes;
  }

  /**
   * For the given type, adds to relations:
   * <pre>
   *       Implementations: for Type implemented interfaces
   *       Extensions: for Type extended class
   *       Aggregations: for non private Types declared in Type, taking care of Collection<ActualType> situations
   *       Uses: for dependencies created by non private methods and constructors' parameters.
   * </pre>
   *
   * @param relations A Set to add found relations to.
   * @param fromType The Type originating the relation.
   */
  private static void addFromTypeRelations(Set<Relation> relations, Class<?> fromType) {
    addImplementations(relations, fromType);
    addExtensions(relations, fromType);
    addAggregations(relations, fromType);
    addUses(relations, fromType);
  }

  private static void addImplementations(Set<Relation> relations, Class<?> fromType) {
    Class<?>[] interfaces = fromType.getInterfaces();
    for (Class<?> i : interfaces) {
      Relation anImplements = new Implementation(fromType, i.getName());
      relations.add(anImplements);
    }
  }

  private static void addExtensions(Set<Relation> relations, Class<?> fromType) {
    Class<?> superclass = fromType.getSuperclass();
    if (null == superclass || Object.class.equals(superclass)) {
      return;
    }
    Relation extension = new Extension(fromType, superclass.getName());
    relations.add(extension);
  }

  private static void addAggregations(Set<Relation> relations, Class<?> fromType) {
    Field[] declaredFields = fromType.getDeclaredFields();
    for (Field f : declaredFields) {
      addAggregation(relations, fromType, f);
    }
  }

  private static void addUses(Set<Relation> relations, Class<?> fromType) {
    Method[] methods = fromType.getDeclaredMethods();
    for (Method m : methods) {
      if (!Modifier.isPrivate(m.getModifiers())) {
        addMethodUses(relations, fromType, m);
      }
    }
    Constructor<?>[] constructors = fromType.getDeclaredConstructors();
    for (Constructor<?> c : constructors) {
      if (!Modifier.isPrivate(c.getModifiers())) {
        addConstructorUses(relations, fromType, c);
      }
    }
  }

  private static void addConstructorUses(Set<Relation> relations, Class<?> fromType, Constructor<?> c) {
    Type[] genericParameterTypes = c.getGenericParameterTypes();
    for (Type genericParameterType : genericParameterTypes) {
      addConstructorUse(relations, fromType, genericParameterType, c);
    }
  }

  private static void addMethodUses(Set<Relation> relations, Class<?> fromType, Method m) {
    Type[] genericParameterTypes = m.getGenericParameterTypes();
    for (Type genericParameterType : genericParameterTypes) {
      addMethodUse(relations, fromType, genericParameterType, m);
    }
  }

  private static void addConstructorUse(Set<Relation> relations, Class<?> fromType, Type toType, Constructor<?> c) {
    final String name = ClassHelper.getSimpleName(c.getName()) + "()";
    addUse(relations, fromType, toType, name);
  }

  private static void addUse(Set<Relation> relations, Class<?> fromType, Type toType, String msg) {
    String toName = toType.getClass().getName();
    StringBuilder name = new StringBuilder(msg);
    if (isMulti(toType)) {
      if (!((Class) toType).isArray()) {
        if (toType instanceof ParameterizedType) {
          ParameterizedType pt = (ParameterizedType) toType;
          Set<String> typeVars = getTypeParams(pt);
          for (String t : typeVars) {
            name.append(toName);
            Relation use = new Use(fromType, t, name.toString());
            relations.add(use);
          }
          return;
        }
      }
      toName = CanonicalName.getClassName(((Class) toType).getName());
    }
    Relation use = new Use(fromType, toName, name.toString());
    relations.add(use);
  }

  private static void addMethodUse(Set<Relation> relations, Class<?> fromType, Type fromParameterType, Method m) {
    String name = ClassHelper.getSimpleName(m.getName()) + "()";
    addUse(relations, fromType, fromParameterType, name);
  }

  private static boolean isMulti(Type type) {
    return (type instanceof Class) && (((Class) type).isArray()
        || Collection.class.isAssignableFrom((Class) type)
        || Map.class.isAssignableFrom((Class) type));
  }

  private static void addAggregation(Set<Relation> relations, Class<?> fromType, Field f) {
    Class<?> delegateType = f.getType();
    String varName = f.getName();
    String message = varName + ": " + ClassHelper.getSimpleName(f.getGenericType().toString());
    String toCardinal = "1";
    String toName = delegateType.getName();
    if (isMulti(delegateType)) {
      toCardinal = "*";
      if (!delegateType.isArray()) {
        Set<String> typeVars = getTypeParams(f);
        for (String type : typeVars) {
          Relation aggregation = new Aggregation(fromType, type, toCardinal, message);
          relations.add(aggregation);
        }
        return;
      }
      toName = CanonicalName.getClassName(delegateType.getName());
    }
    Relation aggregation = new Aggregation(fromType, toName, toCardinal, message);
    relations.add(aggregation);
  }

  private static Set<String> getTypeParams(Field f) {
    Type tp = f.getGenericType();
    if (tp instanceof ParameterizedType) {
      return getTypeParams((ParameterizedType) tp);
    }
    return Collections.emptySet();
  }

  private static Set<String> getTypeParams(ParameterizedType f) {
    Set<String> typeVars = new HashSet<>();
    Type[] actualTypeArguments = f.getActualTypeArguments();
    for (Type t : actualTypeArguments) {
      typeVars.add(t.getClass().getName());
    }
    return typeVars;
  }
}
