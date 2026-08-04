package io.kyle.javaguard.test;

import io.kyle.javaguard.bean.KeyInfo;
import io.kyle.javaguard.bean.TransformInfo;
import io.kyle.javaguard.transform.ClassTransformer;
import io.kyle.javaguard.util.ClassFileUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public class ClassTransformTest {
    @Test
    public void encryptAndDecryptRoundTripInMemory() throws Exception {
        byte[] original = ClassStubGeneratorTest.subjectClass();
        ClassTransformer transformer = new ClassTransformer(transformInfo());

        ByteArrayOutputStream encryptedOutput = new ByteArrayOutputStream();
        Assert.assertTrue(transformer.encrypt(new ByteArrayInputStream(original), encryptedOutput));
        byte[] encrypted = encryptedOutput.toByteArray();

        Assert.assertFalse("encryption must replace the original class bytes", Arrays.equals(original, encrypted));
        Assert.assertTrue("encrypted output must carry the Java Guard class marker",
                ClassFileUtils.isEncryptClass(encrypted));

        ByteArrayOutputStream decryptedOutput = new ByteArrayOutputStream();
        Assert.assertTrue(transformer.decrypt(new ByteArrayInputStream(encrypted), decryptedOutput));
        Assert.assertArrayEquals("decrypt must reproduce the exact class file", original,
                decryptedOutput.toByteArray());
    }

    @Test
    public void encryptingAnEncryptedClassIsIdempotent() throws Exception {
        byte[] original = ClassStubGeneratorTest.subjectClass();
        ClassTransformer transformer = new ClassTransformer(transformInfo());
        ByteArrayOutputStream firstOutput = new ByteArrayOutputStream();
        transformer.encrypt(new ByteArrayInputStream(original), firstOutput);

        ByteArrayOutputStream secondOutput = new ByteArrayOutputStream();
        transformer.encrypt(new ByteArrayInputStream(firstOutput.toByteArray()), secondOutput);

        Assert.assertArrayEquals(firstOutput.toByteArray(), secondOutput.toByteArray());
    }

    private static TransformInfo transformInfo() {
        byte[] sha512 = DigestUtils.sha512("class-transform-test");
        TransformInfo info = new TransformInfo();
        info.setKeyInfo(new KeyInfo(Arrays.copyOfRange(sha512, 0, 32)));
        info.setResourceKeyInfo(new KeyInfo(Arrays.copyOfRange(sha512, 32, 64)));
        return info;
    }
}
