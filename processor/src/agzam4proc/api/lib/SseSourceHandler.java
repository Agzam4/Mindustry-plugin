package agzam4proc.api.lib;

import java.io.IOException;

public interface SseSourceHandler<T> {

	public String get(T t) throws ApiResponse, IOException;
	
}
