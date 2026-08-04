package io.kyle.javaguard.compat.aop;
import java.util.concurrent.atomic.AtomicInteger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
@Aspect @Component
public class ProbeAspect {
    private final AtomicInteger calls = new AtomicInteger();
    @Around("@annotation(io.kyle.javaguard.compat.aop.CompatProbe)")
    public Object around(ProceedingJoinPoint point) throws Throwable { calls.incrementAndGet(); return point.proceed(); }
    public int calls() { return calls.get(); }
}
