package agzam4proc.api.lib;

@SuppressWarnings("serial")
public class ApiResponse extends Exception {
	
	public String content;
	public int code = 200;

	public ApiResponse(String string) {
		this.content = string;
	}

	public ApiResponse wrongParms() {
		code = 400;
		return this;
	}
	public ApiResponse serverError() {
		code = 500;
		return this;
	}

	public ApiResponse unauthorized() {
		code = 401;
		return this;
	}

	public ApiResponse forbidden() {
		code = 403;
		return this;
	}
}