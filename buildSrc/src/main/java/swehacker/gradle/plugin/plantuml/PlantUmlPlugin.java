package swehacker.gradle.plugin.plantuml;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class PlantUmlPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getTasks().create("domain", GenerateDomainModelTask.class, (task) -> {
            task.setMessage("Hello");
            task.setRecipient("World");
        });
    }
}
