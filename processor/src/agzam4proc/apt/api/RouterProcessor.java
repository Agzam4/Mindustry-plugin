package agzam4proc.apt.api;

import javax.annotation.processing.Processor;
import javax.annotation.processing.SupportedOptions;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;
import agzam4proc.BaseProcessor;
import agzam4proc.BaseStep;
import agzam4proc.utils.*;
import net.ltgt.gradle.incap.IncrementalAnnotationProcessor;
import net.ltgt.gradle.incap.IncrementalAnnotationProcessorType;

@AutoService(Processor.class)
@IncrementalAnnotationProcessor(IncrementalAnnotationProcessorType.AGGREGATING)
@SupportedOptions("typescriptOutDir")
public class RouterProcessor extends BaseProcessor {
	
    private DependenciesContext context;
    
    @Override
    protected ImmutableSet<? extends BaseStep> baseSteps() {
    	context = new DependenciesContext(packageName, processingEnv.getTypeUtils(), processingEnv);
    	return ImmutableSet.of(
                new DependenciesStep(context),
                new SchemeStep(context.scheme),
                new RouterStep(context)
            );
    }

}
