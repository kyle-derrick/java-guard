package io.kyle.javaguard.compat.check;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kyle.javaguard.compat.CompatApplication;
import io.kyle.javaguard.compat.aop.ProbeAspect;
import io.kyle.javaguard.compat.config.CompatProperties;
import io.kyle.javaguard.compat.event.ProbeEvent;
import io.kyle.javaguard.compat.event.ProbeListener;
import io.kyle.javaguard.compat.language.LanguageFeatures;
import io.kyle.javaguard.compat.model.Payload;
import io.kyle.javaguard.compat.proxy.SerializableHandler;
import io.kyle.javaguard.compat.service.ProbeService;
import io.kyle.javaguard.compat.spi.GreetingProvider;
import io.kyle.javaguard.compat.spi.impl.DefaultGreetingProvider;
import java.io.*;
import java.lang.reflect.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
@Component
public class CompatChecks {
    private final CompatProperties properties;
    private final ProbeService service;
    private final ProbeAspect aspect;
    private final ProbeListener listener;
    private final ApplicationEventPublisher publisher;
    private final ObjectMapper mapper;
    public CompatChecks(CompatProperties properties, ProbeService service, ProbeAspect aspect,
            ProbeListener listener, ApplicationEventPublisher publisher, ObjectMapper mapper) {
        this.properties = properties; this.service = service; this.aspect = aspect;
        this.listener = listener; this.publisher = publisher; this.mapper = mapper;
    }
    public Map<String, Boolean> run() throws Exception {
        Map<String, Boolean> checks = new LinkedHashMap<String, Boolean>();
        checks.put("diConfig", "hello-compat".equals(properties.getGreeting()));
        int before = aspect.calls();
        checks.put("aop", "advised:x".equals(service.advised("x")) && aspect.calls() == before + 1);
        String key = "k" + System.nanoTime(); before = service.loads(); String cached = service.cached(key);
        checks.put("cache", cached.equals(service.cached(key)) && service.loads() == before + 1);
        checks.put("async", "async:x".equals(service.async("x").get()));
        before = listener.events(); publisher.publishEvent(new ProbeEvent("x"));
        checks.put("event", listener.events() == before + 1);
        Payload decoded = mapper.readValue(mapper.writeValueAsBytes(new Payload("json")), Payload.class);
        checks.put("json", "json".equals(decoded.value));
        GreetingProvider provider = ServiceLoader.load(GreetingProvider.class).iterator().next();
        checks.put("serviceLoader", "service-loader".equals(provider.greeting()));
        Method method = DefaultGreetingProvider.class.getMethod("greeting");
        checks.put("reflection", "service-loader".equals(method.invoke(new DefaultGreetingProvider())));
        GreetingProvider proxy = (GreetingProvider) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{GreetingProvider.class, Serializable.class}, new SerializableHandler());
        checks.put("proxy", "proxied".equals(proxy.greeting()));
        checks.put("serialization", roundTrip(new Payload("serialized")));
        checks.put("resourceUrlConnection", resourceUrlCheck());
        checks.put("nestedPropertiesResource", propertiesResourceCheck());
        checks.put("unicodeResource", "Java Guard — café — 日本語".equals(readResource("/compat/data/unicode.txt").trim()));
        checks.put("structuredResource", readResource("/compat/data/fixture.json").contains("\"fixture\": \"jdk21\""));
        checks.put("classLookup", Class.forName("org.springframework.boot.SpringApplication") != null);
        checks.put("classMajor", classMajor() == CompatApplication.EXPECTED_MAJOR);
        checks.put("languageApi", LanguageFeatures.check());
        return checks;
    }
    public static boolean allTrue(Map<String, Boolean> checks) {
        if (checks.isEmpty()) return false;
        for (Boolean value : checks.values()) if (!Boolean.TRUE.equals(value)) return false;
        return true;
    }
    public static int classMajor() {
        String path = "/" + CompatApplication.class.getName().replace('.', '/') + ".class";
        try (DataInputStream in = new DataInputStream(CompatApplication.class.getResourceAsStream(path))) {
            if (in.readInt() != 0xCAFEBABE) throw new IllegalStateException("bad class header");
            in.readUnsignedShort(); return in.readUnsignedShort();
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
    private static boolean roundTrip(Payload payload) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(payload); }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return "serialized".equals(((Payload) in.readObject()).value);
        }
    }
    private static boolean resourceUrlCheck() throws Exception {
        URL resource = CompatApplication.class.getResource("/compat-marker.txt");
        if (resource == null) return false;
        try (InputStream input = resource.openConnection().getInputStream()) { return "compat-resource".equals(read(input).trim()); }
    }
    private static boolean propertiesResourceCheck() throws Exception {
        Properties resource = new Properties();
        try (InputStream input = CompatApplication.class.getResourceAsStream("/compat/data/profile.properties")) {
            if (input == null) return false; resource.load(input);
        }
        return "jdk21".equals(resource.getProperty("fixture.id"))
                && "top-level-packages".equals(resource.getProperty("fixture.layout"));
    }
    private static String readResource(String path) throws Exception {
        try (InputStream input = CompatApplication.class.getResourceAsStream(path)) { return input == null ? "" : read(input); }
    }
    private static String read(InputStream input) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] buffer = new byte[256]; int count;
        while ((count = input.read(buffer)) >= 0) out.write(buffer, 0, count);
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
