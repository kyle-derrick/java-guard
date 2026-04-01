use crate::jvm::jvm_launcher::jvm_launch;
use args_parser::LauncherArg;

mod args_parser;
mod jar_info;
mod util;
mod base;
mod jvm;

#[cfg(windows)]
#[global_allocator]
static GLOBAL_ALLOC: mimalloc::MiMalloc = mimalloc::MiMalloc;

// Linux/Unix平台使用jemalloc
#[cfg(unix)]
#[global_allocator]
static GLOBAL_ALLOC: tikv_jemallocator::Jemalloc = tikv_jemallocator::Jemalloc;

fn main() {
    let arg = LauncherArg::get();
    // println!("{:#?}", arg);
    jvm_launch(arg);
}