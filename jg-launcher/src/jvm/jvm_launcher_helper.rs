use crate::args_parser::LaunchTarget;
use jni::objects::{JClass, JValue};
use jni::AttachGuard;

const SUN_LAUNCHER_HELPER_CLASS: &str = "sun/launcher/LauncherHelper";

trait JvmLauncherHelper {
    fn check_and_load_main<'local>(&self, env: &AttachGuard<'local>, target: &LaunchTarget) -> jni::errors::Result<JClass<'local>>;
}

struct SunLauncherHelper<'local>{
    class: JClass<'local>
}

impl SunLauncherHelper {
    pub fn from_env<'local>(env: &mut AttachGuard<'local>) -> jni::errors::Result<SunLauncherHelper<'local>> {
        let class = env.find_class(SUN_LAUNCHER_HELPER_CLASS)?;
        Ok(SunLauncherHelper {
            class
        })
    }
}

impl JvmLauncherHelper for SunLauncherHelper {
    fn check_and_load_main<'local>(&self, env: &mut AttachGuard<'local>, target: &LaunchTarget) -> jni::errors::Result<JClass<'local>> {
        let use_stderr = JValue::Bool(true as u8);
        let mode = JValue::Int(target.sun_mode());
        let name = env.new_string(target.target_path()).expect(&format!("path convert failed: {}", target.target_path()));
        let result = env.call_static_method(&self.class, "checkAndLoadMain", "(ZILjava/lang/String;)Ljava/lang/Class;",
                                         &[use_stderr, mode, JValue::Object(&name)])?;
        Ok(JClass::from(result.l()?))
    }
}