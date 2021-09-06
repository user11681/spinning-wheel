package net.auoeke.wheel.loader

import net.auoeke.extensions.type
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
    fun transform(name: String, transformer: (ClassNode) -> Unit) {
        val foundClass = this.findLoadedClass(name)

        if (foundClass === null) {
            transformers.add(name to transformer)
        } else {
            delegates.computeIfAbsent(this, ::DelegateTransformingLoader).define(name, false, transformer)
        }
    }

    public override fun loadClass(name: String, resolve: Boolean): Class<*> {
        if (name.startsWith("java.") || name.startsWith("kotlin.")) {
            return super.loadClass(name, resolve)
        }

        delegates[this]?.findLoadedClass(name)?.also {return it}

        for (index in transformers.indices.reversed()) {
            val (key, value) = transformers[index]

            if (name == key) {
                transformers.removeAt(index)

                return this.define(name, resolve, value)
            }
        }

        return super.loadClass(name, resolve)
    }

    private fun define(name: String, resolve: Boolean, transformer: (ClassNode) -> Unit): Class<*> {
        val classFile = this.getResource("${name.replace('.', '/')}.class")
        val node = ClassNode()
        ClassReader(classFile.openStream()).accept(node, 0)
        transformer(node)

        val writer = ClassWriter(ClassWriter.COMPUTE_FRAMES)
        node.accept(writer)
        val bytecode = writer.toByteArray()
        val type = this.defineClass(name, bytecode, 0, bytecode.size, CodeSource(classFile, null as Array<Certificate?>?))

        if (resolve) {
            this.resolveClass(type)
        }

        return type
    }

    companion object {
        val klass = Classes.klass(type<TransformingClassLoader>())
        private val transformers: MutableList<Pair<String, (ClassNode) -> Unit>> = ArrayList()
        private val delegates: MutableMap<TransformingClassLoader, TransformingClassLoader> = IdentityHashMap()

        init {
            Classes.load<Any>("kotlin.jvm.internal.Intrinsics")
            Classes.load<Any>("kotlin.text.StringsKt")
        }
    }
}
