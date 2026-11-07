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
