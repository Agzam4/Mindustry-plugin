package agzam4proc.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import javax.annotation.processing.Processor;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

import agzam4proc.BaseProcessor;
import agzam4proc.api.annotations.Router;

@AutoService(Processor.class)
public class RouterProcessor extends BaseProcessor {

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(Router.class.getCanonicalName());
    }
	
	@Override
	public void onElement(Element element) throws Throwable {}
	
	@Override
	public void onElements(ArrayList<Element> elements) throws Throwable {
		CodeBlock.Builder b = CodeBlock.builder().add("new Class<?>[]{\n");
		boolean first = true;
		for (var element : elements) {
			if(!(element instanceof TypeElement type)) continue;
			if (!first) b.add(",\n");
			b.add("  $T.class", type);
			first = false;
		}

		b.add("\n}");
		TypeName classAny = ParameterizedTypeName.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(Object.class));
		TypeSpec type = TypeSpec.classBuilder("Routers")
			.addModifiers(Modifier.PUBLIC, Modifier.FINAL)
			.addField(FieldSpec.builder(ArrayTypeName.of(classAny), "routers")
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
				.initializer(b.build())
				.build()).build();
		
		write(type);
	}
	
}
