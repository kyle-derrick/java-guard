use std::{env, process::exit, sync::OnceLock};

use crate::common::{KEY_VERSION, VERSION};
use crate::jar_info::JarInfo;

const SERVER_ARG_KEY: &str = "-server";
const CP_ARG_KEY: &str = "-cp";
const CLASSPATH_ARG_KEY: &str = "-classpath";
const CLASS_PATH_ARG_KEY: &str = "--class-path";
const JAR_ARG_KEY: &str = "-jar";
const VERSION_ARG_KEY: &str = "-version";
const _VERSION_ARG_KEY: &str = "--version";
const HELP_ARG_KEY: &str = "-help";
const HELP_H_ARG_KEY: &str = "-h";
const HELP_C_ARG_KEY: &str = "-?";

const VERBOSE_ARG_PREFIX: &str = "-verbose:";
const SYSTEM_PROPERTY_ARG_PREFIX: &str = "-D";
const VM_ARG_PREFIX: &str = "-X";

const AGENTLIB_ARG_PREFIX: &str = "-agentlib:";
const AGENTPATH_ARG_PREFIX: &str = "-agentpath:";
const JAVAAGENT_ARG_PREFIX: &str = "-javaagent:";

static LAUNCHER_ARG: OnceLock<LauncherArg> = OnceLock::new();

#[derive(Debug)]
pub enum LaunchTarget {
    Class(String),
    Jar(JarInfo),
}

impl LaunchTarget {
    pub fn sun_mode(&self) -> i32 {
        match self {
            LaunchTarget::Class(_) => 1,
            LaunchTarget::Jar(_) => 2
        }
    }

    pub fn target_path(&self) -> &str {
        match self {
            LaunchTarget::Class(path) => path,
            LaunchTarget::Jar(jar) => jar.path()
        }
    }
}

#[derive(Debug)]
pub struct LauncherArg {
    curr_app_path: String,
    server: bool,
    classpath: Vec<String>,
    vm_args: Vec<String>,
    target: LaunchTarget,
    app_args: Vec<String>,
}

impl LauncherArg {

    pub fn get() -> &'static LauncherArg {
        LAUNCHER_ARG.get_or_init(|| __parse_args())
    }

    pub fn server(&self) -> bool {
        self.server
    }
    pub fn classpath(&self) -> &Vec<String> {
        &self.classpath
    }
    pub fn vm_args(&self) -> &Vec<String> {
        &self.vm_args
    }
    pub fn target(&self) -> &LaunchTarget {
        &self.target
    }
    pub fn app_args(&self) -> &Vec<String> {
        &self.app_args
    }
}

fn usage() -> ! {
    println!(r#"
usage: launcher [options] -jar <jar file> [args...]
   // or  launcher [options] <class> [args...]

   Class not currently supported run class!!!!!!

 options:
    -server
    // [-cp -classpath --class-path] <directory and zip/jar file>
    //               like java -classpath argument
    //               (not currently supported!!!!!!)
    -D<name>=<value>
                  system property
    -verbose:[class|module|gc|jni]
                  enable detail output
    -version
    --version     version info
    -? -h -help
                  print usage
    -X            additional options"#);
    exit(0);
}

fn __parse_args() -> LauncherArg {
    let mut server = false;
    let mut classpath: Vec<_> = Vec::new();
    let mut vm_args: Vec<_> = Vec::new();
    let mut target = None;
    let mut app_args: Vec<_> = Vec::new();
    let mut arg_iter = env::args();
    let curr_app_path = arg_iter.next().unwrap();
    while let Some(arg) = arg_iter.next() {
        match arg.as_str() {
            SERVER_ARG_KEY => {
                server = true;
            },
            CP_ARG_KEY | CLASSPATH_ARG_KEY | CLASS_PATH_ARG_KEY => {
                let classpath_str = arg_iter.next().expect("classpath arg not found");
                env::split_paths(&classpath_str).for_each(|item| {
                    if let Some(item) = item.to_str() {
                        classpath.push(item.to_string())
                    }
                });
                panic!("Not currently supported class path")
            },
            VERSION_ARG_KEY | _VERSION_ARG_KEY => {
                println!("launcher version: {}", VERSION);
                println!("launcher key version: {}", KEY_VERSION);
                exit(0)
            },
            HELP_ARG_KEY | HELP_H_ARG_KEY | HELP_C_ARG_KEY => {
                usage()
            },
            JAR_ARG_KEY => {
                if target.is_none() {
                    target = Some(LaunchTarget::Jar(JarInfo::parse(&arg_iter.next().expect("not set jar file: -jar <jar file>"))))
                } else {
                    app_args.push(arg);
                }
            },
            _ => {
                if arg.starts_with(VERBOSE_ARG_PREFIX) {
                } else if arg.starts_with(AGENTLIB_ARG_PREFIX) ||
                    arg.starts_with(AGENTPATH_ARG_PREFIX) ||
                    arg.starts_with(JAVAAGENT_ARG_PREFIX) {
                    panic!("not allow the agent arg!!!")
                } else if arg.starts_with(SYSTEM_PROPERTY_ARG_PREFIX) {
                    vm_args.push(arg);
                } else if arg.starts_with(VM_ARG_PREFIX) {
                    if arg.eq_ignore_ascii_case("-XX:-DisableAttachMechanism") {
                        continue
                    }
                    vm_args.push(arg);
                } else if target.is_none() {
                    target = Some(LaunchTarget::Class(arg));
                    panic!("Not currently supported run class")
                } else {
                    app_args.push(arg);
                }
            }
        }
    }
    if let Some(target) = target {
        LauncherArg {
            curr_app_path,
            server,
            classpath,
            vm_args,
            target,
            app_args
        }
    } else {
        usage()
    }
}