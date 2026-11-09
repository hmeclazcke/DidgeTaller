package ar.didgetaller.engineering;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.jetbrains.kotlin.config.KotlinCompilerVersion;
import org.jetbrains.kotlin.metadata.ProtoBuf;
import org.jetbrains.kotlin.metadata.deserialization.NameResolver;
import org.jetbrains.kotlin.metadata.deserialization.TypeTable;
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil;
import org.jetbrains.org.objectweb.asm.AnnotationVisitor;
import org.jetbrains.org.objectweb.asm.ClassReader;
import org.jetbrains.org.objectweb.asm.ClassVisitor;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Reads declarations from Kotlin metadata, never guesses mangled names or loads application classes. */
public final class JvmMethodInventory {
    private static final class Header extends ClassVisitor {
        String owner;
        int kind = 1;
        final List<String> data = new ArrayList<>();
        final List<String> strings = new ArrayList<>();
        final Set<String> methods = new HashSet<>();
        Header() { super(Opcodes.ASM9); }
        @Override public void visit(int version, int access, String name, String signature, String parent, String[] interfaces) {
            owner = name;
        }
        @Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            methods.add(name + descriptor);
            return null;
        }
        @Override public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            if (!descriptor.equals("Lkotlin/Metadata;")) return null;
            return new AnnotationVisitor(Opcodes.ASM9) {
                @Override public void visit(String name, Object value) { if (name.equals("k")) kind = (Integer) value; }
                @Override public AnnotationVisitor visitArray(String name) {
                    List<String> values = name.equals("d1") ? data : name.equals("d2") ? strings : null;
                    if (values == null) return null;
                    return new AnnotationVisitor(Opcodes.ASM9) {
                        @Override public void visit(String name, Object value) { values.add((String) value); }
                    };
                }
            };
        }
    }

    public static Document inventory(Path classes) throws Exception {
        if (!Files.isDirectory(classes)) throw new IllegalArgumentException("Compiled Kotlin classes are required");
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element root = document.createElement("jvmMethods"); document.appendChild(root);
        root.setAttribute("schema", "1"); root.setAttribute("reader", KotlinCompilerVersion.VERSION);
        root.setAttribute("classesRoot", classes.toString().replace('\\', '/'));
        List<Path> files;
        try (var stream = Files.walk(classes)) { files = stream.filter(p -> p.toString().endsWith(".class")).sorted().toList(); }
        if (files.isEmpty()) throw new IllegalArgumentException("Empty compiled-class input");
        root.setAttribute("classesScanned", Integer.toString(files.size()));
        for (Path file : files) {
            byte[] bytes = Files.readAllBytes(file);
            Header header = new Header(); new ClassReader(bytes).accept(header, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
            if (header.data.isEmpty()) continue;
            String[] data = header.data.toArray(String[]::new), strings = header.strings.toArray(String[]::new);
            NameResolver names; TypeTable types; List<ProtoBuf.Function> functions;
            if (header.kind == 1) {
                var decoded = JvmProtoBufUtil.readClassDataFrom(data, strings);
                names = decoded.getFirst(); types = new TypeTable(decoded.getSecond().getTypeTable());
                functions = decoded.getSecond().getFunctionList();
            } else if (header.kind == 2 || header.kind == 5) {
                var decoded = JvmProtoBufUtil.readPackageDataFrom(data, strings);
                names = decoded.getFirst(); types = new TypeTable(decoded.getSecond().getTypeTable());
                functions = decoded.getSecond().getFunctionList();
            } else continue;
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            for (var function : functions) {
                var method = JvmProtoBufUtil.INSTANCE.getJvmMethodSignature(function, names, types);
                if (method == null || !header.methods.contains(method.getName() + method.getDesc())) continue;
                Element row = document.createElement("function");
                row.setAttribute("owner", header.owner); row.setAttribute("sourceName", names.getString(function.getName()));
                row.setAttribute("name", method.getName()); row.setAttribute("descriptor", method.getDesc());
                row.setAttribute("classSha256", hash); root.appendChild(row);
            }
        }
        return document;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3 || !args[2].equals("false")) throw new IllegalArgumentException("Non-incremental quality compilation required");
        Document report = inventory(Path.of(args[0])); Path output = Path.of(args[1]);
        report.getDocumentElement().setAttribute("compilation", "non-incremental");
        Files.createDirectories(output.getParent());
        var transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8"); transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(new DOMSource(report), new StreamResult(output.toFile()));
        System.out.println("Kotlin metadata method correspondence written to " + output);
    }
}
