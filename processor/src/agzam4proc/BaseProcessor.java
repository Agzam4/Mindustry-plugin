package agzam4proc;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import com.sun.source.util.Trees;

import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;

public abstract class BaseProcessor extends AbstractProcessor {
	
	public static final boolean logFiles = false;
	
    public final String packageName = "agzam4gen." + getClass().getPackageName().substring(getClass().getPackageName().indexOf('.')+1);

    public static Filer filer;
    protected int round;
    protected int rounds = 1;
    protected Trees trees;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
    	filer = env.getFiler();
    	round = 0;
    	this.trees = Trees.instance(env);
    	super.init(env);
    }
    
	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.RELEASE_17;
	}

	public abstract Seq<Class<?>> classes();

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(classes().map(c -> c.getCanonicalName()).toArray(String.class));
    }
    
    public ProcessingEnvironment processingEnv() {
    	return processingEnv;
	}

	Seq<Element> elements = new Seq<Element>();
	ObjectMap<Class<?>, Seq<Element>> map = new ObjectMap<>();
	Seq<Element> _elements = null;

	public Types typeUtils;
	
	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        if(round++ >= rounds) {
        	Log.info("@ round skipped", round);
        	return false; //only process 1 round
        }
        this.typeUtils = processingEnv.getTypeUtils();
		Log.info("&lb[@] Round: @/@ (@)", this, round, rounds, getClass());
		try {
			if (annotations.isEmpty()) {
				onElement(map);
				onElements(elements);
				return true; 
			}
			classes().forEach(c -> {
				if(map.containsKey(c)) return;
				map.put(c, new Seq<>());
			});

			for (TypeElement annotation : annotations) {
				for (Element element : env.getElementsAnnotatedWith(annotation)) {
					elements.add(element);
					var clz = classes().find(c -> Proc.equals(annotation, c));
					map.get(clz).add(element);

					try {
						onElement(element);
					} catch (AptError e) {
						err(e.element, e.message);
						err(element, "@: @", e.element.getSimpleName(), e.message);
					} catch (Throwable e) {
						elements.forEach(s -> err(s, e));
					}
				}
			}
			onElement(map);
			onElements(elements);
		} catch (AptError e) {
			err(e.element, e.message);
		} catch (Throwable e) {
			err(null, e);
//			elements.forEach(s -> );
		}
		return true;
	}
	
	public void onElement(ObjectMap<Class<?>, Seq<Element>> map) throws Throwable {}

	public void onElement(Element element) throws Throwable {}

	public void onElements(Seq<Element> elements) throws Throwable {}

	public void info(String str, Object ...args) {
		Log.info(str, args);
	}

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
			String stackTraceString = Seq.with(sw.toString().split("\n")).select(l -> !l.contains("at org.gradle") 
					&& !l.contains("at jdk.compiler") 
					&& !l.contains("at com.squareup")
					&& !l.contains("at java.base")
					).toString("\n");
	         if(element != null) err(element, stackTraceString);
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


    public void write(String pack, TypeSpec builder) {
    	try {
            JavaFile file = JavaFile.builder(packageName + "." + pack, builder).skipJavaLangImports(true).build();
            String writeString;
            writeString = file.toString();

            JavaFileObject object = filer.createSourceFile(file.packageName + "." + file.typeSpec.name, file.typeSpec.originatingElements.toArray(new Element[0]));
            if(logFiles) Log.info("&g+ @", object.getName());
            Writer stream = object.openWriter();
            stream.write(writeString);
            stream.close();
            warn(null, writeString);
		} catch (Exception e) {
			err(null, e);
		}
    }

    public void write(TypeSpec builder) {
    	try {
            JavaFile file = JavaFile.builder(packageName, builder).skipJavaLangImports(true).build();
            String writeString;
            writeString = file.toString();

            JavaFileObject object = filer.createSourceFile(file.packageName + "." + file.typeSpec.name, file.typeSpec.originatingElements.toArray(new Element[0]));
//            Log.info("+ @", object.getName());
            Writer stream = object.openWriter();
            stream.write(writeString);
            stream.close();
            warn(null, writeString);
		} catch (Exception e) {
			err(null, e);
		}
    }
    
}
