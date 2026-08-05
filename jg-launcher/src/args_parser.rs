#![allow(unused)]
use crate::base::common::{KEY_VERSION, VERSION};
use crate::jar_info::JarInfo;
use crate::util::cfg_parser;
use crate::util::jvm_util::{parse_classpath, print_version};
use std::collections::HashSet;
use std::fs::OpenOptions;
use std::io::Write;
use std::{env, process, process::exit, sync::OnceLock};

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
            LaunchTarget::Jar(_) => 2,
        }
    }

    pub fn main_class(&self) -> &str {
        match self {
            LaunchTarget::Class(name) => name,
            LaunchTarget::Jar(jar) => jar.main_class(),
        }
    }

    pub fn target_value(&self) -> &str {
        match self {
            LaunchTarget::Class(name) => name,
            LaunchTarget::Jar(jar) => jar.path(),
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
        LAUNCHER_ARG.get_or_init(__parse_args)
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
                      enable detailed output
        -version
        --version     version info
        -? -h -help
                      print usage
        -X            additional options"#);
     */
    println!(
        r#"
usage: jg-launcher [options] -jar <jar file> [args...]

 options:
    -server
    -D<name>=<value>
                  system property
    -X            additional options

    @argument file
                  one or more argument files containing options (Java 9+)
    --disable-@files
                  prevent further expansion of argument files (Java 9+)

    -verbose:[class|module|gc|jni]
                  enable detailed output
    -version    java version info
    -jgv        jg version info
    -? -h -help
                  print usage
    "#
    );
    #[cfg(not(feature = "dev"))]
    println!(
        r#"
note: launching a class directly is not currently supported."#
    );
    #[cfg(feature = "dev")]
    println!(
        r#"
    -p <module path>
    --module-path <module path>...
                  a path-separated list of elements, each containing
                  a module or a directory of modules. Each module is
                  a modular JAR or an exploded module directory. (Java 9+)
    --upgrade-module-path <module path>...
                  a path-separated list of elements, each containing
                  a module or a directory of modules that replace
                  upgradeable modules in the runtime image. Each module is
                  a modular JAR or an exploded module directory. (Java 9+)
    --add-modules <module name>[,<module name>...]
                  root modules to resolve in addition to the initial module. (Java 9+)
                  <module name> may also be ALL-DEFAULT, ALL-SYSTEM,
                  or ALL-MODULE-PATH.
    --enable-native-access <module name>[,<module name>...]
                  modules permitted to perform restricted native operations. (Java 21+)
                  <module name> may also be ALL-UNNAMED.
    --list-modules
                  list observable modules and exit (Java 9+)
    -d <module name>
    --describe-module <module name>
                  describe a module and exit (Java 9+)
    --validate-modules
                  validate all modules and exit (Java 9+)
                  this option can help find conflicts and other errors
                  among modules on the module path.
    --show-module-resolution
                  show module resolution output during startup (Java 9+)
    -agentlib:<library name>[=<options>]
                  load the native agent library <library name>, for example -agentlib:jdwp
                  see also -agentlib:jdwp=help
    -agentpath:<path name>[=<options>]
                  load the native agent library specified by its full path name
    -javaagent:<jar path>[=<options>]
                  load a Java programming language agent; see java.lang.instrument
    -ea[:<package name>...|:<class name>]
    -enableassertions[:<package name>...|:<class name>]
                  enable assertions with the specified granularity
    -da[:<package name>...|:<class name>]
    -disableassertions[:<package name>...|:<class name>]
                  disable assertions with the specified granularity
    -esa | -enablesystemassertions
                  enable system assertions
    -dsa | -disablesystemassertions
                  disable system assertions"#
    );
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
            app_args: context.app_args,
        }
    } else {
        usage()
    }
}

