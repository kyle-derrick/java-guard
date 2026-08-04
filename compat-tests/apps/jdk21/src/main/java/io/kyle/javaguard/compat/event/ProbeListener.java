package io.kyle.javaguard.compat.event;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
@Component
public class ProbeListener {
    private final AtomicInteger events = new AtomicInteger();
    @EventListener public void receive(ProbeEvent event) { if (event.value() != null) events.incrementAndGet(); }
    public int events() { return events.get(); }
}
