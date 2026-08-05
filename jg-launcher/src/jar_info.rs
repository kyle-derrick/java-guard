use crate::base::common::{MAIN_CLASS_PREFIX, MANIFEST_FILE};
use std::fs::File;
#[cfg(not(feature = "dev"))]
use {
    crate::base::common::{pub_key_pair, SIGN_LEN, SIGN_LEN_HEX_LEN},
    base64::prelude::BASE64_URL_SAFE_NO_PAD,
    base64::Engine,
    std::fs,
};
// use file_lock::{FileLock, FileOptions};
use std::io;
use zip::ZipArchive;

#[allow(unused)]
#[derive(Debug)]
pub struct JarInfo {
    path: String,
    // file: FileLock,
    #[cfg(not(feature = "dev"))]
    signature: Vec<u8>,
    jar_data_end_index: usize,
    main_class: String,
}

// const MANIFEST_FILE: &str = "META-INF/MANIFEST.MF";
// const MAIN_CLASS_PREFIX: &str = "Main-Class:";
// const SIGNATURE_PREFIX: &str = "JG-Signature:";

#[cfg(not(feature = "dev"))]
const SIGNATURE_MAGIC: &[u8] = b"jgs-v1:";

#[cfg(not(feature = "dev"))]
fn extract_sign_from_comment(comment: &[u8]) -> Result<Vec<u8>, &'static str> {
    if comment.len() <= SIGN_LEN_HEX_LEN {
        return Err("jar has no signature");
    }

    let encoded_end = comment.len() - SIGN_LEN_HEX_LEN;
    let trailer = &comment[encoded_end..];
    if !trailer
        .iter()
        .all(|value| value.is_ascii_digit() || (b'a'..=b'f').contains(value))
    {
        return Err("jar signature info is invalid");
    }
    let sign_len_bytes = hex::decode(trailer).map_err(|_| "jar signature info is invalid")?;
    let sign_len_bytes: [u8; 2] = sign_len_bytes
        .try_into()
        .map_err(|_| "jar signature info length is invalid")?;
    let encoded_len = u16::from_le_bytes(sign_len_bytes) as usize;
    if encoded_len == 0 {
        return Err("jar signature length is invalid");
    }

    let encoded_start = encoded_end
        .checked_sub(encoded_len)
        .ok_or("jar signature length is invalid")?;
    let encoded = &comment[encoded_start..encoded_end];
    let marker_prefix = &comment[..encoded_start];
    let marked = marker_prefix.ends_with(SIGNATURE_MAGIC);
    if !marked && has_malformed_signature_marker(marker_prefix) {
        return Err("jar signature marker is invalid");
    }
    // With no marker-shaped suffix, accept the old base64+hex layout so jars
    // signed before the versioned marker was introduced remain verifiable.

    let signature = BASE64_URL_SAFE_NO_PAD
        .decode(encoded)
        .map_err(|_| "jar signature is invalid")?;
    if signature.len() != SIGN_LEN {
        return Err("jar signature length is invalid");
    }
    if BASE64_URL_SAFE_NO_PAD.encode(&signature).as_bytes() != encoded {
        return Err("jar signature encoding is not canonical");
    }
    Ok(signature)
}

#[cfg(not(feature = "dev"))]
fn has_malformed_signature_marker(prefix: &[u8]) -> bool {
    const MARKER_STEM: &[u8] = b"JavaGuard-Signature-";
    let tail_start = prefix
        .iter()
        .rposition(|value| !value.is_ascii())
        .map_or(0, |index| index + 1);
    prefix[tail_start..]
        .windows(MARKER_STEM.len())
        .any(|window| window == MARKER_STEM)
}

