package swehacker.gradle.plugin.plantuml;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.io.File;
import java.util.Set;

public class PlantUmlPlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    project.getTasks().create("generateDomainModel", GenerateDomainModelTask.class, (task) -> { });
  }
}
