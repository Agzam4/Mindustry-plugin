package agzam4proc.api.proto;

import java.io.IOException;
import java.nio.file.*;

import javax.annotation.processing.ProcessingEnvironment;

import agzam4proc.api.utils.DependenciesContext;
import agzam4proc.api.utils.Scheme;
import arc.struct.Seq;

public abstract class Generator {

	protected final Scheme scheme;
	protected final DependenciesContext context;
	protected final Seq<EndpointInfo> endpoints;
	protected final String outDir;

	public Generator(DependenciesContext context, Seq<EndpointInfo> endpoints, ProcessingEnvironment processingEnv) {
		this.scheme = context.scheme;
		this.context = context;
		this.endpoints = endpoints;
		String lang = languageName();
		this.outDir = processingEnv.getOptions().get(lang + "OutDir");
	}

	public abstract String languageName();
	public abstract String fileExtension();
	public abstract String generate();

	public void write() throws IOException {
		if(outDir == null) return;
		var path = Path.of(outDir, "api." + fileExtension());
		Files.createDirectories(path.getParent());
		Files.writeString(path, generate());
	}

}
