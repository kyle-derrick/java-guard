pub fn java_byte_to_u32(bs: [u8;4]) -> u32 {
    // bs.reverse();
    unsafe {
        let np = bs.as_ptr() as *const u32;
        *np
    }
}

pub fn byte_to_u32(bs: &[u8]) -> u32 {
    match bs.len() {
        0 => 0,
        1..4 => {
            let mut bs_new = [0u8, 4];
            bs_new[..bs.len()].copy_from_slice(bs);
            byte_to_u32(&bs_new)
        },
        _ => unsafe {
            let np = bs.as_ptr() as *const u32;
            *np
        }
    }
}

pub fn byte_to_u16(bs: &[u8]) -> u16 {
    match bs.len() {
        0 => 0,
        1 => byte_to_u16(&[bs[0], 0]),
        _ => unsafe {
            let np = bs.as_ptr() as *const u16;
            *np
        }
    }
}