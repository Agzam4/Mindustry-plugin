package agzam4.database;

import agzam4.database.DBFields.*;
import arc.struct.ObjectSet;

/**
 * TODO: single permissions system for bots and players:
 * 
 * UserEntity{login, permissions}
 * TelegramEntity{telegramId, login}
 * AdminEntity{uuid, usid, ip, ..., login}
 * PlayerEntity{..., login}
 */
public class Users {

//	private static Table<UserEntity> users;
	
	public static void init() {
//		users = new Table<>("users", UserEntity.class);
	}
	
	public static class UserEntity  {

		public @FIELD @PRIMARY_KEY String login;

		private @FIELD String permissions = "";
		
		private ObjectSet<String> permissionsSet = null;

		private void save() {

		}
		
	}
	
	
}
