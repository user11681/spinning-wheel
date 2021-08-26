package net.auoeke.wheel.loader

import net.auoeke.reflect.Classes
import org.gradle.internal.classloader.VisitableURLClassLoader
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import java.net.URL
import java.security.CodeSource
import java.security.cert.Certificate
import java.util.*

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
open class TransformingClassLoader(name: String, parent: ClassLoader?, urls: Collection<URL?>?) : VisitableURLClassLoader(name, parent, urls) {
    fun transform(name: String, transformer: Transformer) {
        val foundClass = findLoadedClass(name)

        if (foundClass == null) {
            transformers.add(name to transformer)
        } else {
            delegates.computeIfAbsent(this) {DelegateTransformingLoader(it)}.define(name, false, transformer)
        }
    }

    public override fun loadClass(name: String, resolve: Boolean): Class<*> {
        if (name.startsWith("java.")) {
            return super.loadClass(name, resolve)
        }

        val delegate: TransformingClassLoader? = delegates[this]

        if (delegate != null) {
            val override = delegate.findLoadedClass(name)

            if (override != null) {
                return override
            }
        }

        for (index in transformers.indices.reversed()) {
            val (key, value) = transformers[index]

            if (name == key) {
                transformers.removeAt(index)

                return define(name, resolve, value)
            }
        }

        return super.loadClass(name, resolve)
    }

    protected fun define(name: String, resolve: Boolean, transformer: Transformer): Class<*> {
        val classFile = getResource("${name.replace('.', '/')}.class")
        val node = ClassNode()
        ClassReader(classFile.openStream()).accept(node, 0)
        transformer.transform(node)

        val writer = ClassWriter(ClassWriter.COMPUTE_FRAMES)
        node.accept(writer)
        val bytecode = writer.toByteArray()
        val type = this.defineClass(name, bytecode, 0, bytecode.size, CodeSource(classFile, null as Array<Certificate?>?))

        if (resolve) {
            resolveClass(type)
        }

        return type
    }

    companion object {
        val klass = Classes.klass(TransformingClassLoader::class.java)
        private val transformers: MutableList<Pair<String, Transformer>> = ArrayList()
        private val delegates: MutableMap<TransformingClassLoader, DelegateTransformingLoader> = IdentityHashMap()
    }
}
