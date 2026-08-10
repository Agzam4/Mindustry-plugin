package agzam4proc;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.tools.Diagnostic;

import com.google.auto.common.BasicAnnotationProcessor;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.util.DocTrees;

import arc.struct.Seq;
import arc.util.Log;

public abstract class BaseProcessor extends BasicAnnotationProcessor {
	
	public static final boolean logFiles = false;
	
    public final String packageName = "agzam4gen." + getClass().getPackageName().replaceAll(".apt", "").substring(getClass().getPackageName().indexOf('.')+1);

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.RELEASE_17;
	}
    
    @Override
    protected Iterable<? extends Step> steps() {
    	var list = baseSteps();
    	list.forEach(s -> s.processor = this);
    	return list;
    }

    protected abstract ImmutableSet<? extends BaseStep> baseSteps();
    
	public void info(String str, Object ...args) {
		Log.info(str, args);
	}

	public void warn(Element element, String str, Object ...args) {
        processingEnv.getMessager().printMessage(
            Diagnostic.Kind.MANDATORY_WARNING, 
            format(str, args), 
            element
        );
	}
	
	public void err(Element element, String str, Object ...args) {
		if(element == null) {
	        processingEnv.getMessager().printMessage(
	            Diagnostic.Kind.ERROR, 
	            format(str, args)
	        );
			return;
		}
        processingEnv.getMessager().printMessage(
            Diagnostic.Kind.ERROR, 
            format(str, args), 
            element
        );
	}

	public void err(Element element, Throwable e) {
		try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
			e.printStackTrace(pw);
			String stackTraceString = Seq.with(sw.toString().split("\n")).select(l -> !l.contains("at org.gradle") 
					&& !l.contains("at jdk.compiler") 
					&& !l.contains("at com.squareup")
					&& !l.contains("at java.base")
					&& !l.contains("at com.google")
					).toString("\n");
			err(element, stackTraceString);
	        Log.err(stackTraceString);
		} catch (IOException ioException) {
			ioException.printStackTrace();
	        if(element != null) err(element, "Error error");
		}
	}


	private static String format(String str, Object... args) {
		StringBuilder formated = new StringBuilder(str.length());
		int arg = 0;
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			if(c == '@' && arg < args.length) {
				formated.append(toString(args[arg++]));
				continue;
			}
			formated.append(c);
		}
		for (int i = arg; i < args.length; i++) {
			if(formated.length() != 0) formated.append(' ');
			formated.append(toString(args[i]));
		}
		return formated.toString();
	}
	
	private static String toString(Object object) {
		if(object == null) return "null";
		return object.toString();
	}



    public void write(String pack, TypeSpec builder, Element... originatingElements) {
        try {
            TypeSpec.Builder specBuilder = builder.toBuilder();
            for (Element element : originatingElements) {
                specBuilder.addOriginatingElement(element);
            }
            TypeSpec finalSpec = specBuilder.build();

            JavaFile file = JavaFile.builder(pack == null ? packageName : packageName + "." + pack, finalSpec)
                    .skipJavaLangImports(true)
                    .build();
            file.writeTo(processingEnv.getFiler());
        } catch (Exception e) {
        	throw new RuntimeException(e.getClass() + ": " + e.getMessage());
        }
    }

    public ProcessingEnvironment processingEnv() {
    	return processingEnv;
	}

	public String getDocComment(ExecutableElement method) {
		return processingEnv().getElementUtils().getDocComment(method);
	}
	
}
