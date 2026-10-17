package com.shanks.cli;

import com.shanks.converter.JsonToAvroConverter;
import com.shanks.converter.OpenApiToAvroConverter;
import org.apache.avro.Schema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for JSON data → Avro conversion through the CLI, covering the
 * --functional-perimeter and --registry flags.
 */
class ConverterCliJsonConvertTest {

    private static final String DATA_JSON = """
            {
              "orderId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
              "customer": {
                "name": "Jane",
                "address": { "city": "Paris" }
              },
              "channels": ["WEB", "MOBILE_APP"]
            }
            """;

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

    private ConverterCli cli() {
        return new ConverterCli(new JsonToAvroConverter(), new OpenApiToAvroConverter());
    }

    private Path writeData(Path dir) throws IOException {
        Path input = dir.resolve("data.json");
        Files.writeString(input, DATA_JSON);
        return input;
    }

    private Schema readSchema(Path avsc) throws IOException {
        return new Schema.Parser().parse(Files.readString(avsc));
    }

    @Test
    void noFlags_usesDefaultNamespace(@TempDir Path dir) throws IOException {
        Path input = writeData(dir);
        Path output = dir.resolve("Data.avsc");

        int exitCode = cli().run(new String[] { input.toString(), output.toString() });

        assertThat(exitCode).isZero();
        Schema schema = readSchema(output);
        assertThat(schema.getNamespace()).isEqualTo("com.shanks.generated");
    }

    @Test
    void functionalPerimeter_appliesToRootAndNestedNamespaces(@TempDir Path dir) throws IOException {
        Path input = writeData(dir);
        Path output = dir.resolve("Data.avsc");

        int exitCode = cli().run(new String[] {
                input.toString(), output.toString(), "--functional-perimeter", "orders" });

        assertThat(exitCode).isZero();
        Schema schema = readSchema(output);
        assertThat(schema.getNamespace()).isEqualTo("com.shanks.generated.orders");
        Schema customer = schema.getField("customer").schema();
        assertThat(customer.getFullName()).isEqualTo("com.shanks.generated.orders.root.Customer");
        assertThat(customer.getField("address").schema().getFullName())
                .isEqualTo("com.shanks.generated.orders.root.customer.Address");
        assertThat(Files.readString(dir.resolve("Data.webhook.avsc")))
                .contains("com.shanks.generated.orders");
    }

    @Test
    void registry_producesSingleSelfContainedRecord(@TempDir Path dir) throws IOException {
        Path input = writeData(dir);
        Path output = dir.resolve("Data.avsc");

        int exitCode = cli().run(new String[] { input.toString(), output.toString(), "--registry" });

        assertThat(exitCode).isZero();
        assertThat(outCapture.toString()).contains("Mode:   Registry");
        String content = Files.readString(output).trim();
        assertThat(content).startsWith("{");
        Schema schema = readSchema(output);
        assertThat(schema.getType()).isEqualTo(Schema.Type.RECORD);
        assertThat(schema.getNamespace()).isEqualTo("com.shanks.generated");
    }

    @Test
    void registryAndFunctionalPerimeter_combineInAnyOrder(@TempDir Path dir) throws IOException {
        Path input = writeData(dir);
        Path output = dir.resolve("Data.avsc");

        int exitCode = cli().run(new String[] {
                input.toString(), output.toString(), "--functional-perimeter", "orders", "--registry" });

        assertThat(exitCode).isZero();
        assertThat(outCapture.toString()).contains("Mode:   Registry");
        Schema schema = readSchema(output);
        assertThat(schema.getNamespace()).isEqualTo("com.shanks.generated.orders");
        assertThat(schema.getField("customer").schema().getFullName())
                .isEqualTo("com.shanks.generated.orders.root.Customer");
    }

    @Test
    void unknownEnvelope_failsWithClearMessageBeforeWritingAnything(@TempDir Path dir) throws IOException {
        Path input = writeData(dir);
        Path output = dir.resolve("out").resolve("Data.avsc");

        int exitCode = cli().run(new String[] { input.toString(), output.toString(), "--envelope", "nope" });

        assertThat(exitCode).isEqualTo(1);
        assertThat(errCapture.toString())
                .contains("Error: Envelope 'nope' not found")
                .doesNotContain("Error during operation");
        assertThat(dir.resolve("out")).doesNotExist();
    }

    @Test
    void unknownOption_isRejected(@TempDir Path dir) throws IOException {
        Path input = writeData(dir);

        int exitCode = cli().run(new String[] { input.toString(), dir.resolve("Data.avsc").toString(), "--regsitry" });

        assertThat(exitCode).isEqualTo(1);
        assertThat(errCapture.toString()).contains("Unknown option: --regsitry");
    }
}
