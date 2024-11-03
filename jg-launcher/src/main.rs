use args_parser::LauncherArg;
use crate::jvm::jvm_launcher::jvm_launch;

mod args_parser;
mod jar_info;
mod util;
mod common;
mod jvm;

fn main() {
    let arg = LauncherArg::get();
    println!("{:#?}", arg);
    jvm_launch(arg);
}