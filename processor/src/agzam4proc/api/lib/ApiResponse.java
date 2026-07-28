package agzam4proc.api.lib;

@SuppressWarnings("serial")
public class ApiResponse extends Exception {
	
	public static final ApiResponse forbidden = new ApiResponse("Forbidden", 403);
	
	public String content;
	public int code = 200;

	public ApiResponse(String string) {
		this.content = string;
	}
	
	public ApiResponse(String string, int code) {
		this.content = string;
		this.code = code;
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

	@Deprecated
	public ApiResponse forbidden() {
		code = 403;
		return this;
	}
}