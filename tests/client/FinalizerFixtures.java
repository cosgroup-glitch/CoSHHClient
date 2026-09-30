package haven;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

// Test-only collaborators: prevent game/config/cache initialization while
// exercising the production Finalizer source and its real ReferenceQueue.
class Warning {
    static final BlockingQueue<Throwable> issued = new LinkedBlockingQueue<>();
    private final Throwable cause;
    Warning(Throwable cause, String message) { this.cause = cause; }
    void issue() { issued.add(cause); }
    static void warn(String format, Object... args) {
        issued.add(new AssertionError(String.format(format, args)));
    }
}
class Console {
    interface Command { void run(Console cons, String[] args); }
    static void setscmd(String name, Command command) {}
}
class HackThread extends Thread {
    HackThread(Runnable task, String name) { super(task, name); }
    HackThread(ThreadGroup group, Runnable task, String name) { super(group, task, name); }
}
interface Disposable { void dispose(); }
interface Indir<T> { T get(); }
