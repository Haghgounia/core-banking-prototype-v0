import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import com.sun.source.util.JavacTask;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Compile-independent Java syntax parser, WITHOUT claiming typechecking or Spring build. */
public final class JavaSyntaxCheck {
    public static void main(String[] args) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager files = compiler.getStandardFileManager(diagnostics, Locale.ROOT, null)) {
            var javaFiles = new ArrayList<java.io.File>();
            for (String path : args) javaFiles.add(Path.of(path).toFile());
            var units = files.getJavaFileObjectsFromFiles(javaFiles);
            JavacTask task = (JavacTask) compiler.getTask(null, files, diagnostics,
                    List.of("--release", "21", "-proc:none"), null, units);
            int parsed=0;
            for (var unit : task.parse()) parsed++;
            int errors=0;
            for (Diagnostic<?> d : diagnostics.getDiagnostics()) {
                if (d.getKind() == Diagnostic.Kind.ERROR) {
                    errors++;
                    System.err.println(d.toString());
                }
            }
            if (errors > 0 || parsed != args.length) throw new IllegalStateException("JAVA_PARSE_FAILED=" + errors);
            System.out.println("CLT_R3_JAVA_SYNTAX_PASS=" + parsed + "/" + parsed);
        }
    }
}
