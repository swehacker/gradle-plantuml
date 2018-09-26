package swehacker.gradle.plugin.plantuml;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class PlantUmlPlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    project.getTasks().create("domain", GenerateDomainModelTask.class, (task) -> {
      task.setDocumentationDir(project.getProperties().get("docsDir").toString());
      task.setOutputFile("domain.puml");

      System.out.println("Project: " + project.getConfigurations().getByName("compileClasspath").resolve());


      project.getProperties().forEach((key, value) -> System.out.println(key + ": " + value));

      //Set<File> ccp = project.getConfigurations().getByName("compileClasspath").getFiles();
      //task.classpath = ccp;
      task.setClassLoader(project.getClass().getClassLoader());

      project.getConfigurations().findByName("runtime").getFiles().forEach(file -> System.out.println(file.getAbsolutePath()));
    });
  }
}
