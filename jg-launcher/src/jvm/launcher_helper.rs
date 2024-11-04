use crate::args_parser::LaunchTarget;
use jni::objects::{JClass, JObject, JValue};
use jni::AttachGuard;

const SUN_LAUNCHER_HELPER_CLASS: &str = "sun/launcher/LauncherHelper";
const CLASS_LOADER: &str = "java/lang/ClassLoader";

pub trait JvmLauncherHelper<'local> {
    fn from_env<'a>(env: &mut AttachGuard<'a>) -> jni::errors::Result<Box<dyn JvmLauncherHelper<'a>>>;
    fn check_and_load_main(&self, env: &mut AttachGuard<'local>, target: &LaunchTarget) -> jni::errors::Result<JClass<'local>>;
}

pub struct SunLauncherHelper<'local>{
    class: JClass<'local>
}

impl<'local> JvmLauncherHelper<'local> for SunLauncherHelper<'local> {
    fn from_env<'a>(env: &mut AttachGuard<'a>) -> jni::errors::Result<Box<dyn JvmLauncherHelper<'a>>> {
        let class = env.find_class(SUN_LAUNCHER_HELPER_CLASS)?;
        Ok(Box::new(SunLauncherHelper {
            class
        }))
    }
    fn check_and_load_main(&self, env: &mut AttachGuard<'local>, target: &LaunchTarget) -> jni::errors::Result<JClass<'local>> {
        let use_stderr = JValue::Bool(true as u8);
        let mode = JValue::Int(target.sun_mode());
        let name = env.new_string(target.target_value()).expect(&format!("path convert failed: {}", target.target_value()));
        let result = env.call_static_method(&self.class, "checkAndLoadMain", "(ZILjava/lang/String;)Ljava/lang/Class;",
                                         &[use_stderr, mode, JValue::Object(&name)])?;
        Ok(JClass::from(result.l()?))
    }
}

pub struct SimpleLauncherHelper<'local>{
    class: JClass<'local>,
    class_loader: JObject<'local>
}

impl<'local> JvmLauncherHelper<'local> for SimpleLauncherHelper<'local> {
    fn from_env<'a>(env: &mut AttachGuard<'a>) -> jni::errors::Result<Box<dyn JvmLauncherHelper<'a>>> {
        let class_loader_class = env.find_class(CLASS_LOADER)?;
        let class_loader_object = env.call_static_method(&class_loader_class, "getSystemClassLoader", "()Ljava/lang/ClassLoader;", &[])?;
        let class_loader = class_loader_object.l()?;
        Ok(Box::new(SimpleLauncherHelper {
            class: class_loader_class,
            class_loader
        }))
    }
    fn check_and_load_main(&self, env: &mut AttachGuard<'local>, target: &LaunchTarget) -> jni::errors::Result<JClass<'local>> {
        let class_name = target.main_class().replace('.', "/");
        let name = env.new_string(&class_name).expect(&format!("path convert failed: {}", &class_name));
        let result = env.call_method(&self.class_loader, "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;", &[JValue::Object(&name)])?;
        Ok(JClass::from(result.l()?))
    }
}