use crate::args_parser::LaunchTarget::Jar;
use crate::args_parser::LauncherArg;
use crate::jvm::launcher_helper::{JvmLauncherHelper, SimpleLauncherHelper, SunLauncherHelper};
use jni::objects::JObject;
use jni::sys::jsize;
use jni::JNIVersion;
use jni::JavaVM;

const JAVA_CLASS_PATH_VM_ARG_PREFIX: &str = "-Djava.class.path=";

pub fn jvm_launch(launcher_arg: &LauncherArg) {
    let vm_ops = launcher_arg.vm_args();

    let mut args_builder = jni::InitArgsBuilder::new()
        .version(JNIVersion::V8);
    let target = launcher_arg.target();
    let mut java_class_path = String::from(JAVA_CLASS_PATH_VM_ARG_PREFIX);
    if let Jar(jar) = &target {
        java_class_path.push_str(jar.path());
        // args_builder = args_builder.option(&java_class_path);
        (jar.main_class().replace('.', "/"), jar.signature())
    } else {
        // todo not currently supported
        panic!("not currently supported run class")
    };
    for item in vm_ops.iter() {
        args_builder = args_builder.option(item.trim());
    };
    let init_args = args_builder
        // .library_path(jvm_lib_path) // 指定jvm.so库路径
        .option(&java_class_path)
        .build()
        .expect("init Jvm args failed");
    // create JVM
    let jvm = JavaVM::new(init_args).unwrap();

    // get JNI env
    let mut env = jvm.attach_current_thread().expect("get jni env failed");

    let app_args = launcher_arg.app_args();
    let args = env.new_object_array(jsize::from(app_args.len() as i32), "java/lang/String", JObject::null()).unwrap();

    for (i, item) in app_args.iter().enumerate() {
        env.set_object_array_element(&args, jsize::from(i as i32), env.new_string(item).unwrap()).unwrap();
    }

    let helper = match SunLauncherHelper::from_env(&mut env) {
        Ok(helper) => helper,
        Err(_) => SimpleLauncherHelper::from_env(&mut env).expect("")
    };

    let main_class = helper.check_and_load_main(&mut env, target).expect(&format!("can not load main class: {}", target.main_class()));
    // let main_class = env.find_class(&main_class_name).expect(&format!("not found main class: {}", &main_class_name));

    let params = [jni::objects::JValue::Object(&args)];
    env.call_static_method(&main_class, "main", "([Ljava/lang/String;)V",
                           &params).unwrap();

    unsafe {
        jvm.detach_current_thread();
        jvm.destroy().unwrap();
    }
}