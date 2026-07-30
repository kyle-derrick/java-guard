use crate::base::common::{inner_key, resource_key};
use crate::base::error::{MessageError, Result};
use crate::with_message;
use ring::aead::{Aad, BoundKey, Nonce, NonceSequence, OpeningKey, UnboundKey, AES_256_GCM, NONCE_LEN};
use ring::error::Unspecified;

pub fn decrypt(data: &mut [u8]) -> Result<&[u8]> {
    let unbound_key = with_message!(UnboundKey::new(&AES_256_GCM, &inner_key()), "密钥初始化失败！")?;
    let len = data.len();
    let data_end = len-NONCE_LEN;
    let nonce = with_message!((&data[data_end..len]).try_into(), "解密信息失败")?;
    let mut open_key = OpeningKey::new(unbound_key,
                                       OnceNonceSequence::new(nonce));
    with_message!(open_key.open_in_place(Aad::empty(), &mut data[..data_end]), "解密失败")
}
pub fn decrypt_resource(data: &mut [u8]) -> Result<&[u8]> {
    let unbound_key = with_message!(UnboundKey::new(&AES_256_GCM, &resource_key()), "资源密钥初始化失败！")?;
    let len = data.len();
    let data_end = len-NONCE_LEN;
    let nonce = with_message!((&data[data_end..len]).try_into(), "解密资源数据失败")?;
    let mut open_key = OpeningKey::new(unbound_key,
                                       OnceNonceSequence::new(nonce));
    with_message!(open_key.open_in_place(Aad::empty(), &mut data[..data_end]), "资源解密失败")
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