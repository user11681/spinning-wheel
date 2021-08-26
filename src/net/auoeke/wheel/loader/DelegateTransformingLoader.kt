package net.auoeke.wheel.loader

class DelegateTransformingLoader(parent: TransformingClassLoader) : TransformingClassLoader(parent.name + "-delegate", parent, listOf(*parent.urLs)) {
    @Throws(ClassNotFoundException::class)
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        return (parent as TransformingClassLoader).loadClass(name, resolve)
    }
}
