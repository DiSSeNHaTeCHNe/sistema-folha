package br.com.techne.sistemafolha.shared.logging;

/**
 * Prefixo estruturado {@code domain=<nome>} para logs de application services (MOD-21).
 */
public final class DomainLogging {

    private DomainLogging() {
    }

    public static String prefix(String domain) {
        return "domain=" + domain + " ";
    }

    public static String msg(String domain, String message) {
        return prefix(domain) + message;
    }
}
