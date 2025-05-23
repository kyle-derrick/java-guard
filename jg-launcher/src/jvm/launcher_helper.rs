use crate::args_parser::LaunchTarget;
use jni::objects::{JClass, JObject, JValue};
use jni::JNIEnv;
use jni_sys::jboolean;

const SUN_LAUNCHER_HELPER_CLASS: &str = "sun/launcher/LauncherHelper";
const CLASS_LOADER: &str = "java/lang/ClassLoader";

pub enum LauncherHelper<'local> {
    SimpleLauncherHelper(SimpleLauncherHelper<'local>),
    SunLauncherHelper(SunLauncherHelper<'local>)
}

impl<'local> JvmLauncherHelper<'local> for LauncherHelper<'local> {
    fn check_and_load_main(&self, env: &mut JNIEnv<'local>, target: &LaunchTarget) -> jni::errors::Result<JClass<'local>> {
        match self {
            LauncherHelper::SimpleLauncherHelper(helper) => helper.check_and_load_main(env, target),
            LauncherHelper::SunLauncherHelper(helper) => helper.check_and_load_main(env, target),
        }
    }
}

pub trait JvmLauncherHelper<'local> {
    fn check_and_load_main(&self, env: &mut JNIEnv<'local>, target: &LaunchTarget) -> jni::errors::Result<JClass<'local>>;
}

pub struct SunLauncherHelper<'local>{
    class: JClass<'local>
}

impl SunLauncherHelper<'_> {
    pub fn from_env<'a>(env: & mut JNIEnv<'a>) -> jni::errors::Result<LauncherHelper<'a>> {
        let class = env.find_class(SUN_LAUNCHER_HELPER_CLASS)?;
        Ok(LauncherHelper::SunLauncherHelper(SunLauncherHelper {
            class
        }))
    }
}

impl<'local> JvmLauncherHelper<'local> for SunLauncherHelper<'local> {
    fn check_and_load_main(&self, env: &mut JNIEnv<'local>, target: &LaunchTarget) -> jni::errors::Result<JClass<'local>> {
        let use_stderr = JValue::Bool(true as jboolean);
        let mode = JValue::Int(target.sun_mode());
        let name = env.new_string(target.target_value()).expect(&format!("path convert failed: {}", target.target_value()));
        // let name_str = target.target_value();
        let result = env.call_static_method(&self.class, "checkAndLoadMain", "(ZILjava/lang/String;)Ljava/lang/Class;",
                                         &[use_stderr, mode, JValue::Object(&name)])?;
        Ok(JClass::from(result.l()?))
    }
}

pub struct SimpleLauncherHelper<'local>{
    class: JClass<'local>,
    class_loader: JObject<'local>
}

impl SimpleLauncherHelper<'_> {
    pub fn from_env<'a>(env: & mut JNIEnv<'a>) -> jni::errors::Result<LauncherHelper<'a>> {
        let class_loader_class = env.find_class(CLASS_LOADER)?;
        let class_loader_object = env.call_static_method(&class_loader_class, "getSystemClassLoader", "()Ljava/lang/ClassLoader;", &[])?;
        let class_loader = class_loader_object.l()?;
        Ok(LauncherHelper::SimpleLauncherHelper(SimpleLauncherHelper {
            class: class_loader_class,
            class_loader
        }))
    }
}

impl<'local> JvmLauncherHelper<'local> for SimpleLauncherHelper<'local> {
    fn check_and_load_main(&self, env: &mut JNIEnv<'local>, target: &LaunchTarget) -> jni::errors::Result<JClass<'local>> {
        let class_name = target.main_class().replace('/', ".");
        let name = env.new_string(&class_name).expect(&format!("path convert failed: {}", &class_name));
        let result = env.call_method(&self.class_loader, "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;", &[JValue::Object(&name)])?;
        Ok(JClass::from(result.l()?))
    }
}


pub fn find_launcher_helper_from_env<'a>(env: & mut JNIEnv<'a>) -> LauncherHelper<'a> {
    match SunLauncherHelper::from_env(env) {
        Ok(helper) => return helper,
        Err(_) => println!("WARN: not found sun launcher helper")
    }
    SimpleLauncherHelper::from_env(env).expect("cannot init launcher helper")
}