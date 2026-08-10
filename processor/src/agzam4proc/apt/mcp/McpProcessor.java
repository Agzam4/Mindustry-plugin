package agzam4proc.apt.mcp;

import javax.annotation.processing.Processor;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;

import agzam4proc.BaseProcessor;
import agzam4proc.BaseStep;
import net.ltgt.gradle.incap.IncrementalAnnotationProcessor;
import net.ltgt.gradle.incap.IncrementalAnnotationProcessorType;

@AutoService(Processor.class)
@IncrementalAnnotationProcessor(IncrementalAnnotationProcessorType.AGGREGATING)
public class McpProcessor extends BaseProcessor {

	@Override
	protected ImmutableSet<? extends BaseStep> baseSteps() {
		return ImmutableSet.of(
				new ToolsStep()
				);
	}


}
