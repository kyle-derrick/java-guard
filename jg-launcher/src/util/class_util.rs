use crate::base::common::{ENCRYPT_DATA_TAG, INTERNAL_URL_CONNECTION_CLASS, INTERNAL_URL_CONNECTION_DESC, INTERNAL_URL_CONNECTION_METHOD};
use crate::base::opcode::opcodes;
use crate::util::{aes_util, byte_utils};
use jclass::attribute_info::CodeAttribute;
use jclass::common::constants::CODE_TAG;
use jclass::constant_pool::{ConstantPool, ConstantValue};
use jclass::jclass_info::JClassInfo;
use jclass::util::class_scan::fast_scan_class;
use std::io::{BufWriter, Cursor};
use std::string::ToString;

const URL_OPEN_CONNECTION_METHOD_NAME: &str = "openConnection";

macro_rules! check_index {
    ($arr: expr, $read_len: expr) => {
        if $arr.len() < $read_len {
            eprintln!("class data is invalid, index: {}, max len: {}", $read_len, $arr.len());
            return None;
        }
    };
}

#[inline]
pub fn try_decrypt_class(class_data: &[u8]) -> Option<Vec<u8>> {
    match fast_scan_class(class_data, ENCRYPT_DATA_TAG, false) {
        Ok(Some(info)) if info.specify_attribute.is_some() => {
            let data_range = info.specify_attribute?;
            let mut en_data = class_data[data_range.start..data_range.end].to_vec();
            let data = match aes_util::decrypt(&mut en_data) {
                Ok(data) => data,
                Err(err) => {
                    eprintln!("decrypt class data attribute failed: {}", err);
                    return None;
                }
            };

            let mut new_class_data_bytes = Vec::with_capacity(class_data.len());
            let mut index = 0;
            let mut copied_index = 0;
            loop {
                let start = index;
                index += 2;
                check_index!(data, index);
                let const_index = byte_utils::byte_be_to_u16_fast(data, start) as usize;
                let len = match data[index] {
                    b'I' | b'F' => {
                        4
                    }
                    b'L' | b'D' => {
                        8
                    }
                    b'S' => {
                        check_index!(data, index+3);
                        2 + (byte_utils::byte_be_to_u16_fast(data, index + 1) as usize)
                    }
                    _ => {
                        index += 1;
                        break;
                    }
                };
                index += 1;
                let const_start = info.consts.get(const_index - 1)? + 1;
                let const_end = *info.consts.get(const_index)?;
                // check_index!(class_data, const_start);
                new_class_data_bytes.extend_from_slice(&class_data[copied_index..const_start]);
                copied_index = const_end;
                let start = index;
                index += len;
                check_index!(data, index);
                new_class_data_bytes.extend_from_slice(&data[start..index]);
            }

            let start = index;
            index += 4;
            check_index!(data, index);
            let codes_size = byte_utils::byte_be_to_u32_fast(data, start) as usize;
            let mut info_code_index = 0;
            for _ in 0..codes_size {
                let mut code_range = info.method_codes[info_code_index];
                info_code_index+=1;
                while code_range.0 == 0 {
                    code_range = info.method_codes[info_code_index];
                    info_code_index+=1;
                }
                let start = index;
                index += 4;
                let code_len_start = index;
                index += 4;
                check_index!(data, index);
                let code_len = byte_utils::byte_be_to_u32_fast(data, code_len_start) as usize;
                let ori_attr_start = code_range.0 + 2;
                let ori_code_start = ori_attr_start + 4 + 2 + 2;
                // check_index!(class_data, ori_code_start+4);
                new_class_data_bytes.extend_from_slice(&class_data[copied_index..ori_attr_start]);
                let ori_attr_len = byte_utils::byte_be_to_u32_fast(class_data, ori_attr_start) as usize;
                let ori_code_len = byte_utils::byte_be_to_u32_fast(class_data, ori_code_start) as usize;
                copied_index = ori_code_start + 4 + (ori_code_len);
                if copied_index >= code_range.1 {
                    eprintln!("code info error, code len gt than attribute len.");
                    return None;
                }
                // let start = index;
                index += code_len;
                new_class_data_bytes.extend_from_slice(&((ori_attr_len - ori_code_len + code_len) as u32).to_be_bytes());
                // new_class_data_bytes.extend_from_slice(&class_data[ori_attr_start+4..ori_code_start]);
                check_index!(data, index);
                new_class_data_bytes.extend_from_slice(&data[start..index]);
            }
            new_class_data_bytes.extend_from_slice(&class_data[copied_index..data_range.start - 4]);
            new_class_data_bytes.extend_from_slice(&[0, 0, 0, 0]);
            new_class_data_bytes.extend_from_slice(&class_data[data_range.end..]);

            Some(new_class_data_bytes)
        }
        Err(err) => {
            eprintln!("analysis class failed: {}", err);
            None
        }
        _ => {
            None
        }
    }
}

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
        if method_name == name {
            return true;
        }
    }
    false
}


#[cfg(test)]
mod test {
    use crate::util::class_util::try_decrypt_class;
    use jclass::util::class_scan::fast_scan_class;
    use std::fs::File;
    use std::io::{Read, Write};
    use std::time::Instant;

    #[test]
    fn class_decrypt_test() {
        let mut f = File::open("D:\\data\\code\\project\\java-guard\\out\\tmp\\antlr-4.13.2-complete\\org\\antlr\\v4\\Tool.class").unwrap();
        let mut class_data = Vec::with_capacity(32 * 1024);
        f.read_to_end(&mut class_data).unwrap();

        let new_class_data = try_decrypt_class(&class_data).unwrap();

        let mut of = File::create("D:\\data\\code\\project\\java-guard\\out\\tmp\\antlr-4.13.2-complete\\org\\antlr\\v4\\Tool.2.class").unwrap();
        of.write_all(&new_class_data).unwrap();
        // println!("{:?}", new_class_data_bytes);

        let i = fast_scan_class(&new_class_data, &[], true).unwrap();
        println!("{:?}", i);


        let now = Instant::now();
        for i in 0..1000 {
            let new_class_data = try_decrypt_class(&class_data).unwrap();
        }
        println!("{} ns", now.elapsed().as_nanos());
    }
}