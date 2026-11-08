package ar.didgetaller.engineering;

import javax.xml.parsers.DocumentBuilderFactory;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer;
import org.jetbrains.kotlin.psi.KtPsiFactory;
import org.junit.Test;
import static org.junit.Assert.*;

public class SourceInventoryTest {
    @Test public void fingerprintsAuthoredCallablesWithoutTreatingFormattingAsNewLogic() throws Exception {
        var disposable = Disposer.newDisposable();
        try {
            var environment = KotlinCoreEnvironment.createForProduction(disposable,
                    new CompilerConfiguration(), EnvironmentConfigFiles.JVM_CONFIG_FILES);
            var factory = new KtPsiFactory(environment.getProject(), false);
            var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            var first = SourceInventory.inspect(document, factory, "test/Example.kt",
                    "package test\nclass Example(val count: Int = 1) { val next get() = count + 1; val task = { count + 2 }; fun read() = count + 1 }");
            var formatted = SourceInventory.inspect(document, factory, "test/Example.kt",
                    "package test\nclass Example(val count: Int = 1) { val next get() = count + 1; val task = { count + 2 }; fun read() = /* reason */ count   + 1 }");
            var changed = SourceInventory.inspect(document, factory, "test/Example.kt",
                    "package test\nclass Example(val count: Int = 1) { val next get() = count + 1; val task = { count + 2 }; fun read() = count + 3 }");
            var a = (org.w3c.dom.Element) first.getElementsByTagName("function").item(0);
            var b = (org.w3c.dom.Element) formatted.getElementsByTagName("function").item(0);
            var c = (org.w3c.dom.Element) changed.getElementsByTagName("function").item(0);
            assertFalse(a.getAttribute("semanticHash").isEmpty());
            assertEquals(a.getAttribute("semanticHash"), b.getAttribute("semanticHash"));
            assertNotEquals(a.getAttribute("semanticHash"), c.getAttribute("semanticHash"));
            var lineBreak = SourceInventory.inspect(document, factory, "test/Example.kt",
                    "package test\nfun read() { return\n 1 }");
            var sameLine = SourceInventory.inspect(document, factory, "test/Example.kt",
                    "package test\nfun read() { return 1 }");
            assertNotEquals(((org.w3c.dom.Element) lineBreak.getElementsByTagName("function").item(0)).getAttribute("semanticHash"),
                    ((org.w3c.dom.Element) sameLine.getElementsByTagName("function").item(0)).getAttribute("semanticHash"));
            for (String tag : new String[]{"lambda", "accessor", "constructor", "initializer"}) {
                var entry = (org.w3c.dom.Element) first.getElementsByTagName(tag).item(0);
                assertNotNull(tag, entry);
                assertFalse(tag, entry.getAttribute("semanticHash").isEmpty());
                assertFalse(tag, entry.getAttribute("identity").isEmpty());
            }
        } finally { Disposer.dispose(disposable); }
    }
    @Test public void preservesNestedOwnersOverloadsAndLocalFunctions() throws Exception {
        var disposable = Disposer.newDisposable();
        try {
            var environment = KotlinCoreEnvironment.createForProduction(disposable,
                    new CompilerConfiguration(), EnvironmentConfigFiles.JVM_CONFIG_FILES);
            var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            var result = SourceInventory.inspect(document, new KtPsiFactory(environment.getProject(), false), "test/Example.kt",
                    "package test\nobject Example {\nfun read(x: Int) = x\nfun read(x: String) = x\n"
                    + "class Nested { fun run() {\nfun local() = 1\nlocal()\n} }\n}\ninline fun String.ext() = length");
            var functions = result.getElementsByTagName("function");
            assertEquals(5, functions.getLength());
            assertEquals("test/Example", ((org.w3c.dom.Element) functions.item(0)).getAttribute("owner"));
            assertEquals("test/Example$Nested", ((org.w3c.dom.Element) functions.item(2)).getAttribute("owner"));
            assertEquals("true", ((org.w3c.dom.Element) functions.item(3)).getAttribute("local"));
            assertEquals("true", ((org.w3c.dom.Element) functions.item(4)).getAttribute("inline"));
        } finally { Disposer.dispose(disposable); }
    }
}