fn __parse_args_item<I>(arg: String, arg_iter: &mut I, context: &mut ParseArgsContext)
where
    I: Iterator<Item = String>,
{
    if context.target.is_none() {
        match arg.as_str() {
            SERVER_ARG_KEY => {
                context.server = true;
            }
            CP_ARG_KEY | CLASSPATH_ARG_KEY | CLASS_PATH_ARG_KEY => {
                let classpath_str = arg_iter.next().expect("classpath arg not found");
                context.classpath = Some(parse_classpath(&classpath_str));

                #[cfg(not(feature = "dev"))]
                panic!("Not currently supported class path")
            }
            VERSION_ARG_KEY => {
                print_version(false);
                exit(0)
            }
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
            }
            HELP_ARG_KEY | HELP_H_ARG_KEY | HELP_C_ARG_KEY => usage(),
            JAR_ARG_KEY => {
                let jar_info =
                    JarInfo::parse(&arg_iter.next().expect("not set jar file: -jar <jar file>"));
                #[cfg(not(feature = "dev"))]
                jar_info.verify();
                context.target = Some(LaunchTarget::Jar(jar_info))
            }
            MODULES_PATH_ARG_KEY
            | MODULES_PATH_SHORT_ARG_KEY
            | ADD_MODULES_ARG_KEY
            | ENABLE_NATIVE_ACCESS_ARG_KEY
            | UPGRADE_MODULE_PATH_ARG_KEY
            | DESCRIBE_MODULE_ARG_KEY
            | DESCRIBE_MODULE_SHORT_ARG_KEY
            | ADD_OPENS_ARG_KEY => {
                context.vm_args.push(
                    [
                        arg.as_str(),
                        &arg_iter
                            .next()
                            .unwrap_or_else(|| panic!("arg {arg} not found value")),
                    ]
                    .join("="),
                );

                #[cfg(not(feature = "dev"))]
                panic!("Not currently supported {}", arg)
            }
            LIST_MODULES_ARG_KEY | VALIDATE_MODULES_ARG_KEY | SHOW_MODULE_RESOLUTION_ARG_KEY => {
                context.vm_args.push(arg.clone());

                #[cfg(not(feature = "dev"))]
                panic!("Not currently supported {}", arg)
            }
            EA_ARG_KEY
            | ENABLEASSERTIONS_ARG_KEY
            | DA_ARG_KEY
            | DISABLEASSERTIONS_ARG_KEY
            | ESA_ARG_KEY
            | ENABLESYSTEMASSERTIONS_ARG_KEY
            | DSA_ARG_KEY
            | DISABLESYSTEMASSERTIONS_ARG_KEY => {
                context.vm_args.push(arg.clone());

                #[cfg(not(feature = "dev"))]
                panic!("Not currently supported {}", arg)
            }
            DISABLE_CFG_FILES_ARG_KEY => {
                context.can_load_cfg_file = false;
            }
            _ => {
                if arg.starts_with(ADD_OPENS_PREFIX_ARG_KEY)
                    || arg.starts_with(EA_ARG_PREFIX)
                    || arg.starts_with(ENABLEASSERTIONS_ARG_PREFIX)
                    || arg.starts_with(DA_ARG_PREFIX)
                    || arg.starts_with(DISABLEASSERTIONS_ARG_PREFIX)
                {
                    #[cfg(not(feature = "dev"))]
                    panic!("Not currently supported {}", arg)
                } else if context.can_load_cfg_file && arg.starts_with(CFG_FILE_PREFIX) {
                    let cfg_name = &arg[1..];
                    if !context.cfg_files.contains(cfg_name) {
                        context.cfg_files.insert(cfg_name.to_string());
                        let cfg_vm_args =
                            cfg_parser::parser(&arg[1..]).expect("Failed to parse cfg file");
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
                    // 非开发构建自行建立受保护的类路径，忽略外部覆盖。 / Non-dev builds construct the protected classpath, so external overrides are ignored.
                    if arg.starts_with("-Djava.class.path") {
                        return;
                    }
                } else if arg.eq_ignore_ascii_case(RE_DISABLE_ATTACH_MECHANISM)
                    || arg.eq_ignore_ascii_case(DEBUG_ARG)
                    || arg.starts_with(RUNJDWP_ARG_PREFIX)
                {
                    #[cfg(not(feature = "dev"))]
                    // 非开发构建过滤重新附加和调试选项，避免绕过启动器限制。 / Non-dev builds filter re-attach and debug options to prevent bypassing launcher restrictions.
                    return;
                } else if arg.eq(NOVERIFY_ARG_PREFIX) {
                    #[cfg(feature = "dev")]
                    context.vm_args.push(NOVERIFY_ARG_FINAL.to_string());
                    return;
                } else if arg.starts_with(AGENTLIB_ARG_PREFIX)
                    || arg.starts_with(AGENTPATH_ARG_PREFIX)
                    || arg.starts_with(JAVAAGENT_ARG_PREFIX)
                {
                    #[cfg(not(feature = "dev"))]
                    // 非开发构建拒绝代理注入，避免修改受保护类。 / Non-dev builds reject agent injection to prevent protected classes from being modified.
                    panic!("agent options are not allowed");
                } else if arg.starts_with('-') {
                    if !arg.starts_with(VM_ARG_PREFIX) {
                        panic!("not support vm arg: {arg}");
                    }
                } else if context.target.is_none() {
                    // TODO: 确定类目标的最终支持策略。 / Decide the final support policy for class targets.
                    context.target = Some(LaunchTarget::Class(arg));
                    return;
                    // panic!("Not currently supported run class")
                }
                context.vm_args.push(arg.clone());
            }
        }
    } else {
        context.app_args.push(arg.clone());
    }
}

fn init_launcher(target: &LaunchTarget, vm_args: &mut Vec<String>, app_args: &[String]) {
    #[cfg(windows)]
    {
        // 初始化通用控件配置。 / Initialize the common-controls configuration.
        let init_ctrls = winapi::um::commctrl::INITCOMMONCONTROLSEX {
            dwSize: std::mem::size_of::<winapi::um::commctrl::INITCOMMONCONTROLSEX>() as u32, // 必须设置结构体大小。 / The structure size is required.
            dwICC: winapi::um::commctrl::ICC_WIN95_CLASSES, // 指定要初始化的控件类别。 / Select the control classes to initialize.
        };

        // 初始化 Windows 通用控件。 / Initialize Windows common controls.
        let result = unsafe { winapi::um::commctrl::InitCommonControlsEx(&init_ctrls) };

        if result == 0 {
            eprintln!("ERROR: InitCommonControlsEx failed!");
            // 初始化失败时仅报告错误；启动行为保持不变。 / Report initialization failure only; launcher behavior remains unchanged.
        }
    }

    let name = match target {
        LaunchTarget::Class(class) => class,
        LaunchTarget::Jar(jar) => jar.path(),
    };
    vm_args.push(format!(
        "{}{} {}",
        JAVA_COMMAND_VM_ARG_PREFIX,
        name,
        app_args.join(" ")
    ));
    vm_args.push(JAVA_LAUNCHER_ARG.to_string());
    vm_args.push(format!("{}{}", JAVA_LAUNCHER_PID_ARG_PREFIX, process::id()));
    #[cfg(not(feature = "dev"))]
    vm_args.push(DISABLE_ATTACH_MECHANISM.to_string());
}
