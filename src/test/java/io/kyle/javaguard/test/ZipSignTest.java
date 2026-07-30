package io.kyle.javaguard.test;

import io.kyle.javaguard.bean.SignatureInfo;
import io.kyle.javaguard.util.ZipSignUtils;
import net.lingala.zip4j.ZipFile;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipSignTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void signsAndVerifiesZipWithoutExistingComment() throws Exception {
        File zip = createZip(null);
        SignatureInfo keys = newKeys();

        ZipSignUtils.sign(zip, keys.newSignSigner());

        Assert.assertTrue(ZipSignUtils.verify(zip, keys.newVerifySigner()));
    }

    @Test
    public void signsAndVerifiesZipWhilePreservingExistingComment() throws Exception {
        String originalComment = "existing portable comment";
        File zip = createZip(originalComment);
        SignatureInfo keys = newKeys();

        ZipSignUtils.sign(zip, keys.newSignSigner());

        Assert.assertTrue(ZipSignUtils.verify(zip, keys.newVerifySigner()));
        Assert.assertTrue(new ZipFile(zip).getComment().startsWith(originalComment));
    }

    @Test
    public void repeatedSigningReplacesSignatureAndPreservesOriginalComment() throws Exception {
        String originalComment = "original comment";
        File zip = createZip(originalComment);
        SignatureInfo keys = newKeys();

        ZipSignUtils.sign(zip, keys.newSignSigner());
        String firstSignedComment = new ZipFile(zip).getComment();
        ZipSignUtils.sign(zip, keys.newSignSigner());
        String secondSignedComment = new ZipFile(zip).getComment();

        Assert.assertTrue(ZipSignUtils.verify(zip, keys.newVerifySigner()));
        Assert.assertTrue(secondSignedComment.startsWith(originalComment));
        Assert.assertEquals(firstSignedComment.length(), secondSignedComment.length());
    }

    @Test
    public void rejectsMalformedSignatureSuffixWithoutReadingBeforeComment() throws Exception {
        File zip = createZip("ordinary-comment-not-a-signatureffff");
        SignatureInfo keys = newKeys();

        try {
            ZipSignUtils.verify(zip, keys.newVerifySigner());
            Assert.fail("expected missing signature failure");
        } catch (io.kyle.javaguard.exception.TransformException expected) {
            Assert.assertTrue(expected.getMessage().contains("not found signer"));
        }
    }

    @Test
    public void signingTreatsMalformedSuffixAsOriginalComment() throws Exception {
        String originalComment = "ordinary-comment-not-a-signatureffff";
        File zip = createZip(originalComment);
        SignatureInfo keys = newKeys();

        ZipSignUtils.sign(zip, keys.newSignSigner());

        Assert.assertTrue(ZipSignUtils.verify(zip, keys.newVerifySigner()));
        Assert.assertTrue(new ZipFile(zip).getComment().startsWith(originalComment));
    }

    @Test
    public void verificationFailsAfterSignedContentIsTampered() throws Exception {
        File zip = createZip("comment");
        SignatureInfo keys = newKeys();
        ZipSignUtils.sign(zip, keys.newSignSigner());

        try (RandomAccessFile file = new RandomAccessFile(zip, "rw")) {
            file.seek(0);
            file.write(file.read() ^ 1);
        }

        Assert.assertFalse(ZipSignUtils.verify(zip, keys.newVerifySigner()));
    }

    private File createZip(String comment) throws Exception {
        File zip = temporaryFolder.newFile("archive-" + System.nanoTime() + ".zip");
        try (ZipOutputStream output = new ZipOutputStream(new FileOutputStream(zip))) {
            if (comment != null) {
                output.setComment(comment);
            }
            output.putNextEntry(new ZipEntry("content.txt"));
            output.write("signed content".getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        }
        return zip;
    }

    private static SignatureInfo newKeys() {
        Ed25519PrivateKeyParameters privateKey =
                new Ed25519PrivateKeyParameters(new SecureRandom());
        SignatureInfo keys = new SignatureInfo();
        keys.setPrivateKey(privateKey);
        keys.setPublicKey(privateKey.generatePublicKey());
        return keys;
    }
}
