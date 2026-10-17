package com.shanks.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void parse_readsFlagsInAnyOrderAndFirstPositionalAsSchemaName() {
        CliArguments args = CliArguments.parse(new String[] {
                "api.yaml", "out.avsc", "--doc", "--functional-perimeter", "users", "User",
                "--registry", "--envelope", "minimal" });

        assertThat(args.getSchemaName()).isEqualTo("User");
        assertThat(args.isRegistry()).isTrue();
        assertThat(args.toConversionOptions().includeDoc()).isTrue();
        assertThat(args.toConversionOptions().namespace()).isEqualTo("com.shanks.generated.users");
        assertThat(args.toConversionOptions().envelope()).isEqualTo("minimal");
    }

    @Test
    void parse_readsFromRequestBodyValues() {
        CliArguments args = CliArguments.parse(new String[] {
                "api.yaml", "out.avsc", "--from-request-body", "/users", "POST" });

        assertThat(args.hasRequestBody()).isTrue();
        assertThat(args.getRequestBodyPath()).isEqualTo("/users");
        assertThat(args.getRequestBodyMethod()).isEqualTo("POST");
        assertThat(args.getSchemaName()).isNull();
    }

    @Test
    void parse_rejectsMissingValuesAndUnknownOptions() {
        assertThatThrownBy(() -> CliArguments.parse(new String[] { "a", "b", "--envelope" }))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("--envelope requires a value");
        assertThatThrownBy(() -> CliArguments.parse(new String[] { "a", "b", "--from-request-body", "/users" }))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("--from-request-body requires");
        assertThatThrownBy(() -> CliArguments.parse(new String[] { "a", "b", "--bogus" }))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown option: --bogus");
    }

    @Test
    void getOutputDirectory_treatsNonAvscPathAsDirectory() {
        assertThat(new CliArguments("api.yaml", "out").getOutputDirectory()).isEqualTo("out");
        assertThat(new CliArguments("api.yaml", "out/").getOutputDirectory()).isEqualTo("out");
        assertThat(new CliArguments("api.yaml", "out/nested/").getOutputDirectory()).isEqualTo("out/nested");
    }

    @Test
    void getOutputDirectory_usesParentOfAvscFile() {
        assertThat(new CliArguments("api.yaml", "out/User.avsc").getOutputDirectory()).isEqualTo("out");
        assertThat(new CliArguments("api.yaml", "User.avsc").getOutputDirectory()).isEqualTo(".");
    }
}
