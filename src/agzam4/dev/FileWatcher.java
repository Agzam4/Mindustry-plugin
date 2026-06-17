package agzam4.dev;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import arc.files.Fi;
import arc.func.Cons;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Threads;

import static java.nio.file.StandardWatchEventKinds.*;

public class FileWatcher implements Runnable {
	
	private final Fi fi;
    private final Cons<Fi> listener;
    private final boolean isDirectory;
    private final WatchService service;
    private final Map<WatchKey, Path> keyPathMap = new HashMap<>();

    public FileWatcher(Fi fi, Cons<Fi> listener) throws IOException {
        this.fi = fi;
        this.listener = listener;
        this.isDirectory = fi.isDirectory();
        this.service = FileSystems.getDefault().newWatchService();

        Path path = Paths.get(fi.absolutePath());
        if (isDirectory) {
            registerTree(path);
            return;
        } 
        Path parent = path.getParent();
        if (parent == null) return;
        WatchKey key = parent.register(service, StandardWatchEventKinds.ENTRY_MODIFY);
        keyPathMap.put(key, parent);
    }

    private void registerTree(Path startDir) throws IOException {
        Files.walkFileTree(startDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                WatchKey key = dir.register(service, ENTRY_MODIFY, ENTRY_CREATE, ENTRY_DELETE);
                keyPathMap.put(key, dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                WatchKey key = service.take();
                Path dir = keyPathMap.get(key);
                
                if(dir == null) {
                    key.reset();
                    continue;
                }

                for(WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind == OVERFLOW) continue;

                    Path context = (Path) event.context();
                    Path path = dir.resolve(context);

                    Fi changed = new Fi(path.toFile());

                    if (isDirectory) {
                        if(kind == ENTRY_CREATE && changed.isDirectory()) {
                            try {
                                registerTree(path);
                            } catch (IOException e) {
                            	Log.err(e);
                            }
                        }
                        listener.get(changed);
                        continue;
                    }
                    if(kind != ENTRY_MODIFY || !changed.equals(fi)) continue;
                    listener.get(changed);
                }

                boolean valid = key.reset();
                if (!valid) {
                    keyPathMap.remove(key);
                    if (keyPathMap.isEmpty()) break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            try {
                service.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void watch(Cons<Fi> listener, Fi... files) throws IOException {
        for (Fi fi : files) {
            if(!fi.exists()) throw new FileNotFoundException(fi.path());
            Threads.daemon("FileWatcher-" + fi.name(), new FileWatcher(fi, listener));
        }
    }

    public static void watch(Cons<Fi> listener, Seq<Fi> files) throws IOException {
        for (Fi fi : files) {
            if(!fi.exists()) throw new FileNotFoundException(fi.path());
            Threads.daemon("FileWatcher-" + fi.name(), new FileWatcher(fi, listener));
        }
    }
}
