package agzam4proc;

import javax.lang.model.element.Element;

import arc.util.Strings;

@SuppressWarnings("serial")
public class AptError extends RuntimeException {

	public final Element element;
	
	public AptError(Element element, String message, Object ...args) {
		super(Strings.format(message, args));
		this.element = element;
	}


}
