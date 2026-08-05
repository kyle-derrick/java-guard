use crate::base::common::{
    INTERNAL_URL_CONNECTION_CLASS, INTERNAL_URL_CONNECTION_DESC, INTERNAL_URL_CONNECTION_METHOD,
};
use crate::base::opcode::opcodes;
use crate::util::byte_utils;
use jclass::attribute_info::CodeAttribute;
use jclass::common::constants::CODE_TAG;
use jclass::constant_pool::{ConstantPool, ConstantValue};
use jclass::jclass_info::JClassInfo;
use std::cmp::min;
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
    let url_class_utf8_index = info.constant_pool.add_constant(ConstantValue::ConstantUtf8(
        INTERNAL_URL_CONNECTION_CLASS.to_string(),
    ));
    let url_method_utf8_index = info.constant_pool.add_constant(ConstantValue::ConstantUtf8(
        INTERNAL_URL_CONNECTION_METHOD.to_string(),
    ));
    let url_desc_utf8_index = info.constant_pool.add_constant(ConstantValue::ConstantUtf8(
        INTERNAL_URL_CONNECTION_DESC.to_string(),
    ));
    let url_class_index = info
        .constant_pool
        .add_constant(ConstantValue::ConstantClass(url_class_utf8_index));
    let url_desc_index = info
        .constant_pool
        .add_constant(ConstantValue::ConstantNameAndType(
            url_method_utf8_index,
            url_desc_utf8_index,
        ));
    let url_method_index = info
        .constant_pool
        .add_constant(ConstantValue::ConstantMethodref(
            url_class_index,
            url_desc_index,
        ));

    for method in &mut info.methods {
        if check_name(
            &info.constant_pool,
            method.name,
            URL_OPEN_CONNECTION_METHOD_NAME,
        ) {
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
                    code_attr.codes.extend_from_slice(&[
                        method_index_bytes[0],
                        method_index_bytes[1],
                        end_code,
                    ]);
                    // 包装后的返回序列不增加操作数栈峰值，因此保留 max_stack。 / The wrapped return sequence does not raise the operand-stack peak, so max_stack is retained.

                    for code_attr in &mut code_attr.attributes {
                        if check_name(&info.constant_pool, code_attr.name, "LocalVariableTable") {
                            let (length, table) = parse_local_variable_table(&code_attr.data);
                            if length == -1 || table.is_empty() {
                                break;
                            }

                            let length = length as u16;
                            for item in &table {
                                if (item.start_pc + item.length) == length {
                                    code_attr.data[item.length_index..item.length_index + 2]
                                        .copy_from_slice(&(item.length + 3).to_be_bytes());
                                }
                            }
                        }
                    }
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

#[derive(Debug)]
pub struct LocalVariableEntry {
    pub start_pc: u16,
    pub length: u16,         // 作用域长度（字节码偏移）
    pub length_index: usize, // 数据偏移
}

fn parse_local_variable_table(data: &[u8]) -> (i32, Vec<LocalVariableEntry>) {
    if data.len() < 2 {
        eprintln!("WARN: URL class has invalid method");
        return (-1, Vec::with_capacity(0));
    }

    let size = byte_utils::byte_be_to_u16_fast(data, 0) as usize;
    let size_from_data_len = (data.len() - 2) / 10;
    if size_from_data_len < size {
        eprintln!("WARN: local table size invalid {size} / {size_from_data_len} ")
    }
    let size = min(size, size_from_data_len);
    let mut entries = Vec::with_capacity(size);
    let mut data_index = 2;
    let mut this_length = -1;
    for _ in 0..size {
        let start_pc = byte_utils::byte_be_to_u16_fast(data, data_index);
        data_index += 2;
        let length_index = data_index;
        let length = byte_utils::byte_be_to_u16_fast(data, data_index);
        data_index += 6;
        let index = byte_utils::byte_be_to_u16_fast(data, data_index);
        data_index += 2;

        if index == 0 {
            this_length = length as i32;
        }

        entries.push(LocalVariableEntry {
            start_pc,
            length,
            length_index,
        });
    }

    (this_length, entries)
}
