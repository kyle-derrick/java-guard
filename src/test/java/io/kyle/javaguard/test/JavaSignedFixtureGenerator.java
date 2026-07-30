package io.kyle.javaguard.test;

import io.kyle.javaguard.util.ZipSignUtils;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Generates the deterministic Java-signed fixture consumed by jg-launcher tests. */
public final class JavaSignedFixtureGenerator {
    // RFC 8032 section 7.1, test vector 1. This public test seed must never be used in production.
    private static final byte[] RFC8032_TEST_SEED = decodeHex(
            "9d61b19deffd5a60ba844af492ec2cc4"
                    + "4449c5697b326919703bac031cae7f60");
    private static final long ZIP_TIMESTAMP = 315532800000L; // 1980-01-01T00:00:00Z
    private static final String ORIGINAL_COMMENT = "java-guard deterministic interop fixture";

    private JavaSignedFixtureGenerator() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("usage: JavaSignedFixtureGenerator <output.jar>");
        }
        generate(new File(args[0]));
    }

    public static void generate(File output) throws Exception {
        File parent = output.getCanonicalFile().getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IllegalStateException("cannot create fixture directory: " + parent);
        }
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(output))) {
            zip.setComment(ORIGINAL_COMMENT);
            addStoredEntry(zip, "META-INF/MANIFEST.MF",
                    ("Manifest-Version: 1.0\r\n"
                            + "Main-Class: fixture.Main\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            addStoredEntry(zip, "fixture/Main.class", fixtureMainClass());
        }

        Ed25519PrivateKeyParameters privateKey = new Ed25519PrivateKeyParameters(RFC8032_TEST_SEED, 0);
        Ed25519Signer signer = new Ed25519Signer();
        signer.init(true, privateKey);
        ZipSignUtils.sign(output, signer);
    }

    private static void addStoredEntry(ZipOutputStream zip, String name, byte[] content) throws Exception {
        CRC32 crc = new CRC32();
        crc.update(content);
        ZipEntry entry = new ZipEntry(name);
        entry.setMethod(ZipEntry.STORED);
        entry.setTime(ZIP_TIMESTAMP);
        entry.setSize(content.length);
        entry.setCompressedSize(content.length);
        entry.setCrc(crc.getValue());
        zip.putNextEntry(entry);
        zip.write(content);
        zip.closeEntry();
    }

    private static byte[] fixtureMainClass() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
                "fixture/Main", null, "java/lang/Object", null);
        MethodVisitor constructor = writer.visitMethod(Opcodes.ACC_PRIVATE, "<init>", "()V", null, null);
        constructor.visitCode();
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        constructor.visitInsn(Opcodes.RETURN);
        constructor.visitMaxs(1, 1);
        constructor.visitEnd();
        MethodVisitor main = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "main", "([Ljava/lang/String;)V", null, null);
        main.visitCode();
        main.visitInsn(Opcodes.RETURN);
        main.visitMaxs(0, 1);
        main.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] decodeHex(String value) {
        try {
            return Hex.decodeHex(value.toCharArray());
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid test seed", e);
        }
    }
}
