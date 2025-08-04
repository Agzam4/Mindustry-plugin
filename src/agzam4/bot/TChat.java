package agzam4.bot;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import agzam4.utils.Log;
import arc.struct.IntMap;
import arc.util.ArcRuntimeException;
import arc.util.Nullable;
import arc.util.Strings;
import arc.util.serialization.JsonValue;
import arc.util.serialization.JsonWriter;

public class TChat extends TSender {
	
	public @Nullable Integer thread = null;
	public IntMap<TChat> threads = new IntMap<TChat>();

	public @Nullable TChat thread(int id) {
		return threads.get(id);
	}

	public void thread(TChat thread) {
		threads.put(thread.thread, thread);
	}
	
	public TChat(long id) {
		super(id);
	}

	public TChat(JsonValue json) {
		super(json);
		if(json.has("thread")) thread = json.getInt("thread");
		if(json.has("threads")) {
			for (var tJson : json.get("threads")) {
				TChat t = new TChat(tJson);
				if(t.thread == null) throw new ArcRuntimeException(Strings.format("Multichat @ contains theard without id", t.id));
				threads.put(t.thread, t);
			}
		}
	}
	
	@Override
	protected void write(JsonWriter writer) throws IOException {
		if(thread != null) writer.set("thread", thread);
		if(threads.size > 0) {
			writer.array("threads");
			for (var t : threads) {
				writer.object();
				t.value.write(writer);
				writer.pop();
			}
			writer.pop();
		}
		super.write(writer);
	}

	@Override
	public void message(String message) {
		if(thread == null) {
			super.message(message);
			return;
		}
		TelegramBot.send(b -> {
			b.chatId(id).text(message).messageThreadId(thread).parseMode("html");
		});
	}
	
	@Override
	public void message(BufferedImage image) {
		if(thread == null) {
			super.message(image);
			return;
		}
		try(ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			ImageIO.write(image, "png", outputStream);
			SendPhoto sendPhoto = new SendPhoto();
			sendPhoto.setChatId(id);
			sendPhoto.setMessageThreadId(thread);
			ByteArrayInputStream stream = new ByteArrayInputStream(outputStream.toByteArray());
			sendPhoto.setPhoto(new InputFile(stream, "tmp-" + System.nanoTime()));
			outputStream.close();
			stream.close();
			TelegramBot.send(sendPhoto);
		} catch (Exception e) {
			Log.err(e);
		}
	}
	
	@Override
	public String toString() {
		if(threads.size > 0) return Strings.format("Mutichat-@ (@ threads)", uid(), threads.size);
		if(thread != null) return Strings.format("Thread-@/@", uid(), Integer.toUnsignedString(thread, Character.MAX_RADIX));
		return Strings.format("Group-@", uid());
	}

	@Override
	public String fuid() {
		if(thread == null) return super.fuid();
		return super.fuid() + "/" + Integer.toUnsignedString(thread, Character.MAX_RADIX);
	}
}
