package com.shanks.cli;

import com.shanks.converter.JsonToAvroConverter;
import com.shanks.converter.OpenApiToAvroConverter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the interactive directory (mass conversion) mode: for each OpenAPI
 * spec file found in the input directory, the CLI lists its schemas, prompts
 * for one to convert plus per-file flags, and converts it.
 */
class ConverterCliMassConvertTest {

    private static final String WIDGET_SPEC = """
            openapi: 3.0.3
            info:
              title: Widget API
              version: 1.0.0
            paths: {}
            components:
              schemas:
                Widget:
                  type: object
                  properties:
                    name:
                      type: string
            """;

    private static final String SECOND_SPEC = """
            openapi: 3.0.3
            info:
              title: Second API
              version: 1.0.0
            paths: {}
            components:
              schemas:
                Foo:
                  type: object
                  properties:
                    value:
                      type: string
            """;

    private static final String WEBHOOK_SPEC = """
            openapi: 3.1.0
            info:
              title: Webhook API
              version: 1.0.0
            webhooks:
              ping:
                post:
                  operationId: onPing
                  requestBody:
                    content:
                      application/json:
                        schema:
                          $ref: '#/components/schemas/PingEvent'
            components:
              schemas:
                PingEvent:
                  type: object
                  properties:
                    id:
                      type: string
            """;

    private static final String BROKEN_SPEC = "invalid yaml content [[[";

    private final ByteArrayOutputStream outCapture = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errCapture = new ByteArrayOutputStream();
    private PrintStream originalOut;
    private PrintStream originalErr;

    @BeforeEach
    void captureStreams() {
        originalOut = System.out;
        originalErr = System.err;
        System.setOut(new PrintStream(outCapture));
        System.setErr(new PrintStream(errCapture));
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    private ConverterCli cliWithInput(String script) {
        return new ConverterCli(new JsonToAvroConverter(), new OpenApiToAvroConverter(),
                new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8)));
    }

    private void writeSpec(Path dir, String fileName, String content) throws IOException {
        Files.writeString(dir.resolve(fileName), content);
    }

    private String outArg(Path outDir) {
        return outDir.toAbsolutePath() + "/";
    }

    @Test
    void numericChoice_appliesDefaultFlags(@TempDir Path in, @TempDir Path out) throws IOException {
        writeSpec(in, "widget.yaml", WIDGET_SPEC);
        ConverterCli cli = cliWithInput("1\n\n\n\n\n");

        int exitCode = cli.run(new String[] { in.toString(), outArg(out) });

        assertThat(exitCode).isZero();
        assertThat(out.resolve("Widget.v1.0.0.avsc")).exists();
    }

    @Test
    void nameChoice_selectsSameSchemaAsNumber(@TempDir Path in, @TempDir Path out) throws IOException {
        writeSpec(in, "widget.yaml", WIDGET_SPEC);
        ConverterCli cli = cliWithInput("Widget\n\n\n\n\n");

        int exitCode = cli.run(new String[] { in.toString(), outArg(out) });

        assertThat(exitCode).isZero();
        assertThat(out.resolve("Widget.v1.0.0.avsc")).exists();
    }

    @Test
    void registryYes_stillProducesValidOutput(@TempDir Path in, @TempDir Path out) throws IOException {
        writeSpec(in, "widget.yaml", WIDGET_SPEC);
        ConverterCli cli = cliWithInput("1\no\nn\n\n\n");

        int exitCode = cli.run(new String[] { in.toString(), outArg(out) });

        assertThat(exitCode).isZero();
        String content = Files.readString(out.resolve("Widget.v1.0.0.avsc"));
        assertThat(content).contains("\"type\" : \"record\"");
    }

    @Test
    void functionalPerimeterValue_appliesNamespace(@TempDir Path in, @TempDir Path out) throws IOException {
        writeSpec(in, "widget.yaml", WIDGET_SPEC);
        ConverterCli cli = cliWithInput("1\nn\nn\nbilling\n\n");

        int exitCode = cli.run(new String[] { in.toString(), outArg(out) });

        assertThat(exitCode).isZero();
        String content = Files.readString(out.resolve("Widget.v1.0.0.avsc"));
        assertThat(content).contains("com.shanks.generated.billing");
    }

    @Test
    void webhookDerivedEntry_isSelectableAndConvertible(@TempDir Path in, @TempDir Path out) throws IOException {
        writeSpec(in, "webhook.yaml", WEBHOOK_SPEC);
        ConverterCli cli = cliWithInput("OnPing\n\n\n\n\n");

        int exitCode = cli.run(new String[] { in.toString(), outArg(out) });

        assertThat(exitCode).isZero();
        assertThat(out.resolve("OnPing.v1.0.0.avsc")).exists();
    }

    @Test
    void skip_doesNotConvertAndDoesNotPromptForFlags(@TempDir Path in, @TempDir Path out) throws IOException {
        writeSpec(in, "widget.yaml", WIDGET_SPEC);
        ConverterCli cli = cliWithInput("s\n");

        int exitCode = cli.run(new String[] { in.toString(), outArg(out) });

        assertThat(exitCode).isZero();
        assertThat(out.resolve("Widget.avsc")).doesNotExist();
    }

    @Test
    void invalidChoice_thenValid_reprompts(@TempDir Path in, @TempDir Path out) throws IOException {
        writeSpec(in, "widget.yaml", WIDGET_SPEC);
        ConverterCli cli = cliWithInput("bogus\n1\n\n\n\n\n");

        int exitCode = cli.run(new String[] { in.toString(), outArg(out) });

        assertThat(exitCode).isZero();
        assertThat(out.resolve("Widget.v1.0.0.avsc")).exists();
    }

    @Test
    void quit_abortsBatchWithoutConvertingAnything(@TempDir Path in, @TempDir Path out) throws IOException {
        writeSpec(in, "a-widget.yaml", WIDGET_SPEC);
        writeSpec(in, "b-second.yaml", SECOND_SPEC);
        ConverterCli cli = cliWithInput("q\n");

        int exitCode = cli.run(new String[] { in.toString(), outArg(out) });

        assertThat(exitCode).isZero();
        assertThat(out.resolve("Widget.avsc")).doesNotExist();
        assertThat(out.resolve("Foo.avsc")).doesNotExist();
    }

    @Test
    void eofMidFlagPrompts_abortsRestOfBatchWithoutCrashing(@TempDir Path in, @TempDir Path out) throws IOException {
        writeSpec(in, "a-widget.yaml", WIDGET_SPEC);
        writeSpec(in, "b-second.yaml", SECOND_SPEC);
        ConverterCli cli = cliWithInput("1\n");

        int exitCode = cli.run(new String[] { in.toString(), outArg(out) });

        assertThat(exitCode).isZero();
        assertThat(out.resolve("Widget.avsc")).doesNotExist();
        assertThat(out.resolve("Foo.avsc")).doesNotExist();
    }

    @Test
    void unparsableSpec_isReportedAsFailureButBatchContinues(@TempDir Path in, @TempDir Path out)
            throws IOException {
        writeSpec(in, "a-broken.yaml", BROKEN_SPEC);
        writeSpec(in, "b-widget.yaml", WIDGET_SPEC);
        ConverterCli cli = cliWithInput("1\n\n\n\n\n");

        int exitCode = cli.run(new String[] { in.toString(), outArg(out) });

        assertThat(exitCode).isEqualTo(1);
        assertThat(out.resolve("Widget.v1.0.0.avsc")).exists();
    }
}
