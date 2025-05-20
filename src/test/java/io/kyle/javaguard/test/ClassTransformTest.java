package io.kyle.javaguard.test;

import io.kyle.javaguard.bean.EncryptInfo;
import io.kyle.javaguard.bean.KeyInfo;
import io.kyle.javaguard.bean.TransformInfo;
import io.kyle.javaguard.transform.ClassTransformer;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Stack;

@Ignore
public class ClassTransformTest {
    @Test
    public void test() throws Exception {
        InputStream origin = ClassTransformTest.class.getResourceAsStream("TestClass.class");
        Files.createDirectories(Paths.get("out/e"));
        Files.createDirectories(Paths.get("out/d"));
        String outEncryptFile = "out/e/TestClass.class";
        String outDecryptFile = "out/d/TestClass.class";
        TransformInfo transformInfo = new TransformInfo();
        byte[] sha512 = DigestUtils.sha512("test");
        transformInfo.setKeyInfo(new KeyInfo(Arrays.copyOfRange(sha512, 0, 512 >> 4)));
        transformInfo.setResourceKeyInfo(new KeyInfo(Arrays.copyOfRange(sha512, 512 >> 4, sha512.length)));
        ClassTransformer classTransformer = new ClassTransformer(transformInfo);
        BufferedInputStream stream = new BufferedInputStream(origin);
        FileOutputStream out = new FileOutputStream(outEncryptFile);
        classTransformer.encrypt(stream, out);
        stream.close();
        out.close();
        FileInputStream encryptStream = new FileInputStream(outEncryptFile);
        FileOutputStream outDecryptStream = new FileOutputStream(outDecryptFile);
        classTransformer.decrypt(encryptStream, outDecryptStream);
        encryptStream.close();
        outDecryptStream.close();
    }

    @Test
    public void test2() throws Exception {
//        byte[] bytes = FileUtils.readFileToByteArray(new File("/home/kyle/data/code/java/JavaGuard/out/e/TestClass.class"));
//        ClassFile classFile = new ClassFile(new DataInputStream(new ByteArrayInputStream(bytes)));
//        CtClass ctClass = ClassPool.getDefault().makeClass(classFile);
//        Class<?> aClass = ctClass.toClass();
        Class<?> aClass = ClassLoader.getSystemClassLoader().loadClass("io.kyle.javaguard.test.TestClass");

        Method code2 = aClass.getMethod("code2", byte[].class);
        LocalVariableTableParameterNameDiscoverer discoverer = new LocalVariableTableParameterNameDiscoverer();
        String[] parameterNames = discoverer.getParameterNames(code2);
        System.out.println(String.join(",", parameterNames));
    }

