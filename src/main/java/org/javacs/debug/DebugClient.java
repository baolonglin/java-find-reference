package org.javacs.debug;

public interface DebugClient {
    void initialized();

    void stopped(StoppedEventBody evt);

    void exited(ExitedEventBody evt);

    void output(OutputEventBody evt);

    void breakpoint(BreakpointEventBody evt);

    RunInTerminalResponseBody runInTerminal(RunInTerminalRequest req);
}