use aes_gcm::KeyInit;
use ring::aead::{Aad, BoundKey, Nonce, NonceSequence, OpeningKey, SealingKey, AES_256_GCM};
use ring::error::Unspecified;
use sha2::digest::Update;
use sha2::{Digest, Sha256};

fn decrypt(data: &[u8]) -> Vec<u8> {
    // let cipher = Aes256Gcm::new_from_slice(&inner_key())?;
    // cipher.decrypt_in_place()
    // todo
    vec![]
}


struct OneNonceSequence(Option<Nonce>);

impl OneNonceSequence {
    /// Constructs the sequence allowing `advance()` to be called
    /// `allowed_invocations` times.
    fn new(nonce: Nonce) -> Self {
        Self(Some(nonce))
    }
}

impl NonceSequence for OneNonceSequence {
    fn advance(&mut self) -> Result<Nonce, Unspecified> {
        self.0.take().ok_or(ring::error::Unspecified)
    }
}

#[test]
fn test() {
    let mut data = b"asdasdfasfasf";
    println!("{:?}", &data);
    let key = b"test1234";
    let mut sha256_hasher = Sha256::new();
    Digest::update(&mut sha256_hasher, key);
    let hash_result = sha256_hasher.finalize();
    let digest:[u8;12] = (&md5::compute(hash_result.as_slice()).as_slice()[..12]).try_into().unwrap();
    // ring::digest::SHA256.

    let nonce_seq = OneNonceSequence::new(Nonce::assume_unique_for_key(digest));
    let nonce = Nonce::assume_unique_for_key(digest);

    let mut opening_key = SealingKey::new(ring::aead::UnboundKey::new(&AES_256_GCM, hash_result.as_slice()).unwrap(), nonce_seq);
    let mut result = data.to_vec();
    result.resize(result.len() + AES_256_GCM.tag_len(), 0);
    opening_key.seal_in_place_append_tag(Aad::from(""), &mut result).unwrap();
    println!("{:?}", &result);
    //---------------------------
    let nonce_seq = OneNonceSequence::new(Nonce::assume_unique_for_key(digest));
    let nonce = Nonce::assume_unique_for_key(digest);

    let mut opening_key = OpeningKey::new(ring::aead::UnboundKey::new(&AES_256_GCM, hash_result.as_slice()).unwrap(), nonce_seq);
    let mut result = opening_key.open_in_place(Aad::from(""), &mut result).unwrap();
    println!("{:?}", &result);

    //-----------------


    let mut data:[u8;31] = [120, 120, 224, 106, 129, 142, 77, 228, 117, 155, 181, 249, 4, 98, 3, 70, 20, 165, 113, 65, 13, 232, 31, 37, 73, 129, 140, 31, 14, 195, 21];
    let key = b"test123";
    let mut sha256_hasher = Sha256::new();
    Digest::update(&mut sha256_hasher, key);
    let hash_result = sha256_hasher.finalize();
    let aes_key = hash_result.as_slice();
    let digest:[u8;12] = (&md5::compute(aes_key).as_slice()[..12]).try_into().unwrap();
    println!("{:?}", &aes_key);
    println!("{:?}", &digest);

    let nonce_seq = OneNonceSequence::new(Nonce::assume_unique_for_key(digest));

    let mut opening_key = OpeningKey::new(ring::aead::UnboundKey::new(&AES_256_GCM, aes_key).unwrap(), Nonce::assume_unique_for_key(digest));
    let mut result = opening_key.open_in_place(Aad::empty(), &mut data).unwrap();
    println!("{:?}", &result);

    // let aes256 = Aes256Gcm::new_from_slice(hash_result.as_slice()).unwrap();
    // aes256.decrypt_in_place().unwrap()
}