package com.shanks.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CliArgumentsTest {

    @Test
    void isInputDirectory_trueForDirectory(@TempDir Path dir) {
        CliArguments args = new CliArguments(dir.toString(), "out.avsc");

        assertThat(args.isInputDirectory()).isTrue();
    }

    @Test
    void isInputDirectory_falseForFile(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("api.yaml");
        Files.writeString(file, "openapi: 3.0.3\n");
        CliArguments args = new CliArguments(file.toString(), "out.avsc");

        assertThat(args.isInputDirectory()).isFalse();
    }

    @Test
    void isInputDirectory_falseForNonExistentPath() {
        CliArguments args = new CliArguments("does-not-exist.yaml", "out.avsc");

        assertThat(args.isInputDirectory()).isFalse();
    }
}
