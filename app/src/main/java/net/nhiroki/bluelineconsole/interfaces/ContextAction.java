package net.nhiroki.bluelineconsole.interfaces;

import net.nhiroki.bluelineconsole.applicationMain.BaseWindowActivity;

public class ContextAction {
    public interface Callback {
        void execute(BaseWindowActivity activity);
    }

    private final String name;
    private final Callback callback;

    public ContextAction(String name, Callback callback) {
        this.name = name;
        this.callback = callback;
    }

    public String getName() { return name; }
    public Callback getCallback() { return callback; }
}
