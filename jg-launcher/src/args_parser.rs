#![allow(unused)]
use crate::base::common::{KEY_VERSION, VERSION};
use crate::jar_info::JarInfo;
use crate::util::jvm_util::{parse_classpath, print_version};
use std::fs::OpenOptions;
use std::io::Write;
use std::{env, process, process::exit, sync::OnceLock};
use std::collections::HashSet;
use crate::util::cfg_parser;

const SERVER_ARG_KEY: &str = "-server";
const CP_ARG_KEY: &str = "-cp";
const CLASSPATH_ARG_KEY: &str = "-classpath";
const CLASS_PATH_ARG_KEY: &str = "--class-path";
const MODULES_PATH_ARG_KEY: &str = "--module-path";
const MODULES_PATH_SHORT_ARG_KEY: &str = "-p";
const ADD_MODULES_ARG_KEY: &str = "--add-modules";
const ENABLE_NATIVE_ACCESS_ARG_KEY: &str = "--enable-native-access";
const UPGRADE_MODULE_PATH_ARG_KEY: &str = "--upgrade-module-path";
const ADD_OPENS_ARG_KEY: &str = "--add-opens";
const ADD_OPENS_PREFIX_ARG_KEY: &str = "--add-opens=";
const LIST_MODULES_ARG_KEY: &str = "--list-modules";
const DESCRIBE_MODULE_SHORT_ARG_KEY: &str = "-d";
const DESCRIBE_MODULE_ARG_KEY: &str = "--describe-module";
const VALIDATE_MODULES_ARG_KEY: &str = "--validate-modules";
const SHOW_MODULE_RESOLUTION_ARG_KEY: &str = "--show-module-resolution";
const DISABLE_CFG_FILES_ARG_KEY: &str = "--disable-@files";
const JAR_ARG_KEY: &str = "-jar";
const VERSION_ARG_KEY: &str = "-version";
const FULL_VERSION_ARG_KEY: &str = "-fullversion";
const JG_VERSION_ARG_KEY: &str = "-jgv";
const HELP_ARG_KEY: &str = "-help";
const HELP_H_ARG_KEY: &str = "-h";
const HELP_C_ARG_KEY: &str = "-?";

const VERBOSE_ARG_PREFIX: &str = "-verbose:";
const SYSTEM_PROPERTY_ARG_PREFIX: &str = "-D";
const VM_ARG_PREFIX: &str = "-X";
const CFG_FILE_PREFIX: &str = "@";

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

const EA_ARG_KEY: &str = "-ea";
const EA_ARG_PREFIX: &str = "-ea:";
const ENABLEASSERTIONS_ARG_KEY: &str = "-enableassertions";
const ENABLEASSERTIONS_ARG_PREFIX: &str = "-enableassertions:";
const DA_ARG_KEY: &str = "-da";
const DA_ARG_PREFIX: &str = "-da:";
const DISABLEASSERTIONS_ARG_KEY: &str = "-disableassertions";
const DISABLEASSERTIONS_ARG_PREFIX: &str = "-disableassertions:";
const ESA_ARG_KEY: &str = "-esa";
const ENABLESYSTEMASSERTIONS_ARG_KEY: &str = "-enablesystemassertions";
const DSA_ARG_KEY: &str = "-dsa";
const DISABLESYSTEMASSERTIONS_ARG_KEY: &str = "-disablesystemassertions";

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
    app_execute_cmd: String,
    server: bool,
    classpath: Option<Vec<String>>,
    vm_args: Vec<String>,
    target: LaunchTarget,
    app_args: Vec<String>,
}

#[derive(Debug)]
struct ParseArgsContext {
    app_args: Vec<String>,
    vm_args: Vec<String>,
    classpath: Option<Vec<String>>,
    server: bool,
    can_load_cfg_file: bool,
    cfg_files: HashSet<String>,
    target: Option<LaunchTarget>,
}

#[allow(unused)]
impl LauncherArg {

