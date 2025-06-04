use crate::base::common::{inner_key, resource_key};
use crate::base::error::{MessageError, Result};
use crate::with_message;
use ring::aead::{Aad, BoundKey, Nonce, NonceSequence, OpeningKey, UnboundKey, AES_256_GCM, NONCE_LEN};
use ring::error::Unspecified;

pub fn decrypt(data: &mut [u8]) -> Result<&[u8]> {
    let unbound_key = with_message!(UnboundKey::new(&AES_256_GCM, &inner_key()), "密钥初始化失败！")?;
    let nonce = with_message!((&data[0..NONCE_LEN]).try_into(), "解密信息失败")?;
    let mut open_key = OpeningKey::new(unbound_key,
                                       OnceNonceSequence::new(nonce));
    with_message!(open_key.open_in_place(Aad::empty(), &mut data[NONCE_LEN..]), "解密失败")
}
pub fn decrypt_resource(data: &mut [u8]) -> Result<&[u8]> {
    let unbound_key = with_message!(UnboundKey::new(&AES_256_GCM, &resource_key()), "资源密钥初始化失败！")?;
    let nonce = with_message!((&data[0..NONCE_LEN]).try_into(), "解密资源数据失败")?;
    let mut open_key = OpeningKey::new(unbound_key,
                                       OnceNonceSequence::new(nonce));
    with_message!(open_key.open_in_place(Aad::empty(), &mut data[NONCE_LEN..]), "资源解密失败")
}


struct OnceNonceSequence(Option<[u8;NONCE_LEN]>);

impl OnceNonceSequence {
    /// Constructs the sequence allowing `advance()` to be called
    /// `allowed_invocations` times.
    fn new(nonce: [u8;NONCE_LEN]) -> Self {
        Self(Some(nonce))
    }
}

impl NonceSequence for OnceNonceSequence {
    fn advance(&mut self) -> core::result::Result<Nonce, Unspecified> {
        Ok(Nonce::assume_unique_for_key(self.0.take().ok_or(Unspecified)?))
    }
}

#[cfg(test)]
mod test {
    use crate::util::aes_util::OnceNonceSequence;
    use ring::aead::{Aad, BoundKey, Nonce, OpeningKey, SealingKey, AES_256_GCM};
    #[test]
    fn test() {
        let mut data = b"asdasdfasfasf";
        println!("{:?}", &data);
        let key = [0u8;32];
        let digest = [0u8; 12];

        let nonce_seq = OnceNonceSequence::new(digest);
        let nonce = Nonce::assume_unique_for_key(digest);

        let mut opening_key = SealingKey::new(ring::aead::UnboundKey::new(&AES_256_GCM, &key).unwrap(), nonce_seq);
        let mut result = data.to_vec();
        result.resize(result.len() + AES_256_GCM.tag_len(), 0);
        opening_key.seal_in_place_append_tag(Aad::from(""), &mut result).unwrap();
        println!("{:?}", &result);
        //---------------------------
        let nonce_seq = OnceNonceSequence::new(digest);
        let nonce = Nonce::assume_unique_for_key(digest);

        let mut opening_key = OpeningKey::new(ring::aead::UnboundKey::new(&AES_256_GCM, &key).unwrap(), nonce_seq);
        let mut result = opening_key.open_in_place(Aad::from(""), &mut result).unwrap();
        println!("{:?}", &result);
    }
}