package agzam4;

public class PlayerData {

	public String usid = "<empty>";
	public String connectMessage = null;
	public String disconnectedMessage = null;
	public String name = null;
	
	public static PlayerData from(String uuid) {
		return new PlayerData();
	}
	
	private PlayerData() {}
}