    pub fn get() -> &'static LauncherArg {
        LAUNCHER_ARG.get_or_init(|| __parse_args())
    }

    #[allow(unused)]
    pub fn app_execute_cmd(&self) -> &str {
        &self.app_execute_cmd
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
usage: jg-launcher [options] -jar <jar file> [args...]
   // or  jg-launcher [options] <class> [args...]
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
usage: jg-launcher [options] -jar <jar file> [args...]

 options:
    -server
    -D<name>=<value>
                  system property
    -X            additional options

    @argument 文件
                  一个或多个包含选项的参数文件（Java9+）
    --disable-@files
                  阻止进一步扩展参数文件（Java9+）

    -verbose:[class|module|gc|jni]
                  enable detail output
    -version    java version info
    -jgv        jg version info
    -? -h -help
                  print usage
    "#);
    #[cfg(not(feature = "dev"))]
    println!(r#"
note: Class not currently supported run class!!!!!!"#);
    #[cfg(feature = "dev")]
    println!(r#"
    -p <模块路径>
    --module-path <模块路径>...
                  ; 分隔的元素列表，每个元素都是
                  模块或包含模块的目录的文件路径。每个模块都是
                  模块化 JAR 或展开的模块目录。（Java9+）
    --upgrade-module-path <模块路径>...
                  ; 分隔的元素列表，每个元素都是
                  模块或包含模块（用于替换运行时映像中的
                  可升级模块）的目录的文件路径。每个模块都是
                  模块化 JAR 或展开的模块目录。（Java9+）
    --add-modules <模块名称>[,<模块名称>...]
                  除了初始模块之外要解析的根模块。（Java9+）
                  <模块名称> 还可以为 ALL-DEFAULT, ALL-SYSTEM,
                  ALL-MODULE-PATH.
    --enable-native-access <module name>[,<module name>...]
                  允许执行受限本机操作的模块。（Java21+）
                  <module name> 还可以为 ALL-UNNAMED。
    --list-modules
                  列出可观察模块并退出（Java9+）
    -d <module name>
    --describe-module <模块名称>
                  描述模块并退出（Java9+）
    --validate-modules
                  验证所有模块并退出（Java9+）
                  --validate-modules 选项对于查找
                  模块路径中模块的冲突及其他错误可能非常有用。
    --show-module-resolution
                  在启动过程中显示模块解析输出（Java9+）
    -agentlib:<库名>[=<选项>]
                  加载本机代理库 <库名>, 例如 -agentlib:jdwp
                  另请参阅 -agentlib:jdwp=help
    -agentpath:<路径名>[=<选项>]
                  按完整路径名加载本机代理库
    -javaagent:<jar 路径>[=<选项>]
                  加载 Java 编程语言代理, 请参阅 java.lang.instrument
    -ea[:<程序包名称>...|:<类名>]
    -enableassertions[:<程序包名称>...|:<类名>]
                  按指定的粒度启用断言
    -da[:<程序包名称>...|:<类名>]
    -disableassertions[:<程序包名称>...|:<类名>]
                  按指定的粒度禁用断言
    -esa | -enablesystemassertions
                  启用系统断言
    -dsa | -disablesystemassertions
                  禁用系统断言"#);
    exit(0);
}

fn __parse_args() -> LauncherArg {
    let mut app_args: Vec<_> = Vec::new();
    let mut arg_iter = env::args();
    // let args: Vec<String> = arg_iter.collect();
    // let mut log_file = OpenOptions::new().create(true).append(true).open("D:\\test.out")
    //     .unwrap();
    // log_file.write_all(args.join(" ").as_bytes());
    // let mut arg_iter = args.into_iter();
    let app_execute_cmd = arg_iter.next().unwrap();
    let mut context = ParseArgsContext {
        app_args: Vec::new(),
        vm_args: Vec::new(),
        classpath: None,
        server: false,
        can_load_cfg_file: true,
        cfg_files: HashSet::new(),
        target: None,
    };
    while let Some(arg) = arg_iter.next() {
        __parse_args_item(arg, &mut arg_iter, &mut context);
    }
    if let Some(target) = context.target {
        init_launcher(&target, &mut context.vm_args, &app_args);
        LauncherArg {
            app_execute_cmd,
            server: context.server,
            classpath: context.classpath,
            vm_args: context.vm_args,
            target,
            app_args: context.app_args
        }
    } else {
        usage()
    }
}

fn __parse_args_item<I>(arg: String, arg_iter: &mut I, context: &mut ParseArgsContext) where I: Iterator<Item=String>  {
        if context.target.is_none() {
            match arg.as_str() {
                SERVER_ARG_KEY => {
                    context.server = true;
                },
                CP_ARG_KEY | CLASSPATH_ARG_KEY | CLASS_PATH_ARG_KEY => {
                    let classpath_str = arg_iter.next().expect("classpath arg not found");
                    context.classpath = Some(parse_classpath(&classpath_str));

                    #[cfg(not(feature = "dev"))]
                    panic!("Not currently supported class path")
                },
                VERSION_ARG_KEY => {
                    print_version(false);
                    exit(0)
                },
                FULL_VERSION_ARG_KEY => {
                    print_version(true);
                    exit(0)
                }
                JG_VERSION_ARG_KEY => {
                    println!("jg-launcher version: {}", VERSION);
                    println!("jg-launcher key version: {}", KEY_VERSION);
                    #[cfg(feature = "dev")]
                    println!("jg-launcher is [dev] version!");
                    exit(0)
                },
                HELP_ARG_KEY | HELP_H_ARG_KEY | HELP_C_ARG_KEY => {
                    usage()
                },
                JAR_ARG_KEY => {
                    let jar_info = JarInfo::parse(&arg_iter.next().expect("not set jar file: -jar <jar file>"));
                    #[cfg(not(feature = "dev"))]
                    jar_info.verify();
                    context.target = Some(LaunchTarget::Jar(jar_info))
                },
                MODULES_PATH_ARG_KEY | MODULES_PATH_SHORT_ARG_KEY | ADD_MODULES_ARG_KEY |
                ENABLE_NATIVE_ACCESS_ARG_KEY | UPGRADE_MODULE_PATH_ARG_KEY |
                DESCRIBE_MODULE_ARG_KEY | DESCRIBE_MODULE_SHORT_ARG_KEY | ADD_OPENS_ARG_KEY => {
                    context.vm_args.push([arg.as_str(), &arg_iter.next().expect(&format!("arg {} not found value", arg))].join("="));

                    #[cfg(not(feature = "dev"))]
                    panic!("Not currently supported {}", arg)
                },
                LIST_MODULES_ARG_KEY | VALIDATE_MODULES_ARG_KEY | SHOW_MODULE_RESOLUTION_ARG_KEY => {
                    context.vm_args.push(arg.clone());

                    #[cfg(not(feature = "dev"))]
                    panic!("Not currently supported {}", arg)
                }
                EA_ARG_KEY | ENABLEASSERTIONS_ARG_KEY | DA_ARG_KEY | DISABLEASSERTIONS_ARG_KEY |
                ESA_ARG_KEY | ENABLESYSTEMASSERTIONS_ARG_KEY | DSA_ARG_KEY | DISABLESYSTEMASSERTIONS_ARG_KEY => {
                    context.vm_args.push(arg.clone());

                    #[cfg(not(feature = "dev"))]
                    panic!("Not currently supported {}", arg)
                }
                DISABLE_CFG_FILES_ARG_KEY => {
                    context.can_load_cfg_file = false;
                }
                _ => {
                    if arg.starts_with(ADD_OPENS_PREFIX_ARG_KEY) {
                        #[cfg(not(feature = "dev"))]
                        panic!("Not currently supported {}", arg)
                    } else if arg.starts_with(EA_ARG_PREFIX) || arg.starts_with(ENABLEASSERTIONS_ARG_PREFIX) ||
                        arg.starts_with(DA_ARG_PREFIX) || arg.starts_with(DISABLEASSERTIONS_ARG_PREFIX) {
                        #[cfg(not(feature = "dev"))]
                        panic!("Not currently supported {}", arg)
                    } else if context.can_load_cfg_file && arg.starts_with(CFG_FILE_PREFIX) {
                        let cfg_name = &arg[1..];
                        if !context.cfg_files.contains(cfg_name) {
                            context.cfg_files.insert(cfg_name.to_string());
                            let cfg_vm_args = cfg_parser::parser(&arg[1..]).expect("Failed to parse cfg file");
                            // println!("parsed cfg: {:?}", cfg_vm_args);
                            let mut cfg_vm_args_iter = cfg_vm_args.into_iter();
                            while let Some(item) = cfg_vm_args_iter.next() {
                                __parse_args_item(item, &mut cfg_vm_args_iter, context);
                            }
                        }
                        return;
                    } else if arg.starts_with(VERBOSE_ARG_PREFIX) {
                        return;
                    } else if arg.starts_with(SYSTEM_PROPERTY_ARG_PREFIX) {
                        #[cfg(not(feature = "dev"))]
                        if arg.starts_with("-Djava.class.path") {
                            return;
                        }
                    } else if arg.eq_ignore_ascii_case(RE_DISABLE_ATTACH_MECHANISM) ||
                        arg.eq_ignore_ascii_case(DEBUG_ARG) ||
                        arg.starts_with(RUNJDWP_ARG_PREFIX) {
                        #[cfg(not(feature = "dev"))]
                        return;
                    } else if arg.eq(NOVERIFY_ARG_PREFIX) {
                        #[cfg(feature = "dev")]
                        context.vm_args.push(NOVERIFY_ARG_FINAL.to_string());
                        return;;
                    } else if arg.starts_with(AGENTLIB_ARG_PREFIX) ||
                        arg.starts_with(AGENTPATH_ARG_PREFIX) ||
                        arg.starts_with(JAVAAGENT_ARG_PREFIX) {
                        #[cfg(not(feature = "dev"))]
                        panic!("not allow the agent arg!!!");
                    } else if arg.starts_with('-') {
                        if !arg.starts_with(VM_ARG_PREFIX) {
                            panic!("not support vm arg: {arg}");
                        }
                    } else if context.target.is_none() {
                        // todo 待定
                        context.target = Some(LaunchTarget::Class(arg));
                        return;;
                        // panic!("Not currently supported run class")
                    }
                    context.vm_args.push(arg.clone());
                }
            }
        } else {
            context.app_args.push(arg.clone());
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
    #[cfg(not(feature = "dev"))]
    vm_args.push(DISABLE_ATTACH_MECHANISM.to_string());
}