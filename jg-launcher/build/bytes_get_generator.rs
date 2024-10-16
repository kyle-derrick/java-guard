use rand::rngs::ThreadRng;
use rand::{thread_rng, Rng};
use std::cmp::min;

struct BytesSplitNode {
    value: Vec<u8>,
    index: usize,
    child: Vec<BytesSplitNode>
}
const LAYER_MAX_NODE: usize = 36;

const BYTE_LIST_VARIABLE_NAME: &str = "bytes";
const BYTE_SIGN_VARIABLE_NAME: &str = "sign";
const BYTE_HANDLE_FUNC_NAME: &str = "handle_bytes";

fn generate_code_from_bytes(bytes: &[u8], rng: &mut ThreadRng, sign: u8) -> String {
    if bytes.len() > 5 {
        let signs = (0..rng.gen_range(1..=bytes.len())).map(|_| rng.gen::<u8>()).collect::<Vec<u8>>();
        let mut new_bytes = Vec::with_capacity(bytes.len());
        for (i, b) in bytes.iter().enumerate() {
            new_bytes.push((b^sign^signs[i%signs.len()]).to_string());
        }
        let bs: String = new_bytes.join(",");
        let signs: String = signs.iter().map(u8::to_string).collect::<Vec<String>>().join(",");
        format!("  {BYTE_HANDLE_FUNC_NAME}({BYTE_LIST_VARIABLE_NAME}, &[{bs}], &[{signs}], {BYTE_SIGN_VARIABLE_NAME});")
    //       format!(r#"
    // let bs = [{bs}];
    // let signs = [{signs}];
    // for (i, item) in (&bs).iter().enumerate() {{
    //   {BYTE_LIST_VARIABLE_NAME}.push((|a, b| item ^ a ^ b)({BYTE_SIGN_VARIABLE_NAME}, signs[i%signs.len()]));
    // }}"#)
    } else {
        let mut code_list: Vec<String> = Vec::with_capacity(bytes.len());
        for b in bytes {
            let sub_sign = rng.gen::<u8>();
            let b = b ^ sign ^ sub_sign;
            code_list.push(format!("  {BYTE_LIST_VARIABLE_NAME}.push((|a, b| {b}u8 ^ a ^ b)({BYTE_SIGN_VARIABLE_NAME}, {sub_sign}u8));"))
        }
        code_list.join("\n")
    }
}

fn generate_code_from_node(tree: &BytesSplitNode, rng: &mut ThreadRng, funcs: &mut Vec<String>, prefix: &str, sign: u8) -> String {
    let child = &tree.child;
    let mut code_list = Vec::with_capacity(child.len() + 1);
    for (index, node) in child.iter().enumerate() {
        if index == tree.index {
            code_list.push(generate_code_from_bytes(tree.value.as_slice(), rng, sign));
        }
        let sign = rng.gen::<u8>();
        let name = generate_code_from_node(node, rng, funcs, prefix, sign);
        code_list.push(format!("  {name}({BYTE_LIST_VARIABLE_NAME}, {sign});"));
    }
    if tree.index == child.len() {
        code_list.push(generate_code_from_bytes(tree.value.as_slice(), rng, sign));
    }
    let code_content = code_list.join("\n");
    let seq = funcs.len();
    let name = format!("{prefix}_{seq}");
    funcs.push(format!(r#"
fn {name}({BYTE_LIST_VARIABLE_NAME}: &mut Vec<u8>, sign: u8) {{
  {}
}}"#, &code_content));
    name
}

fn generate_node(bytes: &[u8], rng: &mut ThreadRng) -> BytesSplitNode {
    let len = bytes.len();
    let value_len = if len == 1 {
        1
    } else if len < 4 {
        rng.gen_range(1..=len)
    } else {
        rng.gen_range(1..=len >> 2)
    };
    let remaining_len = len - value_len;
    if remaining_len == 0 {
        return BytesSplitNode {
            value: bytes.to_vec(),
            index: 0,
            child: Vec::new()
        };
    }
    let node_size = if remaining_len == 1 {
        1
    } else {
        rng.gen_range(1..=min(remaining_len, LAYER_MAX_NODE))
    };
    let value_index = rng.gen_range(0..=node_size);
    let mut remaining_items = remaining_len - node_size;
    let mut nodes = Vec::with_capacity(node_size);
    let mut value = bytes;
    let mut curr_index = 0;
    for i in 0..node_size {
        if i == value_index {
            value = &bytes[curr_index..curr_index+value_len];
            curr_index += value_len;
        }
        let sub_len = if i == node_size - 1 {
            1 + remaining_items
        } else if remaining_items > 0 {
            let ext_items = rng.gen_range(0..=remaining_items);
            remaining_items -= ext_items;
            ext_items + 1
        } else {
            1
        };
        let node = generate_node(&bytes[curr_index..curr_index + sub_len], rng);
        curr_index += sub_len;
        nodes.push(node);
    }
    if value_index == node_size {
        value = &bytes[curr_index..len];
    }

    BytesSplitNode {
        value: value.to_vec(),
        index: value_index,
        child: nodes
    }
}
pub fn get_common_func_code() -> String {
    format!(r#"
fn handle_bytes({BYTE_LIST_VARIABLE_NAME}: &mut Vec<u8>, bs: &[u8], signs: &[u8], sign: u8) {{
  for (i, item) in (&bs).iter().enumerate() {{
    {BYTE_LIST_VARIABLE_NAME}.push((|a, b| item ^ a ^ b)({BYTE_SIGN_VARIABLE_NAME}, signs[i%signs.len()]));
  }}
}}"#)
}
pub fn generate_func_code(bytes: &[u8], name: &str) -> Vec<String> {
    let mut rng = thread_rng();
    let len = bytes.len();
    let node = generate_node(bytes, &mut rng);
    let mut func_codes = Vec::new();
    let sign = rng.gen::<u8>();
    let prefix = format!("__{name}");
    let func_name = generate_code_from_node(&node, &mut rng, &mut func_codes, &prefix, sign);
    func_codes.push(format!(r#"
pub fn {name}() -> Vec<u8> {{
  let mut bytes = Vec::with_capacity({len});
  {func_name}(&mut bytes, {sign}u8);
  bytes
}}"#));
    func_codes
}