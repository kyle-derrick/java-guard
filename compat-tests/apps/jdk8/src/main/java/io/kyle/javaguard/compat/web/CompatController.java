package io.kyle.javaguard.compat.web;
import io.kyle.javaguard.compat.CompatApplication;
import io.kyle.javaguard.compat.check.CompatChecks;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
@RestController
public class CompatController {
    private final CompatChecks compatChecks;
    public CompatController(CompatChecks compatChecks) { this.compatChecks = compatChecks; }
    @GetMapping("/compat/check")
    public ResponseEntity<Map<String, Object>> check(
            @RequestHeader(value = "X-Compat-Launch-Token", required = false) String requestToken,
            @RequestParam(defaultValue = "false") boolean fail) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        String launchToken = System.getProperty("compat.launch-token", "");
        body.put("fixture", "jdk8");
        body.put("bootVersion", SpringApplication.class.getPackage().getImplementationVersion());
        body.put("javaHome", System.getProperty("java.home"));
        body.put("javaVersion", System.getProperty("java.version"));
        body.put("javaFeature", javaFeature());
        body.put("pid", processId());
        body.put("classMajor", CompatChecks.classMajor());
        body.put("expectedClassMajor", CompatApplication.EXPECTED_MAJOR);
        body.put("launchTokenSha256", sha256(launchToken));
        if (!tokenMatches(launchToken, requestToken)) {
            body.put("ok", false);
            body.put("error", "launch token mismatch");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
        }
        try {
            Map<String, Boolean> checks = compatChecks.run();
            body.put("checks", checks);
            boolean ok = !fail && CompatChecks.allTrue(checks);
            body.put("ok", ok);
            if (fail) body.put("error", "requested failure");
            else if (!ok) body.put("error", "one or more compatibility checks failed");
            return ResponseEntity.status(ok ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        } catch (Exception e) {
            body.put("ok", false);
            body.put("error", e.getClass().getName() + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }
    private static boolean tokenMatches(String expected, String actual) {
        return !expected.isEmpty() && actual != null && MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }
    private static int javaFeature() {
        String version = System.getProperty("java.specification.version", "");
        if (version.startsWith("1.")) version = version.substring(2);
        int dot = version.indexOf('.');
        return Integer.parseInt(dot < 0 ? version : version.substring(0, dot));
    }
    private static long processId() {
        String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
        int separator = runtimeName.indexOf('@');
        String value = separator < 0 ? runtimeName : runtimeName.substring(0, separator);
        try { return Long.parseLong(value); }
        catch (NumberFormatException e) { throw new IllegalStateException("cannot parse JVM pid: " + runtimeName, e); }
    }
    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(64);
            for (byte item : digest) result.append(String.format("%02x", item & 0xff));
            return result.toString();
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
}