    @Test
    public void testClassOut() throws Exception {
        ClassPool aDefault = ClassPool.getDefault();
        Stack<String> stack = new Stack<>();
        String originName = ClassFile.class.getName();
        stack.push(originName);
        HashSet<String> exists = new HashSet<>();
        exists.add(originName);
        int usedCount = 0;
        while (!stack.isEmpty()) {
            String className = stack.pop();
            CtClass ctClass = aDefault.getCtClass(className);
            ConstPool constPool = ctClass.getClassFile().getConstPool();
            for (int i = 1; i < constPool.getSize(); i++) {
                if (constPool.getTag(i) == ConstPool.CONST_Class) {
                    String classInfo = constPool.getClassInfo(i);
                    if (!classInfo.startsWith("javassist")) {
                        continue;
                    }
                    if (exists.add(classInfo)) {
                        stack.push(classInfo);
                        System.out.println(classInfo);
                        usedCount++;
                    }
                }
            }
        }

        ZipArchiveInputStream stream = new ZipArchiveInputStream(new FileInputStream("/home/kyle/.m2/repository/org/javassist/javassist/3.30.2-GA/javassist-3.30.2-GA.jar"));
        ZipArchiveEntry nextEntry;
        int count = 0;
        while ((nextEntry = stream.getNextEntry()) != null) {
            if (nextEntry.getName().endsWith(".class")) {
                count++;
            }
        }
        stream.close();
        System.out.println(usedCount);
        System.out.println(count);
    }
}
/*
javassist.bytecode.ConstPool
javassist.bytecode.AccessFlag
javassist.bytecode.SourceFileAttribute
javassist.bytecode.MethodInfo
javassist.bytecode.FieldInfo
javassist.bytecode.AttributeInfo
javassist.bytecode.AnnotationsAttribute
javassist.bytecode.SignatureAttribute
javassist.bytecode.InnerClassesAttribute
javassist.bytecode.BadBytecode
javassist.CannotCompileException
javassist.bytecode.Descriptor
javassist.bytecode.DuplicateMemberException
javassist.CtClass
javassist.NotFoundException
javassist.CtPrimitiveType
javassist.ClassPool
javassist.bytecode.Descriptor$PrettyPrinter
javassist.bytecode.Descriptor$Iterator
javassist.ClassPoolTail
javassist.CtClassType
javassist.CtArray
javassist.CtNewClass
javassist.Modifier
javassist.util.proxy.DefineClassHelper
javassist.util.proxy.DefinePackageHelper
javassist.util.proxy.DefinePackageHelper$Helper
javassist.util.proxy.DefinePackageHelper$Java9
javassist.util.proxy.DefinePackageHelper$Java7
javassist.util.proxy.DefinePackageHelper$JavaOther
javassist.util.proxy.DefinePackageHelper$1
javassist.util.proxy.SecurityActions
javassist.util.proxy.SecurityActions$1
javassist.util.proxy.SecurityActions$2
javassist.util.proxy.SecurityActions$3
javassist.util.proxy.SecurityActions$4
javassist.util.proxy.SecurityActions$5
javassist.util.proxy.SecurityActions$6
javassist.util.proxy.SecurityActions$7
javassist.util.proxy.SecurityActions$8
javassist.util.proxy.SecurityActions$TheUnsafe
javassist.util.proxy.DefineClassHelper$Helper
javassist.util.proxy.DefineClassHelper$Java11
javassist.util.proxy.DefineClassHelper$Java9
javassist.util.proxy.DefineClassHelper$Java7
javassist.util.proxy.DefineClassHelper$JavaOther
javassist.util.proxy.DefineClassHelper$1
javassist.util.proxy.DefineClassHelper$Java9$ReferencedUnsafe
javassist.CtConstructor
javassist.CtNewConstructor
javassist.compiler.Javac
javassist.compiler.CompileError
javassist.bytecode.Bytecode
javassist.CtNewWrappedConstructor
javassist.CtMethod$ConstParameter
javassist.CtMethod
javassist.CtBehavior
javassist.CtNewMethod
javassist.bytecode.CodeAttribute
javassist.bytecode.CodeIterator
javassist.bytecode.Opcode
javassist.CtNewWrappedMethod
javassist.CtMethod$StringConstParameter
javassist.CtMethod$LongConstParameter
javassist.CtMethod$IntConstParameter
javassist.ClassMap
javassist.bytecode.SyntheticAttribute
javassist.CtMember$Cache
javassist.compiler.JvstCodeGen
javassist.CtMember
javassist.compiler.MemberCodeGen
javassist.compiler.JvstTypeChecker
javassist.compiler.TokenId
javassist.compiler.CodeGen
javassist.compiler.ast.Member
javassist.compiler.ast.ASTree
javassist.compiler.ast.CastExpr
javassist.compiler.ast.ASTList
javassist.compiler.ast.Symbol
javassist.compiler.MemberResolver
javassist.compiler.ast.CallExpr
javassist.compiler.ProceedHandler
javassist.compiler.ast.Expr
javassist.compiler.ast.Stmnt
javassist.compiler.ast.Declarator
javassist.compiler.SymbolTable
javassist.compiler.ast.Visitor
javassist.compiler.MemberResolver$Method
javassist.compiler.NoFieldException
javassist.compiler.ast.Keyword
javassist.compiler.ast.FieldDecl
javassist.compiler.ast.MethodDecl
javassist.compiler.ast.AssignExpr
javassist.compiler.TypeChecker
javassist.compiler.ast.IntConst
javassist.compiler.ast.StringL
javassist.compiler.CodeGen$ReturnHook
javassist.compiler.CodeGen$1
javassist.compiler.ast.Variable
javassist.compiler.ast.ArrayInit
javassist.compiler.ast.CondExpr
javassist.compiler.ast.BinExpr
javassist.compiler.ast.InstanceOfExpr
javassist.compiler.ast.DoubleConst
javassist.compiler.ast.NewExpr
javassist.CtField
javassist.CtField$Initializer
javassist.CtField$MultiArrayInitializer
javassist.CtField$ArrayInitializer
javassist.CtField$StringInitializer
javassist.CtField$DoubleInitializer
javassist.CtField$FloatInitializer
javassist.CtField$LongInitializer
javassist.CtField$IntInitializer
javassist.CtField$MethodInitializer
javassist.CtField$NewInitializer
javassist.CtField$ParamInitializer
javassist.CtField$PtreeInitializer
javassist.CtField$CodeInitializer
javassist.CtField$CodeInitializer0
javassist.compiler.MemberCodeGen$JsrHook
javassist.compiler.ast.Pair
javassist.compiler.MemberCodeGen$JsrHook2
javassist.compiler.AccessorMaker
javassist.bytecode.ExceptionsAttribute
javassist.bytecode.ByteArray
javassist.bytecode.CodeIterator$Gap
javassist.bytecode.ExceptionTable
javassist.bytecode.CodeIterator$AlignmentException
javassist.bytecode.LineNumberAttribute
javassist.bytecode.LocalVariableAttribute
javassist.bytecode.StackMapTable
javassist.bytecode.StackMap
javassist.bytecode.CodeIterator$Pointers
javassist.bytecode.CodeAttribute$LdcEntry
javassist.bytecode.CodeIterator$LdcW
javassist.bytecode.CodeIterator$Branch
javassist.bytecode.CodeIterator$Jump16
javassist.bytecode.CodeIterator$If16
javassist.bytecode.CodeIterator$Jump32
javassist.bytecode.CodeIterator$Table
javassist.bytecode.CodeIterator$Lookup
javassist.bytecode.CodeIterator$Switcher
javassist.bytecode.CodeIterator$Branch16
javassist.bytecode.StackMap$Copier
javassist.bytecode.StackMap$InsertLocal
javassist.bytecode.StackMap$Shifter
javassist.bytecode.StackMap$SwitchShifter
javassist.bytecode.StackMap$NewRemover
javassist.bytecode.StackMap$Printer
javassist.bytecode.StackMap$Writer
javassist.bytecode.StackMap$SimpleCopy
javassist.bytecode.StackMap$Walker
javassist.bytecode.StackMapTable$Copier
javassist.bytecode.StackMapTable$RuntimeCopyException
javassist.bytecode.StackMapTable$InsertLocal
javassist.bytecode.StackMapTable$Printer
javassist.bytecode.StackMapTable$OffsetShifter
javassist.bytecode.StackMapTable$Shifter
javassist.bytecode.StackMapTable$SwitchShifter
javassist.bytecode.StackMapTable$NewRemover
javassist.bytecode.StackMapTable$Writer
javassist.bytecode.StackMapTable$SimpleCopy
javassist.bytecode.StackMapTable$Walker
javassist.bytecode.LineNumberAttribute$Pc
javassist.bytecode.ExceptionTableEntry
javassist.bytecode.CodeAttribute$RuntimeCopyException
javassist.bytecode.CodeAnalyzer
javassist.bytecode.ParameterAnnotationsAttribute
javassist.bytecode.LocalVariableTypeAttribute
javassist.CodeConverter
javassist.expr.ExprEditor
javassist.expr.ExprEditor$LoopContext
javassist.expr.Handler
javassist.expr.MethodCall
javassist.expr.FieldAccess
javassist.expr.ExprEditor$NewOp
javassist.expr.NewExpr
javassist.expr.ConstructorCall
javassist.expr.NewArray
javassist.expr.Instanceof
javassist.expr.Cast
javassist.expr.Expr
javassist.expr.Cast$ProceedForCast
javassist.expr.Instanceof$ProceedForInstanceof
javassist.expr.NewArray$ProceedForArray
javassist.expr.NewExpr$ProceedForNew
javassist.expr.FieldAccess$ProceedForRead
javassist.expr.FieldAccess$ProceedForWrite
javassist.convert.TransformNew
javassist.convert.TransformNewClass
javassist.convert.TransformFieldAccess
javassist.convert.TransformReadField
javassist.convert.TransformWriteField
javassist.convert.TransformAccessArrayField
javassist.convert.TransformCall
javassist.convert.TransformCallToStatic
javassist.convert.TransformBefore
javassist.convert.TransformAfter
javassist.convert.Transformer
javassist.CodeConverter$ArrayAccessReplacementMethodNames
javassist.CodeConverter$DefaultArrayAccessReplacementMethodNames
javassist.bytecode.analysis.Analyzer
javassist.bytecode.analysis.Frame
javassist.bytecode.analysis.Type
javassist.bytecode.analysis.MultiType
javassist.bytecode.analysis.MultiArrayType
javassist.bytecode.analysis.SubroutineScanner
javassist.bytecode.analysis.IntQueue
javassist.bytecode.analysis.Executor
javassist.bytecode.analysis.Util
javassist.bytecode.analysis.Analyzer$ExceptionInfo
javassist.bytecode.analysis.Subroutine
javassist.bytecode.analysis.Analyzer$1
javassist.bytecode.analysis.IntQueue$Entry
javassist.bytecode.analysis.IntQueue$1
javassist.bytecode.AnnotationsAttribute$Copier
javassist.bytecode.AnnotationsAttribute$Parser
javassist.bytecode.annotation.AnnotationsWriter
javassist.bytecode.annotation.Annotation
javassist.bytecode.AnnotationsAttribute$Renamer
javassist.bytecode.AnnotationsAttribute$Walker
javassist.bytecode.annotation.BooleanMemberValue
javassist.bytecode.annotation.ByteMemberValue
javassist.bytecode.annotation.CharMemberValue
javassist.bytecode.annotation.ShortMemberValue
javassist.bytecode.annotation.IntegerMemberValue
javassist.bytecode.annotation.LongMemberValue
javassist.bytecode.annotation.FloatMemberValue
javassist.bytecode.annotation.DoubleMemberValue
javassist.bytecode.annotation.ClassMemberValue
javassist.bytecode.annotation.StringMemberValue
javassist.bytecode.annotation.ArrayMemberValue
javassist.bytecode.annotation.AnnotationMemberValue
javassist.bytecode.annotation.EnumMemberValue
javassist.bytecode.annotation.Annotation$Pair
javassist.bytecode.annotation.MemberValue
javassist.bytecode.annotation.AnnotationImpl
javassist.bytecode.annotation.NoSuchClassError
javassist.bytecode.AnnotationDefaultAttribute
javassist.bytecode.annotation.MemberValueVisitor
javassist.bytecode.SignatureAttribute$Type
javassist.bytecode.ByteVector
javassist.compiler.Parser
javassist.compiler.Lex
javassist.compiler.Javac$CtFieldWithInit
javassist.compiler.Javac$1
javassist.compiler.Javac$2
javassist.compiler.Javac$3
javassist.compiler.Token
javassist.compiler.KeywordTable
javassist.compiler.SyntaxError
javassist.bytecode.EnclosingMethodAttribute
javassist.bytecode.ConstantAttribute
javassist.FieldInitLink
javassist.ClassPathList
javassist.ClassPath
javassist.ClassClassPath
javassist.LoaderClassPath
javassist.JarClassPath
javassist.JarDirClassPath
javassist.DirClassPath
javassist.JarDirClassPath$1
javassist.CtClass$1
javassist.CtClass$DelayedFileOutputStream
javassist.bytecode.SignatureAttribute$Cursor
javassist.bytecode.SignatureAttribute$ClassType
javassist.bytecode.SignatureAttribute$ClassSignature
javassist.bytecode.SignatureAttribute$ArrayType
javassist.bytecode.SignatureAttribute$ObjectType
javassist.bytecode.SignatureAttribute$MethodSignature
javassist.bytecode.SignatureAttribute$TypeParameter
javassist.bytecode.SignatureAttribute$TypeVariable
javassist.bytecode.SignatureAttribute$TypeArgument
javassist.bytecode.SignatureAttribute$BaseType
javassist.bytecode.SignatureAttribute$1
javassist.bytecode.SignatureAttribute$NestedClassType
javassist.bytecode.BootstrapMethodsAttribute
javassist.bytecode.DeprecatedAttribute
javassist.bytecode.MethodParametersAttribute
javassist.bytecode.NestHostAttribute
javassist.bytecode.NestMembersAttribute
javassist.bytecode.TypeAnnotationsAttribute
javassist.bytecode.TypeAnnotationsAttribute$Copier
javassist.bytecode.TypeAnnotationsAttribute$Renamer
javassist.bytecode.TypeAnnotationsAttribute$SubCopier
javassist.bytecode.TypeAnnotationsAttribute$SubWalker
javassist.bytecode.TypeAnnotationsAttribute$TAWalker
javassist.bytecode.annotation.TypeAnnotationsWriter
javassist.bytecode.BootstrapMethodsAttribute$BootstrapMethod
javassist.bytecode.stackmap.MapMaker
javassist.bytecode.stackmap.TypedBlock
javassist.bytecode.stackmap.BasicBlock$JsrBytecode
javassist.bytecode.stackmap.Tracer
javassist.bytecode.stackmap.BasicBlock$Catch
javassist.bytecode.stackmap.TypeData$ClassName
javassist.bytecode.stackmap.TypeData$BasicType
javassist.bytecode.stackmap.TypeData$AbsTypeVar
javassist.bytecode.stackmap.TypeData
javassist.bytecode.stackmap.BasicBlock
javassist.bytecode.stackmap.BasicBlock$Maker
javassist.bytecode.stackmap.BasicBlock$Mark
javassist.bytecode.stackmap.TypeTag
javassist.bytecode.stackmap.TypeData$TypeVar
javassist.bytecode.stackmap.TypeData$ArrayType
javassist.bytecode.stackmap.TypeData$ArrayElement
javassist.bytecode.stackmap.TypeData$UninitThis
javassist.bytecode.stackmap.TypeData$UninitData
javassist.bytecode.stackmap.TypeData$NullType
javassist.bytecode.stackmap.TypeData$UninitTypeVar
javassist.bytecode.stackmap.TypedBlock$Maker
javassist.bytecode.ClassInfo
javassist.bytecode.FieldrefInfo
javassist.bytecode.MethodrefInfo
javassist.bytecode.InterfaceMethodrefInfo
javassist.bytecode.StringInfo
javassist.bytecode.IntegerInfo
javassist.bytecode.FloatInfo
javassist.bytecode.LongInfo
javassist.bytecode.DoubleInfo
javassist.bytecode.NameAndTypeInfo
javassist.bytecode.Utf8Info
javassist.bytecode.MethodHandleInfo
javassist.bytecode.MethodTypeInfo
javassist.bytecode.DynamicInfo
javassist.bytecode.InvokeDynamicInfo
javassist.bytecode.ModuleInfo
javassist.bytecode.PackageInfo
javassist.bytecode.LongVector
javassist.bytecode.ConstInfo
javassist.bytecode.MemberrefInfo
javassist.bytecode.ConstInfoPadding
*/