use args_parser::LauncherArg;
use crate::jvm::jvm_launcher::jvm_launch;

mod args_parser;
mod jar_info;
mod util;
mod base;
mod jvm;

fn main() {
    let arg = LauncherArg::get();
    // println!("{:#?}", arg);
    jvm_launch(arg);
}

#[cfg(test)]
mod test {
    use std::fs::File;
    use std::io::{Read, Write};
    use jclass::util::class_scan::fast_scan_class;
    use crate::base::common::ENCRYPT_DATA_TAG;
    use crate::util::{aes_util, byte_utils};

    #[test]
    fn class_decrypt_test() {
        let mut f = File::open("D:\\data\\code\\project\\java-guard\\out\\tmp\\antlr-4.13.2-complete\\org\\antlr\\v4\\Tool.class").unwrap();
        let mut class_data_arr = Vec::with_capacity(32 * 1024);
        f.read_to_end(&mut class_data_arr).unwrap();

        match fast_scan_class(&class_data_arr, ENCRYPT_DATA_TAG, false) {
            Ok(Some(info)) if info.specify_attribute.is_some() => {
                let data_range = info.specify_attribute.unwrap();
                let mut en_data = class_data_arr[data_range.start..data_range.end].to_vec();
                let data = match aes_util::decrypt(&mut en_data) {
                    Ok(data) => data,
                    Err(err) => {
                        eprintln!("decrypt class data attribute failed: {}", err);
                        return;
                    }
                };

                let mut new_class_data_bytes = Vec::with_capacity(class_data_arr.len());
                // todo decrypt class
                let mut index = 0;
                let mut copied_index = 0;
                loop {
                    let start = index;
                    index += 2;
                    let const_index = byte_utils::be_byte_to_u16(&data[start..index]) as usize;
                    if const_index != 0 {
                        let const_start = info.consts[const_index - 1] + 1;
                        let const_end = info.consts[const_index];
                        new_class_data_bytes.extend_from_slice(&class_data_arr[copied_index..const_start]);
                        copied_index = const_end;
                    }
                    let len = match data[index] as char {
                        'I' | 'F' => {
                            4
                        }
                        'L' | 'D' => {
                            8
                        }
                        'S' => {
                            let start = index;
                            index += 4;
                            let len = byte_utils::byte_to_u32(&data[(start + 1)..index + 1]);
                            new_class_data_bytes.extend_from_slice(&(len as u16).to_be_bytes());
                            len as usize
                        }
                        _ => {
                            index += 1;
                            break
                        }
                    };
                    index += 1;
                    let start = index;
                    index += len;
                    new_class_data_bytes.extend_from_slice(&data[start..index]);
                }

                let start = index;
                index += 4;
                let x = &data[start..index];
                let codes_size = byte_utils::byte_to_u32(&data[start..index]) as usize;
                for i in 0..codes_size {
                    let start = index;
                    index += 4;
                    let x2 = &data[start..index];
                    let code_len = byte_utils::byte_to_u32(&data[start..index]);
                    let code_range = info.method_codes[i];

                    let mut attr_start = code_range.0 + 2;
                    new_class_data_bytes.extend_from_slice(&class_data_arr[copied_index..attr_start]);
                    let mut code_start = attr_start + 4 + 2 + 2;
                    let mut ori_attr_len = byte_utils::be_byte_to_u32(&class_data_arr[attr_start..attr_start + 4]);
                    let ori_code_len = byte_utils::be_byte_to_u32(&class_data_arr[code_start..code_start + 4]);
                    copied_index = code_start + 4 + (ori_code_len as usize);
                    let start = index;
                    index += code_len as usize;
                    new_class_data_bytes.extend_from_slice(&(ori_attr_len - ori_code_len + code_len).to_be_bytes());
                    new_class_data_bytes.extend_from_slice(&class_data_arr[attr_start+4..code_start]);
                    new_class_data_bytes.extend_from_slice(&code_len.to_be_bytes());
                    new_class_data_bytes.extend_from_slice(&data[start..index]);
                }
                new_class_data_bytes.extend_from_slice(&class_data_arr[copied_index..data_range.start - 4]);
                new_class_data_bytes.extend_from_slice(&1_u32.to_be_bytes());
                new_class_data_bytes.push(0);
                new_class_data_bytes.extend_from_slice(&class_data_arr[data_range.end..]);

                let mut of = File::create("D:\\data\\code\\project\\java-guard\\out\\tmp\\antlr-4.13.2-complete\\org\\antlr\\v4\\Tool.2.class").unwrap();
                of.write_all(&new_class_data_bytes).unwrap();
                // println!("{:?}", new_class_data_bytes);

                let i = fast_scan_class(&new_class_data_bytes, &[], true).unwrap();
                println!("{:?}", i)
            }
            Err(err) => {
                eprintln!("err failed: {}", err);
                return;
            }
            _ => {}
        }
    }
}