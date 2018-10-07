package swehacker.gradle.plugin.plantuml;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.io.File;
import java.util.Set;

public class PlantUmlPlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    project.getTasks().create("generateDomainModel", GenerateDomainModelTask.class, (task) -> {
      //task.setDocumentationDir(project.getProperties().get("docsDir").toString());
      //task.setOutputFile("domain.puml");
        //project.getBuildscript().getConfigurations().stream().forEach(config -> config.forEach(System.out::println));
      //project.getConfigurations().getByName("compileClasspath").forEach(file -> System.out.println(file.toString()));

      //task.setClassPath(project.getConfigurations().getByName("compileClasspath"));
      //task.setClassLoader(project.getClass().getClassLoader());

      //project.getConfigurations().findByName("runtime").getFiles().forEach(file -> System.out.println(file.getAbsolutePath()));
    });
  }
}
