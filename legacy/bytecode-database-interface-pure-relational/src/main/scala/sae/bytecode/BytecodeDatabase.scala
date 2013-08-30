package sae
package bytecode

import sae.BagExtent
import sae.bytecode.model._
import dependencies._
import internal._
import sae.bytecode.model.instructions._
import sae.syntax.RelationalAlgebraSyntax._
import sae.bytecode.transform._
import de.tud.cs.st.bat._
import sae.reader.BytecodeReader
import java.io.{File, InputStream}

/**
 *  extends(Class1, Class2)
 *  implements(Class1, Class2)
 *  field_type(FieldReference, Class)
 *  parameter(MethodReference, Class)
 *  return_type(MethodReference, Class)
 *  write_field(MethodReference, FieldReference)
 *  read_field(MethodReference, FieldReference)
 *  calls(Method1, Method2)
 *  class_cast(MethodReference, Class)
 *  instanceof(MethodReference, Class)
 *  create(MethodReference, Class)
 *  create_class_array(MethodReference, Class)
 *  throw(MethodReference, Class)
 *  get_class(MethodReference, Class)
 *  annotation(Class|FieldReference|MethodReference, Class)
 *  parameter_annotation(MethodReference, Class)
 */
class BytecodeDatabase extends Database
{
    /**
     * BEWARE INITIALIZATION ORDER OF FIELDS (scala compiler will not warn you)
     */

    // TODO check whether declared_types and classfile methods can be declared as views in combination with a classfile_source(Class, File) table and how this affects performance

    val declared_classes = new BagExtent[ClassDeclaration]

    lazy val declared_types: Relation[ObjectType] = Π((_: ClassDeclaration).objectType)(declared_classes)

    val declared_methods = new BagExtent[MethodDeclaration]

    val declared_fields = new BagExtent[FieldDeclaration]

    val classes: Relation[ObjectType] = new BagExtent[ObjectType]

    val methods: Relation[MethodReference] = new BagExtent[MethodReference]

    val fields: Relation[FieldReference] = new BagExtent[FieldReference]

    val instructions: Relation[Instr[_]] = new BagExtent[Instr[_]]

    /**
     * Begin with individual relations (derived are lazy vals others are filled during bytecode reading)
     */

    val `extends`: Relation[`extends`] = new BagExtent[`extends`]

    val implements: Relation[implements] = new BagExtent[implements]

    lazy val subtypes: Relation[(ObjectType, ObjectType)] = TC(
        `extends`.⊎[Dependency[ObjectType, ObjectType], implements](implements)
    )((_: Dependency[ObjectType, ObjectType]).source, (_: Dependency[ObjectType, ObjectType]).target)


    // inner classes are added multiple times for each inner class, since the definition is repeated in the classfile
    // TODO these relations are currently external for performance measurements
    val internal_inner_classes: Relation[unresolved_inner_class_entry] = new BagExtent[unresolved_inner_class_entry]

    val internal_enclosing_methods: Relation[unresolved_enclosing_method] = new BagExtent[unresolved_enclosing_method]

    lazy val internal_guaranteed_inner_classes: Relation[inner_class] =
        δ(
            Π(
                // the directly encoded inner classes have their outer type set
                (entry: unresolved_inner_class_entry) =>
                    new inner_class(
                        entry.outerClassType,
                        entry.innerClassType,
                        true,
                        if (entry.innerName eq null) {
                            None
                        } else {
                            Some(entry.innerName)
                        }
                    )
            )(
                σ((_: unresolved_inner_class_entry).outerClassType ne null)(internal_inner_classes)
            )
        )
    /*
     * Deduces inner classes only by looking at the inner classes attribute.
     * Taking enclosing methods into account is not feasible for older jars.
     */
    lazy val inner_classes: Relation[inner_class] =
        internal_guaranteed_inner_classes ⊎
                (
                        Π(
                            // the directly encoded inner classes have their outer type set
                            (entry: unresolved_inner_class_entry) =>
                                new inner_class(
                                    entry.declaringClass,
                                    entry.innerClassType,
                                    false,
                                    if (entry.innerName eq null) {
                                        None
                                    } else {
                                        Some(entry.innerName)
                                    }
                                )
                        )
                                (
                                    // TODO this is a pragmatic solution that checks that the name if the inner type is longer than the name of the outer type, it passes all tests, and it seems that classes never mention inner_classes beyond one level which might be falsely identified by this test
                                    σ((e: unresolved_inner_class_entry) =>
                                        (e.outerClassType eq null) && (e
                                                .innerClassType.className.length() > e.declaringClass.className
                                                .length()))(
                                        internal_inner_classes
                                    )
                                )
                        )
    lazy val field_type: Relation[field_type] = Π((f: FieldDeclaration) =>
        new field_type(f, f
                .fieldType))(declared_fields)

    val parameter: Relation[parameter] = new BagExtent[parameter]