impl JarInfo {
    pub fn parse(path: &str) -> Self {
        let jar_file = File::open(path).unwrap_or_else(|_| panic!("can not open jar: {}", path));
        let jar_file_len = jar_file
            .metadata()
            .expect("cannot get jar file metadata")
            .len();
        if jar_file_len > usize::MAX as u64 {
            // Currently only supports 4G and below, support later
            panic!("The jar file is too large, exceeding {}", usize::MAX)
        }
        let mut archive =
            ZipArchive::new(jar_file).unwrap_or_else(|_| panic!("can not open jar: {}", path));
        let manifest = archive
            .by_name(MANIFEST_FILE)
            .expect("not found MANIFEST.MF in jar");
        let manifest_content =
            io::read_to_string(manifest).expect("cannot read MANIFEST.MF in jar");
        let mut main_class = None;
        manifest_content.lines().for_each(|line| {
            if line.starts_with(MAIN_CLASS_PREFIX) && main_class.is_none() {
                main_class = Some(line[MAIN_CLASS_PREFIX.len()..].trim().to_string());
            }
        });
        if main_class.is_none() {
            panic!("not found Main Class in jar")
        }
        let comment = archive.comment();
        #[cfg(not(feature = "dev"))]
        let sign = extract_sign_from_comment(comment).expect("cannot extract jar signature");
        let jar_data_end_index = (jar_file_len as usize)
            .checked_sub(comment.len() + 2)
            .expect("jar comment length is invalid");
        if let Some(main_class) = main_class {
            JarInfo {
                path: path.to_string(),
                // file: file_lock,
                #[cfg(not(feature = "dev"))]
                signature: sign,
                jar_data_end_index,
                main_class,
            }
        } else {
            panic!("jar is invalid: not found main class or not found signature")
        }
    }

    #[cfg(not(feature = "dev"))]
    pub fn verify(&self) {
        self.verify_with_public_key(pub_key_pair())
            .expect("jar signature verify failed");
    }

    #[cfg(not(feature = "dev"))]
    fn verify_with_public_key<B: AsRef<[u8]>>(
        &self,
        public_key: ring::signature::UnparsedPublicKey<B>,
    ) -> Result<(), ring::error::Unspecified> {
        let content =
            fs::read(&self.path).unwrap_or_else(|_| panic!("cannot read jar file: {}", self.path));
        public_key.verify(&content[..self.jar_data_end_index], &self.signature)
    }

    pub fn path(&self) -> &String {
        &self.path
    }
    // pub fn file(&self) -> &FileLock {
    //     &self.file
    // }
    #[cfg(not(feature = "dev"))]
    #[allow(unused)]
    pub fn signature(&self) -> &Vec<u8> {
        &self.signature
    }
    pub fn main_class(&self) -> &String {
        &self.main_class
    }
}

#[cfg(all(test, not(feature = "dev")))]
mod tests {
    use super::*;
    use ring::signature::{UnparsedPublicKey, ED25519};

    const RFC8032_TEST_PUBLIC_KEY: [u8; 32] = [
        0xd7, 0x5a, 0x98, 0x01, 0x82, 0xb1, 0x0a, 0xb7, 0xd5, 0x4b, 0xfe, 0xd3, 0xc9, 0x64, 0x07,
        0x3a, 0x0e, 0xe1, 0x72, 0xf3, 0xda, 0xa6, 0x23, 0x25, 0xaf, 0x02, 0x1a, 0x68, 0xf7, 0x07,
        0x51, 0x1a,
    ];

    #[test]
    fn verifies_java_generated_signed_jar() {
        let path = concat!(
            env!("CARGO_MANIFEST_DIR"),
            "/tests/fixtures/java-signed-v1.jar"
        );
        let jar = JarInfo::parse(path);

        assert_eq!(jar.main_class(), "fixture.Main");
        jar.verify_with_public_key(UnparsedPublicKey::new(&ED25519, RFC8032_TEST_PUBLIC_KEY))
            .expect("Java-generated fixture signature must verify in Rust");
    }

