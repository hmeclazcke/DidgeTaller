package ar.didgetaller.engineering;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.HashMap;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiErrorElement;
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.lexer.KotlinLexer;
import org.jetbrains.kotlin.com.intellij.psi.TokenType;
import org.jetbrains.kotlin.psi.*;

/** Compiler PSI supplies declaration boundaries and owners; no source-regex signatures. */
public final class SourceInventory {
    private SourceInventory() { }

    static Element inspect(Document document, KtPsiFactory factory, String path, String source) throws Exception {
        String normalized = source.replace("\r\n", "\n");
        KtFile file = factory.createFile(Path.of(path).getFileName().toString(), normalized);
        PsiErrorElement error = PsiTreeUtil.findChildOfType(file, PsiErrorElement.class);
        if (error != null) {
            throw new IllegalArgumentException("Invalid Kotlin source: " + path + ":" + line(normalized, error.getTextOffset()) + " " + error.getErrorDescription());
        }
        Element result = document.createElement("source");
        result.setAttribute("file", path.replace('\\', '/'));
        result.setAttribute("sha256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(normalized.getBytes(StandardCharsets.UTF_8))));
        result.setAttribute("lines", Integer.toString(normalized.split("\n", -1).length));
        file.accept(new KtTreeVisitorVoid() {
            @Override public void visitNamedFunction(KtNamedFunction function) {
                Element entry = document.createElement("function");
                entry.setAttribute("name", function.getName() == null ? "" : function.getName());
                entry.setAttribute("line", Integer.toString(line(normalized, function.getNameIdentifier() == null
                        ? function.getTextOffset() : function.getNameIdentifier().getTextOffset())));
                entry.setAttribute("start", Integer.toString(line(normalized, function.getTextOffset())));
                entry.setAttribute("end", Integer.toString(line(normalized, function.getTextRange().getEndOffset() - 1)));
                entry.setAttribute("parameters", Integer.toString(function.getValueParameters().size()));
                entry.setAttribute("receiver", Boolean.toString(function.getReceiverTypeReference() != null));
                entry.setAttribute("inline", Boolean.toString(function.hasModifier(KtTokens.INLINE_KEYWORD)));
                entry.setAttribute("suspend", Boolean.toString(function.hasModifier(KtTokens.SUSPEND_KEYWORD)));
                entry.setAttribute("body", Boolean.toString(function.hasBody()));
                entry.setAttribute("compose", Boolean.toString(function.getAnnotationEntries().stream()
                        .anyMatch(a -> a.getShortName() != null && a.getShortName().asString().equals("Composable"))));
                List<String> owners = new ArrayList<>();
                boolean local = false;
                for (PsiElement parent = function.getParent(); parent != file; parent = parent.getParent()) {
                    if (parent instanceof KtNamedFunction || parent instanceof KtFunctionLiteral) local = true;
                    if (parent instanceof KtClassOrObject owner) {
                        owners.add(owner.getName() == null ? "<anonymous>" : owner.getName());
                    }
                }
                Collections.reverse(owners);
                String facade = file.getName().replace(".kt", "Kt");
                entry.setAttribute("owner", file.getPackageFqName().asString().replace('.', '/') + "/"
                        + (owners.isEmpty() ? facade : String.join("$", owners)));
                entry.setAttribute("local", Boolean.toString(local));
                entry.setAttribute("signature", (function.getReceiverTypeReference() == null ? "" : function.getReceiverTypeReference().getText() + ".")
                        + function.getName() + (function.getValueParameterList() == null ? "" : function.getValueParameterList().getText())
                        + (function.getTypeReference() == null ? "" : ": " + function.getTypeReference().getText()));
                describe(entry, function, file, normalized, "function", entry.getAttribute("signature"));
                entry.setAttribute("bodyHash", fingerprint(function.getBodyExpression() == null ? "" : function.getBodyExpression().getText()));
                result.appendChild(entry);
                super.visitNamedFunction(function);
            }
            @Override public void visitLambdaExpression(KtLambdaExpression lambda) {
                Element entry = document.createElement("lambda");
                entry.setAttribute("line", Integer.toString(line(normalized, lambda.getTextOffset())));
                describe(entry, lambda, file, normalized, "lambda", "lambda");
                result.appendChild(entry);
                super.visitLambdaExpression(lambda);
            }
            @Override public void visitPropertyAccessor(KtPropertyAccessor accessor) {
                Element entry = document.createElement("accessor");
                entry.setAttribute("line", Integer.toString(line(normalized, accessor.getTextOffset())));
                entry.setAttribute("body", Boolean.toString(accessor.hasBody()));
                describe(entry, accessor, file, normalized, "accessor", accessor.isGetter() ? "get" : "set");
                result.appendChild(entry);
                super.visitPropertyAccessor(accessor);
            }
            @Override public void visitPrimaryConstructor(KtPrimaryConstructor constructor) {
                add(constructor, "constructor", "primary" + (constructor.getValueParameterList() == null ? "" : constructor.getValueParameterList().getText()));
                super.visitPrimaryConstructor(constructor);
            }
            @Override public void visitSecondaryConstructor(KtSecondaryConstructor constructor) {
                add(constructor, "constructor", "secondary" + constructor.getValueParameterList().getText());
                super.visitSecondaryConstructor(constructor);
            }
            @Override public void visitClassInitializer(KtClassInitializer initializer) {
                add(initializer, "initializer", "init");
                super.visitClassInitializer(initializer);
            }
            @Override public void visitProperty(KtProperty property) {
                if (!property.isLocal() && property.getInitializer() != null) {
                    Element entry = add(property.getInitializer(), "initializer", property.getName());
                    entry.setAttribute("property", property.getName());
                }
                super.visitProperty(property);
            }
            private Element add(PsiElement node, String kind, String label) {
                Element entry = document.createElement(kind);
                describe(entry, node, file, normalized, kind, label);
                result.appendChild(entry);
                return entry;
            }
        });
        var occurrences = new HashMap<String, Integer>();
        for (var node = result.getFirstChild(); node != null; node = node.getNextSibling()) {
            Element entry = (Element) node;
            String identity = entry.getAttribute("identity");
            int occurrence = occurrences.merge(identity, 1, Integer::sum);
            entry.setAttribute("identity", identity + "|occurrence:" + occurrence);
        }
        return result;
    }

    private static void describe(Element entry, PsiElement node, KtFile file, String source, String kind, String label) {
        List<String> scope = new ArrayList<>();
        for (PsiElement parent = node.getParent(); parent != file; parent = parent.getParent()) {
            if (parent instanceof KtClassOrObject owner) scope.add(owner.getName() == null ? "<anonymous>" : owner.getName());
            else if (parent instanceof KtNamedFunction function) scope.add("fun:" + function.getName());
            else if (parent instanceof KtProperty property) scope.add("property:" + property.getName());
            else if (parent instanceof KtLambdaExpression) scope.add("<lambda>");
        }
        Collections.reverse(scope);
        String owner = file.getPackageFqName() + "/" + String.join("/", scope);
        String identity = owner + "|" + kind + "|" + tokens(label);
        entry.setAttribute("kind", kind);
        entry.setAttribute("scope", owner);
        entry.setAttribute("identity", identity);
        entry.setAttribute("semanticHash", fingerprint(node.getText()));
        entry.setAttribute("start", Integer.toString(line(source, node.getTextOffset())));
        entry.setAttribute("end", Integer.toString(line(source, node.getTextRange().getEndOffset() - 1)));
        if (!entry.hasAttribute("line")) entry.setAttribute("line", entry.getAttribute("start"));
        entry.setAttribute("offset", Integer.toString(node.getTextOffset()));
        entry.setAttribute("endOffset", Integer.toString(node.getTextRange().getEndOffset()));
    }

    private static String tokens(String text) {
        var lexer = new KotlinLexer();
        lexer.start(text);
        var result = new StringBuilder();
        while (lexer.getTokenType() != null) {
            var type = lexer.getTokenType();
            if (type == TokenType.WHITE_SPACE || KtTokens.COMMENTS.contains(type)) {
                // Kotlin line breaks can change return/statement semantics. Keep them
                // conservatively even when a wrapping-only edit also triggers review.
                if (text.substring(lexer.getTokenStart(), lexer.getTokenEnd()).contains("\n")) result.append("LINE_BREAK|");
            } else {
                result.append(type).append(':').append(text, lexer.getTokenStart(), lexer.getTokenEnd()).append('|');
            }
            lexer.advance();
        }
        return result.toString();
    }

    private static String fingerprint(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(tokens(text).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }

    private static int line(String source, int offset) {
        return 1 + (int) source.substring(0, offset).chars().filter(c -> c == '\n').count();
    }

    public static void main(String[] arguments) throws Exception {
        var disposable = Disposer.newDisposable();
        try {
            var environment = KotlinCoreEnvironment.createForProduction(disposable,
                    new CompilerConfiguration(), EnvironmentConfigFiles.JVM_CONFIG_FILES);
            var factory = new KtPsiFactory(environment.getProject(), false);
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element root = document.createElement("kotlin-inventory");
            root.setAttribute("parser", "Kotlin compiler PSI 2.2.21");
            root.setAttribute("schema", "2");
            document.appendChild(root);
            try (var paths = Files.walk(Path.of(arguments[0]))) {
                for (Path path : paths.filter(p -> p.toString().endsWith(".kt")).sorted().toList()) {
                    root.appendChild(inspect(document, factory, path.toString(), Files.readString(path)));
                }
            }
            Path output = Path.of(arguments[1]);
            Files.createDirectories(output.getParent());
            TransformerFactory.newInstance().newTransformer().transform(new DOMSource(document), new StreamResult(output.toFile()));
        } finally {
            Disposer.dispose(disposable);
        }
    }
}