    lazy val return_type: Relation[return_type] = Π((m: MethodDeclaration) =>
        new return_type(m, m
                .returnType))(declared_methods)

    val exception_handlers = new BagExtent[ExceptionHandler]()

    // exception handelers can have an undefined catchType. This is used to implement finally blocks
    lazy val handled_exceptions: Relation[ExceptionHandler] = σ((_: ExceptionHandler).catchType != None)(
        exception_handlers
    )

    val thrown_exceptions: Relation[throws] = new BagExtent[throws]()

    lazy val write_field: Relation[write_field] =
        (
                Π[Instr[_], write_field] {
                    case putfield(declaringMethod, _, field) => new write_field(declaringMethod, field, false)
                }(σ[putfield](instructions))
                ) ⊎ (
                Π[Instr[_], write_field] {
                    case putstatic(declaringMethod, _, field) => new write_field(declaringMethod, field, true)
                }(σ[putstatic](instructions))
                )

    lazy val read_field: Relation[read_field] =
        (
                Π[Instr[_], read_field] {
                    case getfield(declaringMethod, _, field) => new read_field(declaringMethod, field, false)
                }(σ[getfield](instructions))
                ) ⊎ (
                Π[Instr[_], read_field] {
                    case getstatic(declaringMethod, _, field) => new read_field(declaringMethod, field, true)
                }(σ[getstatic](instructions))
                )

    lazy val invoke_interface: Relation[invoke_interface] = Π(
        (_: Instr[_]) match {
            case invokeinterface(declaringMethod, pc, callee) => new invoke_interface(declaringMethod, callee)
        }
    )(σ[invokeinterface](instructions))

    lazy val invoke_special: Relation[invoke_special] = Π(
        (_: Instr[_]) match {
            case invokespecial(declaringMethod, pc, callee) => new invoke_special(declaringMethod, callee)
        }
    )(σ[invokespecial](instructions))

    lazy val invoke_virtual: Relation[invoke_virtual] = Π(
        (_: Instr[_]) match {
            case invokevirtual(declaringMethod, pc, callee) => new invoke_virtual(declaringMethod, callee)
        }
    )(σ[invokevirtual](instructions))

    lazy val invoke_static: Relation[invoke_static] = Π(
        (_: Instr[_]) match {
            case invokestatic(declaringMethod, pc, callee) => new invoke_static(declaringMethod, callee)
        }
    )(σ[invokestatic](instructions))

    lazy val calls: Relation[calls] = invoke_interface.⊎[calls, calls](
        invoke_special.⊎[calls, calls](
            invoke_virtual.⊎[calls, invoke_static](
                invoke_static
            )
        )
    )


    // TODO array references to primitive arrays are exempted, is this okay
    lazy val class_cast: Relation[class_cast] =
        Π[Instr[_], class_cast] {
            case checkcast(declaringMethod, _, to) => new class_cast(declaringMethod, to)
        }(
            σ(
                (_: Instr[_]) match {
                    case checkcast(_, _, ObjectType(_)) => true
                    case checkcast(_, _, ArrayType(ObjectType(_))) => true
                    case _ => false
                }
            )(instructions)
        )

    // TODO can we find a better name for the dependency than instanceof
    lazy val instanceof: Relation[sae.bytecode.model.dependencies.instanceof] =
        Π[Instr[_], sae.bytecode.model.dependencies.instanceof] {
            case sae.bytecode.model.instructions.instanceof(declaringMethod, _, typ) =>
                sae.bytecode.model.dependencies.instanceof(declaringMethod, typ)
        }(σ[sae.bytecode.model.instructions.instanceof](instructions))


    lazy val create: Relation[create] =
        Π[Instr[_], create] {
            case `new`(declaringMethod, _, typ) => new create(declaringMethod, typ)
        }(σ[`new`](instructions))

    lazy val create_class_array: Relation[create_class_array] =
        Π[Instr[_], create_class_array] {
            case newarray(declaringMethod, _, typ@ObjectType(_)) => new create_class_array(declaringMethod, typ)
        }(
            σ(
                (_: Instr[_]) match {
                    case newarray(_, _, ObjectType(_)) => true
                    case _ => false
                }
            )(instructions)
        )

    private def classAdder = new Java6ClassTransformer(
        declared_classes.element_added,
        declared_methods.element_added,
        declared_fields.element_added,
        classes.element_added,
        methods.element_added,
        fields.element_added,
        instructions.element_added,
        `extends`.element_added,
        implements.element_added,
        parameter.element_added,
        exception_handlers.element_added,
        thrown_exceptions.element_added,
        internal_inner_classes.element_added,
        internal_enclosing_methods.element_added
    )


    private def classRemover = new Java6ClassTransformer(
        declared_classes.element_removed,
        declared_methods.element_removed,
        declared_fields.element_removed,
        classes.element_removed,
        methods.element_removed,
        fields.element_removed,
        instructions.element_removed,
        `extends`.element_removed,
        implements.element_removed,
        parameter.element_removed,
        exception_handlers.element_removed,
        thrown_exceptions.element_removed,
        internal_inner_classes.element_removed,
        internal_enclosing_methods.element_removed
    )


