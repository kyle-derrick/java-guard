package io.kyle.javaguard.compat.model;
import java.io.Serializable;
public class Payload implements Serializable {
    private static final long serialVersionUID = 1L;
    public String value;
    public Payload() { }
    public Payload(String value) { this.value = value; }
}
