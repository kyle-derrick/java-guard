#![allow(unused)]
use crate::base::common::{KEY_VERSION, VERSION};
use crate::jar_info::JarInfo;
use crate::util::jvm_util::{parse_classpath, print_version};
use std::{env, process, process::exit, sync::OnceLock};
use clap::arg;

const SERVER_ARG_KEY: &str = "-server";
const CP_ARG_KEY: &str = "-cp";
const CLASSPATH_ARG_KEY: &str = "-classpath";
const CLASS_PATH_ARG_KEY: &str = "--class-path";
const JAR_ARG_KEY: &str = "-jar";
const VERSION_ARG_KEY: &str = "-version";
const HELP_ARG_KEY: &str = "-help";
const HELP_H_ARG_KEY: &str = "-h";
const HELP_C_ARG_KEY: &str = "-?";

const VERBOSE_ARG_PREFIX: &str = "-verbose:";
const SYSTEM_PROPERTY_ARG_PREFIX: &str = "-D";
const VM_ARG_PREFIX: &str = "-X";

const AGENTLIB_ARG_PREFIX: &str = "-agentlib:";
const AGENTPATH_ARG_PREFIX: &str = "-agentpath:";
const JAVAAGENT_ARG_PREFIX: &str = "-javaagent:";
const DEBUG_ARG: &str = "-Xdebug";
const RUNJDWP_ARG_PREFIX: &str = "-Xrunjdwp:";
#[allow(unused)]
const NOVERIFY_ARG_PREFIX: &str = "-noverify";
#[allow(unused)]
const NOVERIFY_ARG_FINAL: &str = "-Xverify:none";
const RE_DISABLE_ATTACH_MECHANISM: &str = "-XX:-DisableAttachMechanism";
const DISABLE_ATTACH_MECHANISM: &str = "-XX:+DisableAttachMechanism";
const JAVA_COMMAND_VM_ARG_PREFIX: &str = "-Dsun.java.command=";
const JAVA_LAUNCHER_ARG: &str = "-Dsun.java.launcher=SUN_STANDARD";
const JAVA_LAUNCHER_PID_ARG_PREFIX: &str = "-Dsun.java.launcher.pid=";

static LAUNCHER_ARG: OnceLock<LauncherArg> = OnceLock::new();

#[allow(unused)]
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

    pub fn main_class(&self) -> &str {
        match self {
            LaunchTarget::Class(name) => name,
            LaunchTarget::Jar(jar) => jar.main_class()
        }
    }

    pub fn target_value(&self) -> &str {
        match self {
            LaunchTarget::Class(name) => name,
            LaunchTarget::Jar(jar) => jar.path()
        }
    }

    pub fn is_jar(&self) -> bool {
        matches!(*self, LaunchTarget::Jar(_))
    }
}

#[allow(unused)]
#[derive(Debug)]
pub struct LauncherArg {
    curr_app_path: String,
    server: bool,
    classpath: Option<Vec<String>>,
    vm_args: Vec<String>,
    target: LaunchTarget,
    app_args: Vec<String>,
}

#[allow(unused)]
impl LauncherArg {

