use crate::base::error::MessageError;
use crate::jvm::jvm_launcher::jvm_launch;
use args_parser::LauncherArg;
use std::process::ExitCode;

mod args_parser;
mod base;
mod jar_info;
mod jvm;
mod util;

#[cfg(windows)]
#[global_allocator]
static GLOBAL_ALLOC: mimalloc::MiMalloc = mimalloc::MiMalloc;

// Linux/Unix 平台使用 jemalloc。 / Use jemalloc on Linux/Unix.
#[cfg(unix)]
#[global_allocator]
static GLOBAL_ALLOC: tikv_jemallocator::Jemalloc = tikv_jemallocator::Jemalloc;

fn exit_code(result: Result<(), MessageError>) -> ExitCode {
    match result {
        Ok(()) => ExitCode::SUCCESS,
        Err(error) => {
            eprintln!("{error}");
            ExitCode::FAILURE
        }
    }
}

fn main() -> ExitCode {
    let arg = LauncherArg::get();
    // println!("{:#?}", arg);
    exit_code(jvm_launch(arg))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn maps_launch_result_to_exit_code() {
        assert_eq!(exit_code(Ok(())), ExitCode::SUCCESS);
        assert_eq!(
            exit_code(Err(MessageError::new("launch failed"))),
            ExitCode::FAILURE
        );
    }
}