    /**
     * Read a stream as a jar file and return the appropriate transformer
     */
    def transformerForArchiveStream(stream: InputStream) = {
        val transformer = classAdder
        val reader = new BytecodeReader(transformer)
        reader.readArchive(stream)
        transformer
    }

    /**
     * Read from a list of .jar file streams and return the appropriate transformer
     */
    def transformerForArchiveStreams(streams: Seq[InputStream]) = {
        val transformer = classAdder
        val reader = new BytecodeReader(transformer)
        streams.foreach(reader.readArchive(_))
        transformer
    }


    /**
     * Read from a list of .jar file streams and return the appropriate transformer
     */
    def transformerForArchiveResources(names: Seq[String]) = {
        transformerForArchiveStreams(
            names.map(this.getClass.getClassLoader.getResourceAsStream(_))
        )
    }

    /**
     * Convenience method that opens a stream from a resource in the class path
     */
    def transformerForArchiveResource(name: String): Java6ClassTransformer = {
        val stream = this.getClass.getClassLoader.getResourceAsStream(name)
        transformerForArchiveStream(stream)
    }

    /**
     * Read a stream as a single .class file and return the appropriate transformer
     */
    def transformerForClassfileStream(stream: InputStream) = {
        val transformer = classAdder
        val reader = new BytecodeReader(transformer)
        reader.readClassFile(stream)
        transformer
    }

    /**
     * Read from a list of .class file streams and return the appropriate transformer
     */
    def transformerForClassfileStreams(streams: Seq[InputStream]) = {
        val transformer = classAdder
        val reader = new BytecodeReader(transformer)
        streams.foreach(reader.readClassFile(_))
        transformer
    }

    def transformerForClassfileResources(names: Seq[String]): Java6ClassTransformer = {
        val streams = names.map(this.getClass.getClassLoader.getResourceAsStream(_))
        transformerForClassfileStreams(streams)
    }

    /**
     * Convenience method that opens a stream from a given loaded class
     */
    def transformerForClass[T](clazz: Class[T]): Java6ClassTransformer = {
        val name = clazz.getName.replace(".", "/") + ".class"
        val stream = clazz.getClassLoader.getResourceAsStream(name)
        transformerForClassfileStream(stream)
    }

    /**
     * Convenience method that opens a stream from a given loaded class
     */
    def transformerForClasses(classes: Array[Class[_]]): Java6ClassTransformer = {
        val streams = classes.map(
            (clazz: Class[_]) =>
                (clazz.getClassLoader.getResourceAsStream(
                    clazz.getName.replace(
                        ".",
                        "/"
                    ) + ".class"
                ))
        )
        transformerForClassfileStreams(streams)
    }


    /**
     * Convenience method that opens a stream from a resource in the class path
     */
    def addArchiveAsResource(name: String) {
        val transformer = classAdder
        val reader = new BytecodeReader(transformer)
        val stream = this.getClass.getClassLoader.getResourceAsStream(name)
        reader.readArchive(stream)
        transformer.processAllFacts()
    }


    /**
     * Convenience method that opens a stream from a file in the file system
     */
    def addArchiveAsFile(name: String) {
        val transformer = classAdder
        val reader = new BytecodeReader(transformer)
        reader.readArchive(new java.io.File(name))
        transformer.processAllFacts()
    }

    /**
     * Read a jar archive from the stream.
     * The underlying data is assumed to be in zip (jar) format
     */
    def addArchive(stream: java.io.InputStream) {
        val transformer = classAdder
        val reader = new BytecodeReader(transformer)
        reader.readArchive(stream)
        transformer.processAllFacts()
    }

    def removeArchive(stream: java.io.InputStream) {
        val transformer = classRemover
        val reader = new BytecodeReader(transformer)
        reader.readArchive(stream)
        transformer.processAllFacts()
    }

    def getAddClassFileFunction: (File) => Unit = {

        //addReader.readClassFile(file)
        val f = (x: File) => {
            val transformer = classAdder
            val addReader = new BytecodeReader(transformer)
            addReader.readClassFile(x)
            transformer.processAllFacts()
        }
        f
    }

    def getRemoveClassFileFunction: (File) => Unit = {

        val f = (x: File) => {
            val transformer = classRemover
            val removeReader = new BytecodeReader(transformer)
            removeReader.readClassFile(x)
            transformer.processAllFacts()
        }
        f
    }

    def addClassFile(stream: java.io.InputStream) {
        val transformer = classAdder
        val reader = new BytecodeReader(transformer)
        reader.readClassFile(stream)
        transformer.processAllFacts()
    }

    def removeClassFile(stream: java.io.InputStream) {
        val transformer = classRemover
        val reader = new BytecodeReader(transformer)
        reader.readClassFile(stream)
        transformer.processAllFacts()
    }

}