    pub fn get() -> &'static LauncherArg {
        LAUNCHER_ARG.get_or_init(|| __parse_args())
    }

    pub fn server(&self) -> bool {
        self.server
    }
    pub fn classpath(&self) -> &Option<Vec<String>> {
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
/*
    println!(r#"
usage: launcher [options] -jar <jar file> [args...]
   // or  launcher [options] <class> [args...]
   // but class must be in jar file

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
 */
    println!(r#"
usage: launcher [options] -jar <jar file> [args...]

   Class not currently supported run class!!!!!!

 options:
    -server
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
    let mut classpath: Option<Vec<_>> = None;
    let mut vm_args: Vec<_> = Vec::new();
    let mut target = None;
    let mut app_args: Vec<_> = Vec::new();
    let mut arg_iter = env::args();
    let curr_app_path = arg_iter.next().unwrap();

    while let Some(arg) = arg_iter.next() {
        if target.is_none() {
            match arg.as_str() {
                SERVER_ARG_KEY => {
                    server = true;
                },
                CP_ARG_KEY | CLASSPATH_ARG_KEY | CLASS_PATH_ARG_KEY => {
                    let classpath_str = arg_iter.next().expect("classpath arg not found");
                    classpath = Some(parse_classpath(&classpath_str));

                    #[cfg(not(feature = "dev"))]
                    panic!("Not currently supported class path")
                },
                VERSION_ARG_KEY => {
                    print_version();
                    println!("launcher version: {}", VERSION);
                    println!("launcher key version: {}", KEY_VERSION);
                    exit(0)
                },
                HELP_ARG_KEY | HELP_H_ARG_KEY | HELP_C_ARG_KEY => {
                    usage()
                },
                JAR_ARG_KEY => {
                    let jar_info = JarInfo::parse(&arg_iter.next().expect("not set jar file: -jar <jar file>"));
                    #[cfg(not(feature = "dev"))]
                    jar_info.verify();
                    target = Some(LaunchTarget::Jar(jar_info))
                },
                _ => {
                    if arg.starts_with(VERBOSE_ARG_PREFIX) {
                        continue
                    } else if arg.starts_with(SYSTEM_PROPERTY_ARG_PREFIX) {
                        #[cfg(not(feature = "dev"))]
                        if arg.starts_with("-Djava.class.path") {
                            continue;
                        }
                    } else if arg.eq_ignore_ascii_case(RE_DISABLE_ATTACH_MECHANISM) ||
                            arg.eq_ignore_ascii_case(DEBUG_ARG) ||
                            arg.starts_with(RUNJDWP_ARG_PREFIX) {
                        #[cfg(not(feature = "dev"))]
                        continue;
                    } else if arg.eq(NOVERIFY_ARG_PREFIX) {
                        #[cfg(feature = "dev")]
                        vm_args.push(NOVERIFY_ARG_FINAL.to_string());
                        continue;
                    } else if arg.starts_with(AGENTLIB_ARG_PREFIX) ||
                            arg.starts_with(AGENTPATH_ARG_PREFIX) ||
                            arg.starts_with(JAVAAGENT_ARG_PREFIX) {
                        #[cfg(not(feature = "dev"))]
                        panic!("not allow the agent arg!!!");
                    } else if arg.starts_with('-') {
                        if !arg.starts_with(VM_ARG_PREFIX) {
                            panic!("not support vm arg: {arg}");
                        }
                    } else if target.is_none() {
                        // todo 待定
                        target = Some(LaunchTarget::Class(arg));
                        continue;
                        // panic!("Not currently supported run class")
                    }
                    vm_args.push(arg);
                }
            }
        } else {
            app_args.push(arg);
        }
    }
    if let Some(target) = target {
        init_launcher(&target, &mut vm_args, &app_args);
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


fn init_launcher(target: &LaunchTarget, vm_args: &mut Vec<String>, app_args: &Vec<String>) {
    #[cfg(windows)]
    {
        // 初始化 INITCOMMONCONTROLSEX 结构体
        let mut init_ctrls = winapi::um::commctrl::INITCOMMONCONTROLSEX {
            dwSize: std::mem::size_of::<winapi::um::commctrl::INITCOMMONCONTROLSEX>() as u32, // 结构体大小，必须设置
            dwICC: winapi::um::commctrl::ICC_WIN95_CLASSES, // 指定需要初始化的控件类别
        };

        // 调用 InitCommonControlsEx
        let result = unsafe { winapi::um::commctrl::InitCommonControlsEx(&mut init_ctrls) };

        if result == 0 {
            eprintln!("InitCommonControlsEx failed!");
            // 处理初始化失败的情况，例如获取错误码等
        }
    }

    let name = match target {
        LaunchTarget::Class(class) => class,
        LaunchTarget::Jar(jar) => jar.path()
    };
    vm_args.push(format!("{}{} {}", JAVA_COMMAND_VM_ARG_PREFIX, name, app_args.join(" ")));
    vm_args.push(JAVA_LAUNCHER_ARG.to_string());
    vm_args.push(format!("{}{}", JAVA_LAUNCHER_PID_ARG_PREFIX, process::id()));
    vm_args.push(DISABLE_ATTACH_MECHANISM.to_string());
}