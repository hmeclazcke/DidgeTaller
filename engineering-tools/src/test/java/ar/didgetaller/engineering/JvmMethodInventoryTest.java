package ar.didgetaller.engineering;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.kotlin.cli.common.ExitCode;
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Element;
import static org.junit.Assert.*;

public class JvmMethodInventoryTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void actualCompilerMetadataDistinguishesResultOverloadsValueClassesAndJvmNames() throws Exception {
        Path root = temporary.getRoot().toPath(), output = Files.createDirectory(root.resolve("classes"));
        Path source = root.resolve("Fixture.kt");
        Files.writeString(source, """
            package fixture
            class Reader {
                fun read(value: Int): Result<Int> = Result.success(value)
                fun read(value: String): Result<String> = Result.success(value)
            }
            @JvmInline value class Frequency(val hz: Double)
            fun pitch(value: Frequency): Frequency = value
            @JvmName("alternate") fun named(value: Int): Int = value
            """);
        String stdlib = Path.of(kotlin.Unit.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        var diagnostics = new ByteArrayOutputStream();
        ExitCode code = new K2JVMCompiler().exec(new PrintStream(diagnostics), "-no-stdlib", "-no-reflect",
            "-classpath", stdlib, "-d", output.toString(), source.toString());
        assertEquals(diagnostics.toString(), ExitCode.OK, code);
        var rows = JvmMethodInventory.inventory(output).getElementsByTagName("function");
        Map<String, String> names = new HashMap<>();
        for (int i = 0; i < rows.getLength(); i++) {
            Element row = (Element) rows.item(i);
            assertEquals(64, row.getAttribute("classSha256").length());
            names.put(row.getAttribute("owner") + ":" + row.getAttribute("sourceName") + row.getAttribute("descriptor"), row.getAttribute("name"));
        }
        String integer = names.get("fixture/Reader:read(I)Ljava/lang/Object;");
        String string = names.get("fixture/Reader:read(Ljava/lang/String;)Ljava/lang/Object;");
        assertNotNull(integer); assertNotNull(string); assertNotEquals("read", integer); assertNotEquals("read", string);
        assertEquals("alternate", names.get("fixture/FixtureKt:named(I)I"));
        assertNotNull(names.get("fixture/FixtureKt:pitch(D)D"));
    }

    @Test public void missingAndEmptyBytecodeInputsFail() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> JvmMethodInventory.inventory(temporary.getRoot().toPath().resolve("missing")));
        assertThrows(IllegalArgumentException.class, () -> JvmMethodInventory.inventory(temporary.getRoot().toPath()));
    }

    @Test public void malformedClassesAreNotSilentlySkipped() throws Exception {
        Path root = temporary.getRoot().toPath(); Files.write(root.resolve("Broken.class"), new byte[]{1, 2, 3});
        assertThrows(Exception.class, () -> JvmMethodInventory.inventory(root));
    }
}
