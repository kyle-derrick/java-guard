{
    java.io.InputStream in = this.${originMethod}();
    if ("jar".equals(this.protocol)) {
        byte[] encryptHeader = new byte[] {${encryptHeader}};
        java.io.InputStream bin = new java.io.BufferedInputStream(in, encryptHeader.length);
        bin.mark(encryptHeader.length);
        byte[] header = new byte[encryptHeader.length];
        int read = bin.read(header);
        if (read > encryptHeader.length && java.util.Objects.deepEquals(header, encryptHeader)) {
            javax.crypto.spec.SecretKeySpec sks = new javax.crypto.spec.SecretKeySpec(new byte[] {${resourceKey}}, "${algorithm}");
            javax.crypto.Cipher cipher = null;
            try {
                cipher = javax.crypto.Cipher.getInstance("${transformation}");
                cipher.init(javax.crypto.Cipher.DECRYPT_MODE, sks);
            return new io.kyle.javaguard.support.JGTransformInputStream(in, cipher);
            } catch (Exception ignored) {
            }
        }
        bin.reset();
        return bin;
    }
    return in;
}