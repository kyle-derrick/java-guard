use crate::args_parser::LaunchTarget::Jar;
use crate::args_parser::LauncherArg;
use crate::base::error::MessageError;
use crate::jvm::jvmti_init::{load_ext_runtime, set_callbacks};
use crate::jvm::launcher_helper::{find_launcher_helper_from_env, JvmLauncherHelper};
use crate::util::jvm_util::{destroy_vm, JvmWrapper};
use crate::{jni_result_expect, with_message};
use jni::objects::{JClass, JObject};
use jni::sys::jsize;
use jni::JNIEnv;
use jni::JNIVersion;
#[cfg(feature = "dev")]
use {
    crate::args_parser::LaunchTarget,
    crate::util::jvm_util::parse_classpath,
    std::env,
    std::iter,
};

const JAVA_CLASS_PATH_VM_ARG_PREFIX: &str = "-Djava.class.path=";

pub fn jvm_launch(launcher_arg: &LauncherArg) {
    let vm_ops = launcher_arg.vm_args();

    let mut args_builder = jni::InitArgsBuilder::new()
        .version(JNIVersion::V8);
    if launcher_arg.server() {
        args_builder = args_builder.option("-server");
    }
    let target = launcher_arg.target();
    let mut java_class_path = String::from(JAVA_CLASS_PATH_VM_ARG_PREFIX);
    #[cfg(not(feature = "dev"))]
    if let Jar(jar) = &target {
        java_class_path.push_str(jar.path());
        // args_builder = args_builder.option(&java_class_path);
        // (jar.main_class().replace('.', "/"), jar.signature())
    } else {
        // todo not currently supported
        panic!("not currently supported run class")
    };
    #[cfg(feature = "dev")]
    match launcher_arg.classpath() {
        Some(classpath) => {
            let cp = match &target {
                LaunchTarget::Class(class) => {
                    env::join_paths(classpath)
                }
                Jar(jar) => {
                    env::join_paths(classpath.iter().chain(iter::once(jar.path())))
                }
            }.expect("failed to handle class path!");
            let cp_str = cp.to_str().expect("failed to handle class path!");
            java_class_path.push_str(cp_str);
        }
        None => {
            match &target {
                LaunchTarget::Class(class) => {
                    if let Ok(cp) = env::var("CLASSPATH") {
                        let cp = env::join_paths(parse_classpath(&cp)).expect("failed to handle class path!");
                        java_class_path.push_str(cp.to_str().expect("failed to handle class path!"));
                    }
                }
                Jar(jar) => {
                    java_class_path.push_str(jar.path());
                }
            }
        }
    }
    for item in vm_ops.iter() {
        args_builder = args_builder.option(item.trim());
    };
    let init_args = args_builder
        .option(&java_class_path)
        .build()
        .expect("init Jvm args failed");

    // let jvm = JavaVM::new(init_args).unwrap();
    // let jvm = JavaVM::with_libjvm(init_args,
    //                               || StartJvmResult::Ok(PathBuf::from("D:\\software\\install\\Java\\jdk1.8.0_202\\jre\\bin\\server\\jvm.dll"))).unwrap();

    let wrapper = JvmWrapper::load_jvm().expect("failed to load Java VM!");
    // let wrapper = JvmWrapper::load_jvm_with("D:\\software\\install\\Java\\jdk1.8.0_202\\jre\\bin\\server\\jvm.dll").unwrap();


    let (jvm, mut env) = wrapper.create_java_vm(init_args).expect("failed to create Java VM!");
    let env_ref = &mut env;

    if let Err(e) = run(launcher_arg, &jvm, env_ref) {
        eprintln!("{}", e);
    }

    destroy_vm(&jvm, env_ref);
}

fn run(launcher_arg: &LauncherArg, jvm: &jni::JavaVM, env_ref: &mut JNIEnv) -> Result<(), MessageError> {
    let vers = with_message!(env_ref.get_version(), "get jvm version failed!")?;
    set_callbacks(&jvm, vers.into());

    load_ext_runtime(&jvm, env_ref)?;

    let app_args = launcher_arg.app_args();

    let helper = find_launcher_helper_from_env(env_ref)?;

    let target = launcher_arg.target();
    let main_class = jni_result_expect!(env_ref, helper.check_and_load_main(env_ref, target), &format!("can not load main class: {}", target.main_class()))?;
    // let main_class = env.find_class(&main_class_name).expect(&format!("not found main class: {}", &main_class_name));

    call_main(env_ref, &main_class, app_args)
}

fn call_main(env: &mut JNIEnv, main_class: &JClass, app_args: &Vec<String>) -> Result<(), MessageError> {
    let args = jni_result_expect!(env, env.new_object_array(jsize::from(app_args.len() as i32), "java/lang/String", JObject::null()))?;

    for (i, item) in app_args.iter().enumerate() {
        jni_result_expect!(env, env.set_object_array_element(&args, jsize::from(i as i32), jni_result_expect!(env, env.new_string(item))?))?;
    }

    let params = [jni::objects::JValue::Object(&args)];
    jni_result_expect!(env, env.call_static_method(main_class, "main", "([Ljava/lang/String;)V", &params))?;
    // match env.call_static_method(main_class, "main", "([Ljava/lang/String;)V",
    //                        &params) {
    //     Ok(_) => {
    //     }
    //     Err(err) => {
    //         if let Ok(true) = env.exception_check() {
    //             env.exception_describe().expect("print error failed!");
    //             env.exception_clear().unwrap();
    //         } else {
    //             eprintln!("main method execution failed: {err}")
    //         }
    //     }
    // }
    Ok(())
}