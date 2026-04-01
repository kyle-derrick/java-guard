use crate::base::common::{INTERNAL_URL_CONNECTION_CLASS, INTERNAL_URL_CONNECTION_DESC, INTERNAL_URL_CONNECTION_METHOD};
use crate::base::opcode::opcodes;
use jclass::attribute_info::CodeAttribute;
use jclass::common::constants::CODE_TAG;
use jclass::constant_pool::{ConstantPool, ConstantValue};
use jclass::jclass_info::JClassInfo;
use std::io::{BufWriter, Cursor};
use std::string::ToString;

const URL_OPEN_CONNECTION_METHOD_NAME: &str = "openConnection";

pub fn url_extended_processing(class_data: &[u8]) -> Option<Vec<u8>> {
    let mut info = match JClassInfo::from_reader(&mut Cursor::new(class_data).into()) {
        Ok(info) => info,
        Err(err) => {
            eprintln!("WARN: URL class parse failed: {}", err);
            return None;
        }
    };
    // INTERNAL_URL_CONNECTION_CLASS
    // INTERNAL_URL_CONNECTION_METHOD
    // INTERNAL_URL_CONNECTION_DESC
    let url_class_utf8_index = info.constant_pool.add_constant(ConstantValue::ConstantUtf8(INTERNAL_URL_CONNECTION_CLASS.to_string()));
    let url_method_utf8_index = info.constant_pool.add_constant(ConstantValue::ConstantUtf8(INTERNAL_URL_CONNECTION_METHOD.to_string()));
    let url_desc_utf8_index = info.constant_pool.add_constant(ConstantValue::ConstantUtf8(INTERNAL_URL_CONNECTION_DESC.to_string()));
    let url_class_index = info.constant_pool.add_constant(ConstantValue::ConstantClass(url_class_utf8_index));
    let url_desc_index = info.constant_pool.add_constant(ConstantValue::ConstantNameAndType(url_method_utf8_index, url_desc_utf8_index));
    let url_method_index = info.constant_pool.add_constant(ConstantValue::ConstantMethodref(url_class_index, url_desc_index));

    for method in &mut info.methods {
        if check_name(&info.constant_pool, method.name, URL_OPEN_CONNECTION_METHOD_NAME) {
            for attr in &mut method.attributes {
                if check_name(&info.constant_pool, attr.name, CODE_TAG) {
                    let mut code_attr = match CodeAttribute::new_with_data(&attr.data) {
                        Ok(code_attr) => code_attr,
                        Err(err) => {
                            eprintln!("WARN: Code attribute parse failed: {}", err);
                            continue;
                        }
                    };
                    let end_code_index = code_attr.codes.len() - 1;
                    let end_code = code_attr.codes[end_code_index];
                    code_attr.codes[end_code_index] = opcodes::INVOKESTATIC;
                    let method_index_bytes = url_method_index.to_be_bytes();
                    code_attr.codes.extend_from_slice(&[method_index_bytes[0], method_index_bytes[1], end_code]);
                    // 无需更改栈深

                    match code_attr.to_bytes() {
                        Ok(bytes) => {
                            attr.data.resize(bytes.len(), 0);
                            attr.data.copy_from_slice(&bytes);
                        }
                        Err(err) => {
                            eprintln!("WARN: Code attribute to bytes failed: {}", err);
                        }
                    }
                }
            }
        }
    }

    let mut extended_class_data = Vec::with_capacity(class_data.len() + 6);
    {
        let mut writer = BufWriter::new(&mut extended_class_data).into();
        match info.write_to(&mut writer) {
            Ok(_) => {}
            Err(err) => {
                eprintln!("WARN: failed to write extended class data: {}", err);
                return None;
            }
        }
    }
    Some(extended_class_data)
}

#[inline]
fn check_name(const_pool: &ConstantPool, name_index: u16, name: &str) -> bool {
    let const_item = const_pool.get_constant_item(name_index);
    if let ConstantValue::ConstantUtf8(method_name) = const_item {
        if method_name.as_str() == name {
            return true;
        }
    }
    false
}