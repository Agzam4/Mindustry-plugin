package agzam4proc;

import javax.lang.model.element.Element;

import arc.util.Strings;

@SuppressWarnings("serial")
public class AptError extends RuntimeException {

	public final Element element;
	public final String message;
	
	public AptError(Element element, String message, Object ...args) {
		this.element = element;
		this.message = Strings.format(message, args);
	}


}
