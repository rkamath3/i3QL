package sae
package bytecode

import sae.DefaultLazyView
import sae.bytecode.model._
import sae.collections._
import sae.collections.Conversions._
import sae.bytecode.transform._
import de.tud.cs.st.bat._

/**
 * Convenience database that materializes all queries as a result.
 */
class MaterializedDatabase {
    
    val db : BytecodeDatabase = new BytecodeDatabase()

    val classfiles : QueryResult[ObjectType] = db.classfiles

    val classfile_methods : QueryResult[Method] = db.classfile_methods

    val classes : QueryResult[ObjectType] = db.classes

    val methods : QueryResult[Method] = db.methods

    val instructions : QueryResult[Instr] = db.instructions

    val method_calls : QueryResult[MethodCall] = db.method_calls

    /**
     * Convenience method that opens a stream from a resource in the class path
     */
    def addArchiveAsResource(name : String) : Unit =
        db.addArchiveAsResource(name)

    /**
     * Convenience method that opens a stream from a file in the file system
     */
    def addArchiveAsFile(name : String) : Unit =
        db.addArchiveAsFile(name)

    /**
     * Read a jar archive from the stream.
     * The underlying data is assumed to be in zip (jar) format
     */
    def addArchiveStream(stream : java.io.InputStream) : Unit =
        db.addArchiveStream(stream)
}