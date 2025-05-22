#[inline]
pub fn byte_to_u32(bs: &[u8]) -> u32 {
    match bs.len() {
        0 => 0,
        1..4 => {
            let mut bs_new = [0u8; 4];
            bs_new[4 - bs.len()..].copy_from_slice(bs);
            byte_to_u32(&bs_new)
        },
        _ => u32::from_le_bytes(bs.try_into().unwrap())
    }
}

#[inline]
pub fn byte_to_u16(bs: &[u8]) -> u16 {
    match bs.len() {
        0 => 0,
        1 => byte_to_u16(&[0, bs[0]]),
        _ => u16::from_le_bytes(bs.try_into().unwrap())
    }
}

#[inline(always)]
pub fn byte_be_to_u16_fast(data: &[u8], index: usize) -> u16 {
    unsafe {
        let ptr = data.as_ptr().add(index) as *const u16;
        u16::from_be(ptr.read_unaligned())
    }
}

#[inline(always)]
pub fn byte_be_to_u32_fast(data: &[u8], index: usize) -> u32 {
    unsafe {
        let ptr = data.as_ptr().add(index) as *const u32;
        u32::from_be(ptr.read_unaligned())
    }
}