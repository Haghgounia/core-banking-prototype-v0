import com.sun.source.util.JavacTask;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** Parse only; does not claim successful dependency resolution or Spring compilation. */
public class JavaSyntaxCheck {
  public static void main(String[] args)throws Exception {
    var compiler=ToolProvider.getSystemJavaCompiler();
    var diagnostics=new DiagnosticCollector<JavaFileObject>();
    try(StandardJavaFileManager files=compiler.getStandardFileManager(diagnostics,null,null)){
      Iterable<? extends JavaFileObject> sources=files.getJavaFileObjectsFromFiles(
          Arrays.stream(args).map(File::new).toList());
      JavacTask task=(JavacTask)compiler.getTask(null,files,diagnostics,
          List.of("--release","21","-proc:none"),null,sources);
      task.parse();
      long errors=diagnostics.getDiagnostics().stream().filter(d->d.getKind()==Diagnostic.Kind.ERROR).count();
      if(errors>0){for(var d:diagnostics.getDiagnostics())if(d.getKind()==Diagnostic.Kind.ERROR)
          System.err.println(d);throw new IllegalStateException("CLT_R4D_JAVA_PARSE_ERRORS="+errors);}
      System.out.println("CLT_R4D_JAVA_SYNTAX_PASS="+args.length);
    }
  }
}
