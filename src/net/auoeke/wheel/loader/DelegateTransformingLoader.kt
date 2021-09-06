package net.auoeke.wheel.loader

class DelegateTransformingLoader(parent: TransformingClassLoader) : TransformingClassLoader(parent.name + "-delegate", parent, listOf(*parent.urLs)) {
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        return (this.parent as TransformingClassLoader).loadClass(name, resolve)
    }
}
