package com.leak.intelligentcustomerchat.app.workflow;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DemoScenarioAssetParityTest {

    @Test
    void shouldKeepOpsAndClasspathScenarioAssetsInSync() throws IOException {
        Path opsScenarioDir = Path.of("ops/demo-scenarios");
        Path classpathScenarioDir = Path.of("src/main/resources/demo-scenarios");

        List<String> opsFiles = listScenarioFiles(opsScenarioDir);
        List<String> classpathFiles = listScenarioFiles(classpathScenarioDir);

        assertThat(opsFiles).containsExactlyElementsOf(classpathFiles);
        for (String fileName : opsFiles) {
            String opsContent = Files.readString(opsScenarioDir.resolve(fileName), StandardCharsets.UTF_8);
            String classpathContent = Files.readString(classpathScenarioDir.resolve(fileName), StandardCharsets.UTF_8);
            assertThat(opsContent).as(fileName).isEqualTo(classpathContent);
        }
    }

    private List<String> listScenarioFiles(Path dir) throws IOException {
        try (var stream = Files.list(dir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .sorted(Comparator.naturalOrder())
                    .toList();
        }
    }
}
