package agzam4proc;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

public abstract class BaseProcessor extends AbstractProcessor {
	
    public final String packageName = "agzam4gen." + getClass().getPackageName().substring(getClass().getPackageName().indexOf('.')+1);

    public static Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
    	filer = env.getFiler();
    	super.init(env);
    }
    
	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.RELEASE_17;
	}
	
	ArrayList<Element> _elements = null;
	
	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		if (annotations.isEmpty()) {
			return false; 
		}
		ArrayList<Element> elements = new ArrayList<Element>();
		try {
			for (TypeElement annotation : annotations) {
				for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
					try {
						onElement(element);
						elements.add(element);
					} catch (Error e) {
						elements.forEach(s -> err(s, e));
					}
				}
			}
			onElements(elements);
		} catch (Throwable e) {
			elements.forEach(s -> err(s, e));
		}
		return true;
	}

	public void onElement(Element element) throws Throwable {}

	public void onElements(ArrayList<Element> elements) throws Throwable {}
	

	public void warn(Element element, String str, Object ...args) {
		if(element == null) {
			if(_elements != null) _elements.forEach(e -> warn(e, str, args));
			return;
		}
        processingEnv.getMessager().printMessage(
            Diagnostic.Kind.MANDATORY_WARNING, 
            format(str, args), 
            element
        );
	}
	
	public void err(Element element, String str, Object ...args) {
		if(element == null) {
			if(_elements != null) _elements.forEach(e -> err(e, str, args));
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
			String stackTraceString = sw.toString();
	        err(element, stackTraceString);
		} catch (IOException ioException) {
			ioException.printStackTrace();
	        err(element, "Error error");
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



    public void write(TypeSpec builder) {
    	try {
            JavaFile file = JavaFile.builder(packageName, builder).skipJavaLangImports(true).build();
            String writeString;
            writeString = file.toString();

            JavaFileObject object = filer.createSourceFile(file.packageName + "." + file.typeSpec.name, file.typeSpec.originatingElements.toArray(new Element[0]));
            Writer stream = object.openWriter();
            stream.write(writeString);
            stream.close();
            warn(null, writeString);
		} catch (Exception e) {
			err(null, e);
		}
    }
    
}