    fn signed_comment(existing_comment: &[u8], signature: &[u8], marked: bool) -> Vec<u8> {
        let encoded = BASE64_URL_SAFE_NO_PAD.encode(signature);
        let mut comment = existing_comment.to_vec();
        if marked {
            comment.extend_from_slice(SIGNATURE_MAGIC);
        }
        comment.extend_from_slice(encoded.as_bytes());
        comment.extend_from_slice(hex::encode((encoded.len() as u16).to_le_bytes()).as_bytes());
        comment
    }

    fn comment_with_encoded(prefix: &[u8], encoded: &[u8]) -> Vec<u8> {
        let mut comment = prefix.to_vec();
        comment.extend_from_slice(encoded);
        comment.extend_from_slice(hex::encode((encoded.len() as u16).to_le_bytes()).as_bytes());
        comment
    }

    #[test]
    fn extracts_marked_signature_with_empty_original_comment() {
        let signature = [0x5a; SIGN_LEN];
        let comment = signed_comment(&[], &signature, true);

        assert_eq!(extract_sign_from_comment(&comment), Ok(signature.to_vec()));
    }

    #[test]
    fn extracts_marked_signature_after_nonempty_original_comment() {
        let signature = [0xa5; SIGN_LEN];
        let comment = signed_comment(b"existing comment", &signature, true);

        assert_eq!(extract_sign_from_comment(&comment), Ok(signature.to_vec()));
    }

    #[test]
    fn extracts_legacy_unmarked_signature() {
        let signature = [0x3c; SIGN_LEN];
        let comment = signed_comment(b"legacy comment", &signature, false);

        assert_eq!(extract_sign_from_comment(&comment), Ok(signature.to_vec()));
    }

    #[test]
    fn rejects_missing_or_zero_signature_length() {
        assert_eq!(extract_sign_from_comment(b""), Err("jar has no signature"));
        assert_eq!(
            extract_sign_from_comment(b"existing comment0000"),
            Err("jar signature length is invalid")
        );
    }

    #[test]
    fn rejects_malformed_or_uppercase_signature_length_hex() {
        assert_eq!(
            extract_sign_from_comment(b"signaturezzzz"),
            Err("jar signature info is invalid")
        );
        assert_eq!(
            extract_sign_from_comment(b"signatureABCD"),
            Err("jar signature info is invalid")
        );
    }

    #[test]
    fn rejects_signature_length_underflow() {
        assert_eq!(
            extract_sign_from_comment(b"abc6400"),
            Err("jar signature length is invalid")
        );
    }

    #[test]
    fn rejects_malformed_marker() {
        let signature = [0x11; SIGN_LEN];
        let comment = signed_comment(b"JavaGuard-Signature-v2:", &signature, false);

        assert_eq!(
            extract_sign_from_comment(&comment),
            Err("jar signature marker is invalid")
        );
    }

    #[test]
    fn rejects_malformed_signature_base64() {
        let comment = comment_with_encoded(SIGNATURE_MAGIC, &[b'!'; 86]);

        assert_eq!(
            extract_sign_from_comment(&comment),
            Err("jar signature is invalid")
        );
    }

    #[test]
    fn rejects_wrong_decoded_signature_size() {
        let short_signature = [0x22; SIGN_LEN - 1];
        let comment = signed_comment(&[], &short_signature, true);

        assert_eq!(
            extract_sign_from_comment(&comment),
            Err("jar signature length is invalid")
        );
    }

    #[test]
    fn rejects_noncanonical_base64() {
        let signature = [0; SIGN_LEN];
        let mut encoded = BASE64_URL_SAFE_NO_PAD.encode(signature).into_bytes();
        let last = encoded.last_mut().expect("encoded signature is nonempty");
        assert_eq!(*last, b'A');
        *last = b'B';
        let comment = comment_with_encoded(SIGNATURE_MAGIC, &encoded);

        assert!(extract_sign_from_comment(&comment).is_err());
    }
}
