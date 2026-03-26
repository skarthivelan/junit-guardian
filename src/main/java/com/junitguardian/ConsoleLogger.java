package com.junitguardian;

public class ConsoleLogger {
    private static final String RESET  = "\033[0m";
    private static final String RED    = "\033[0;31m";
    private static final String GREEN  = "\033[0;32m";
    private static final String YELLOW = "\033[0;33m";
    private static final String CYAN   = "\033[0;36m";
    private static final String BOLD   = "\033[1m";

    private final boolean verbose;

    public ConsoleLogger(boolean verbose) { this.verbose = verbose; }

    public void banner(String msg) {
        System.out.println(BOLD + CYAN + "╔══════════════════════════════════════════╗");
        System.out.printf( "║  %-40s║%n", msg);
        System.out.println("╚══════════════════════════════════════════╝" + RESET);
    }

    public void info(String msg)    { System.out.println("  " + msg); }
    public void verbose(String msg) { if (verbose) System.out.println(CYAN + "  [v] " + msg + RESET); }
    public void warn(String msg)    { System.out.println(YELLOW + "  ⚠  " + msg + RESET); }
    public void error(String msg)   { System.err.println(RED + "  ✗  " + msg + RESET); }
    public void success(String msg) { System.out.println(GREEN + "  ✓  " + msg + RESET); }
}
