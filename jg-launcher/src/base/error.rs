use std::fmt::{Debug, Display, Formatter};

#[derive(Debug, Clone)]
pub struct MessageError {
    pub msg: String
}

#[macro_export]
macro_rules! with_message {
    ($result:expr, $msg:expr) => {
        match $result {
            Ok(t) => Ok(t),
            Err(e) => Err(MessageError::new(&format!("{}: {}", $msg, e))),
        }
    };
}

impl MessageError {
    pub fn new(msg: &str) -> MessageError {
        MessageError {
            msg: String::from(msg)
        }
    }
}

impl<T> Into<Result<T>> for MessageError {
    fn into(self) -> Result<T> {
        Err(self)
    }
}

impl Display for MessageError {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        f.write_str(&self.msg)
    }
}

impl std::error::Error for MessageError {

}

pub type Result<T> = core::result::Result<T, MessageError>;