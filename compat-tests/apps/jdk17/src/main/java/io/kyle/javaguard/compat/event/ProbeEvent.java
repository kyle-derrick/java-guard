package io.kyle.javaguard.compat.event;
public final class ProbeEvent {
    private final String value;
    public ProbeEvent(String value) { this.value = value; }
    public String value() { return value; }
}
