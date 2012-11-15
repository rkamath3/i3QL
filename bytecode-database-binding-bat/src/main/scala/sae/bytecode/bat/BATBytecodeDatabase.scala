/* License (BSD Style License):
 *  Copyright (c) 2009, 2011
 *  Software Technology Group
 *  Department of Computer Science
 *  Technische Universität Darmstadt
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 */
package sae.bytecode.bat

import java.io.InputStream
import java.util.zip.{ZipEntry, ZipInputStream}
import sae.{Relation, SetExtent}
import de.tud.cs.st.bat.resolved.{ArrayType, ObjectType}
import sae.bytecode.structure._
import sae.bytecode.BytecodeDatabase
import sae.syntax.sql._
import sae.bytecode.instructions._

/**
 * Created with IntelliJ IDEA.
 * User: Ralf Mitschke
 * Date: 22.08.12
 * Time: 21:08
 */

class BATBytecodeDatabase
    extends BytecodeDatabase
{


    val reader = new SAEJava6Framework (this)

    val classDeclarations = new SetExtent[ClassDeclaration]

    val methodDeclarations = new SetExtent[MethodDeclaration]

    val fieldDeclarations = new SetExtent[FieldDeclaration]

    val classInheritance = new SetExtent[InheritanceRelation]

    val interfaceInheritance = new SetExtent[InheritanceRelation]

    val code = new SetExtent[CodeInfo]

    lazy val instructions: Relation[InstructionInfo] = SELECT (*) FROM (identity[List[InstructionInfo]] _ IN instructionInfos)

    private lazy val instructionInfos: Relation[List[InstructionInfo]] = SELECT ((codeInfo: CodeInfo) => {
        var i = 0
        var index = 0
        val length = codeInfo.code.instructions.length
        var result: List[InstructionInfo] = Nil
        while (i < length) {
            val instr = codeInfo.code.instructions (i)
            if (instr != null) {
                result = (InstructionInfo (codeInfo.declaringMethod, instr, i, index)) :: result
                index += 1
            }
            i += 1
        }
        result
    }
    ) FROM code


    lazy val codeAttributes: Relation[CodeAttribute] = SELECT (
        (codeInfo: CodeInfo) => CodeAttribute (
            codeInfo.declaringMethod,
            codeInfo.code.instructions.size,
            codeInfo.code.maxStack,
            codeInfo.code.maxLocals,
            codeInfo.code.exceptionHandlers
        )
    ) FROM code

    val exceptionHandlers = new SetExtent[ExceptionHandlerInfo]

    def fieldReadInstructions = null

    def inheritance = null

    lazy val constructors: Relation[MethodDeclaration] =
        SELECT (*) FROM (methodDeclarations) WHERE (_.name == "<init>")

    lazy val invokeStatic: Relation[INVOKESTATIC] =
        SELECT ((_: InstructionInfo).asInstanceOf[INVOKESTATIC]) FROM (instructions) WHERE (_.isInstanceOf[INVOKESTATIC])

    lazy val invokeVirtual: Relation[INVOKEVIRTUAL] =
        SELECT ((_: InstructionInfo).asInstanceOf[INVOKEVIRTUAL]) FROM (instructions) WHERE (_.isInstanceOf[INVOKEVIRTUAL])

    lazy val invokeInterface: Relation[INVOKEINTERFACE] =
        SELECT ((_: InstructionInfo).asInstanceOf[INVOKEINTERFACE]) FROM (instructions) WHERE (_.isInstanceOf[INVOKEINTERFACE])

    lazy val invokeSpecial: Relation[INVOKESPECIAL] =
        SELECT ((_: InstructionInfo).asInstanceOf[INVOKESPECIAL]) FROM (instructions) WHERE (_.isInstanceOf[INVOKESPECIAL])

    lazy val readField: Relation[FieldReadInstruction] =
        SELECT ((_: InstructionInfo).asInstanceOf[FieldReadInstruction]) FROM (instructions) WHERE (_.isInstanceOf[FieldReadInstruction])

    lazy val getStatic: Relation[GETSTATIC] =
        SELECT ((_: InstructionInfo).asInstanceOf[GETSTATIC]) FROM (readField) WHERE (_.isInstanceOf[GETSTATIC])

    lazy val getField: Relation[GETFIELD] =
        SELECT ((_: InstructionInfo).asInstanceOf[GETFIELD]) FROM (readField) WHERE (_.isInstanceOf[GETFIELD])

    lazy val writeField: Relation[FieldWriteInstruction] =
        SELECT ((_: InstructionInfo).asInstanceOf[FieldWriteInstruction]) FROM (instructions) WHERE (_.isInstanceOf[FieldWriteInstruction])

    lazy val putStatic: Relation[PUTSTATIC] =
        SELECT ((_: InstructionInfo).asInstanceOf[PUTSTATIC]) FROM (writeField) WHERE (_.isInstanceOf[PUTSTATIC])

    lazy val putField: Relation[PUTFIELD] =
        SELECT ((_: InstructionInfo).asInstanceOf[PUTFIELD]) FROM (writeField) WHERE (_.isInstanceOf[PUTFIELD])

    def addClassFile(stream: InputStream) {
        reader.ClassFile (() => stream)
    }

    def removeClassFile(stream: InputStream) {

    }

    def addArchive(stream: InputStream) {
        val zipStream: ZipInputStream = new ZipInputStream (stream)
        var zipEntry: ZipEntry = null
        while ((({
            zipEntry = zipStream.getNextEntry
            zipEntry
        })) != null)
        {
            if (!zipEntry.isDirectory && zipEntry.getName.endsWith (".class")) {
                addClassFile (new ZipStreamEntryWrapper (zipStream, zipEntry))
            }
        }
        ObjectType.cache.clear ()
        ArrayType.cache.clear ()
    }

    def removeArchive(stream: InputStream) {

    }


}
