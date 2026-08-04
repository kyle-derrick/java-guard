package io.kyle.javaguard.compat.service;
import io.kyle.javaguard.compat.aop.CompatProbe;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
@Service
public class ProbeService {
    private final AtomicInteger loads = new AtomicInteger();
    @CompatProbe public String advised(String value) { return "advised:" + value; }
    @Cacheable("compat") public String cached(String key) { return key + ":" + loads.incrementAndGet(); }
    @Async public CompletableFuture<String> async(String value) { return CompletableFuture.completedFuture("async:" + value); }
    public int loads() { return loads.get(); }
}
