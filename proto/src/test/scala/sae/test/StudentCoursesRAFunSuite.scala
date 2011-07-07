package sae.test

import sae.syntax.RelationalAlgebraSyntax._
import sae.collections._

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import sae.LazyView

/**
 * Test the basic functions of the framework with an example database of students and courses.
 * These tests directly use the relational algebra (RA) operators.
 */
@RunWith(classOf[JUnitRunner])
class StudentCoursesRAFunSuite
        extends FunSuite {

    val database = new StudentCoursesDatabase()
    import database._


    test("indexing") {
        val students = database.students.copy
        val indexed_students = sae.operators.Conversions.lazyViewToIndexedView(students) // make a local copy

        val index = indexed_students.index(identity)
        val nameIndex = indexed_students.index( (_:Student).Name )

        val judy = Student(21111, "judy")
        // we add before accessing the index to test cache semantics
        students += judy
        assert( 3 === index.asList.size )
        assert( 3 === nameIndex.asList.size )

        val john = Student(21111, "john")
        students += john

        assert( 4 === index.asList.size )
        assert( 1 === index.elementCountAt(john) )
        assert( 4 === nameIndex.asList.size )
        assert( 2 === nameIndex.elementCountAt("john") )

        students += john

        assert( 5 === index.asList.size )
        assert( 2 === index.elementCountAt(john) )
        assert( 5 === nameIndex.asList.size )
        assert( 3 === nameIndex.elementCountAt("john") )

    }

    test("selection") {
        assert(2 === students.size)

        // johnsData(StudentId, john) :- student(StudentId, john)
        // TODO this should type now
        val johnsData : QueryResult[Student] = σ((_ : Student).Name == "john")(students)
        assert(1 === johnsData.size)
        assert(Some(john) === johnsData.singletonValue)
    }

    test("maintain selection") {
        // johnsData(StudentId, john) :- student(StudentId, john)

        val students = database.students.copy // make a local copy

        val johnsData : QueryResult[Student] = σ( (_:Student).Name == "john")(students)
        assert(1 === johnsData.size)
        assert(Some(john) === johnsData.singletonValue)

        val otherJohn = Student(11111, "john")
        students += otherJohn

        assert(2 === johnsData.size)
        val twoJohns = johnsData.asList
        assert(twoJohns.contains(john))
        assert(twoJohns.contains(otherJohn))

        students -= john
        assert(1 === johnsData.size)

        students -= otherJohn
        assert(0 === johnsData.size)

        students -= john
        assert(0 === johnsData.size)
    }

    test("projection") {
        // names(Name) :- student(_, Name)
        val names : QueryResult[String] = Π((_ : Student).Name)(students)

        // the type inference is not strong enough, either we need to supply function argument types of the whole function or of the lambda expression (see below) 
        // val names : Relation[String] = Π( (s:Student) => (s.Name), students)

        assert(students.size === names.size)
        val nameList = names.asList
        students.foreach(s =>
            {
                nameList.contains(s.Name)
            }
        )
    }

    test("maintain distinct projection") {
        val students = database.students.copy // make a local copy
        // names(Name) :- student(_, Name)
        val names : QueryResult[String] = δ(Π((_ : Student).Name)(students))

        // the type inference is not strong enough, either we need to supply function argument types of the whole function or of the lambda expression (see below) 
        // val names : Relation[String] = Π( (s:Student) => (s.Name), students)

        assert(2 === names.size)

        val otherJohn = Student(11111, "john")
        students += otherJohn
        assert(2 === names.size)

        val judy = Student(21111, "judy")
        students += judy
        assert(3 === names.size)

        students -= john
        assert(3 === names.size)
        students -= otherJohn
        assert(2 === names.size)

        students -= sally
        assert(1 === names.size)

        students -= judy
        assert(0 === names.size)
    }

    test("optimized projection") {
        // names(StudentId) :- student(StudentId, john)
        // there is an inner projection that does nothing
        val students = database.students.copy
        def fun(x : Student) = x.Name
        val names : LazyView[String] = Π[Student, String](_.Name)(Π[Student, Student](s => s)(students))

        /*
		 * FIXME
		val optimized = ExecutionPlan(names)

		assert( optimized ne names)
		
		val directNames : LazyView[String] = Π[Student, String](fun, students)
		assert( !(optimized match { case Π(_, Π(_,_)) => true; case _ => false }) )
		 */
    }

    test("cross product") {
        // println("test cross product")
        // student_courses(StudentId, SName, CourseId, CName) :- student(StudentId, SName), course(CourseId, CName).
        // actually what we get looks more like the following  query.
        // student_courses(student(StudentId, SName),course(CourseId, CName)) :- student(StudentId, SName), course(CourseId, CName).

        val student_courses : QueryResult[(Student, Course)] = students × courses

        assert(student_courses.size === students.size + courses.size)
        // assert( student_courses.arity === students.arity + courses.arity)

        // the cross product should contain every "student course" pair
        val resultList = student_courses.asList
        students.foreach(student =>
            {
                val onestudents_courses = resultList.filter({ case (s, c) => s == student })
                courses.foreach(course =>
                    onestudents_courses.contains(course)
                )
            }
        )
        courses.foreach(course =>
            {
                val onecourses_students = resultList.filter({ case (s, c) => c == course })
                students.foreach(student =>
                    onecourses_students.contains(student)
                )
            }
        )
    }

    test("joins") {
        // println("test joins")
        // val course_for_student = students ⋈ ((_ : (Student, Enrollment)) match { case (s, e) => s.Id == e.StudentId }, enrollments)

        val course_for_student = ((students, students.Id) ⋈ (enrollments.StudentId, enrollments)) { (s:Student, e:Enrollment ) => (s,e)}

        val eise_students : QueryResult[(Student, Enrollment)] = σ((e:(Student, Enrollment)) => e._2.CourseId == eise.Id)(course_for_student)

        val sed_students : QueryResult[(Student, Enrollment)] = σ((e:(Student, Enrollment)) => e._2.CourseId == sed.Id)(course_for_student)

        // john and sally are registered for eise
        // sally is registered for sed

        assert(course_for_student.size === 3)

        val eiseResultList = eise_students.asList

        assert(eise_students.size === 2)
        students.foreach(student =>
            {
                assert(eiseResultList.exists({ case (s, e) => s == student }))
            }
        )
        assert(sed_students.asList.exists({ case (s, e) => s == sally }))
    }

    test("consecutive joins") {
        // println("test consecutive joins")

        val course_for_student = students ⋈ ((t : (Student, Enrollment)) => t match { case (s, e) => s.Id == e.StudentId }, enrollments) ⋈ ((t : ((Student, Enrollment), Course)) => t._1._2.CourseId == t._2.Id, courses)

        val eise_students : QueryResult[((Student, Enrollment), Course)] = σ( (e :((Student, Enrollment), Course)) => e._2.Id == eise.Id)(course_for_student)

        val sed_students : QueryResult[((Student, Enrollment), Course)] = σ( (e :((Student, Enrollment), Course)) => e._2.Id == sed.Id)(course_for_student)

        // john and sally are registered for eise
        // sally is registered for sed

        // course_for_student.foreach(println)

        assert(course_for_student.size === 3)

        val eiseResultList = eise_students.asList

        assert(eise_students.size === 2)
        students.foreach(student =>
            {
                assert(eiseResultList.exists({ case ((s, e), c) => s == student }))
            }
        )
        assert(sed_students.asList.exists({ case ((s, e), c) => s == sally }))

    }

    test("equi join") {
        val student_names_to_courseId =
            /*
            // version with infix notation on keys and second relation
            (students ⋈ (students.Id , enrollments.StudentId)) (enrollments) {(s:Student, e:Enrollment) => (s.Name, e.CourseId)} 
             */
            // version with double parameter infix on key + relation
            ((students, students.Id) ⋈ (enrollments.StudentId, enrollments)) { (s : Student, e : Enrollment) => (s, e.CourseId) }

        assert(student_names_to_courseId.size === 3)
        
        val student_courses = ((student_names_to_courseId, (_ : (Student, Integer))._2) ⋈ (courses.Id, courses)) { (e : (Student, Integer), c : Course) => (e._1, c) }
        
        //student_courses.foreach( println )
        
        assert(student_courses.size === 3)
        
        val list = student_courses.asList
        
        assert(list.contains((john, eise)))
        assert(list.contains((sally, eise)))
        assert(list.contains((sally, sed)))
    }
    
    test("maintain equi join") {
        val student_names_to_courseId =
            ((students, students.Id) ⋈ (enrollments.StudentId, enrollments)) { (s : Student, e : Enrollment) => (s, e.CourseId) }

        assert(student_names_to_courseId.size === 3)
        
        val student_courses : QueryResult[(Student, Course)]= ((student_names_to_courseId, (_ : (Student, Integer))._2) ⋈ (courses.Id, courses)) { (e : (Student, Integer), c : Course) => (e._1, c) }
        
        //student_courses.foreach( println )
        
        assert(student_courses.size === 3)
        
       
        enrollments += Enrollment(john.Id, sed.Id)
        
        assert(student_courses.size === 4)
        assert(student_courses.asList.contains((john, sed)))

        enrollments += Enrollment(john.Id, sed.Id)
        assert(student_courses.size === 5)
        assert(student_courses.asList.contains((john, sed)))

        enrollments -= Enrollment(john.Id, sed.Id)
        enrollments -= Enrollment(john.Id, sed.Id)
        assert(student_courses.size === 3)
        assert(!student_courses.asList.contains((john, sed)))



        enrollments -= Enrollment(sally.Id, sed.Id)
        
        assert(student_courses.size === 2)
        assert(!student_courses.asList.contains((sally, sed)))

    }


    test("type filter")
    {
        // implicit val m : ClassManifest[Employee] = ClassManifest.fromClass( Employee.getClass() )
        val employees : QueryResult[Person] = σ[Employee](persons)
        assert(employees.size === 2)
        assert(employees.asList.contains(Employee("Johannes")))
        assert(employees.asList.contains(Employee("Christian")))

        val students : QueryResult[Person] = σ[Student](persons)
        assert(students.size === 2)
        assert(students.asList.contains(john))
        assert(students.asList.contains(sally))
    }

    test("type projection")
    {
        val employees : QueryResult[Employee] = Π[Employee](σ[Employee](persons))
        assert(employees.size === 2)
        assert(employees.asList.contains(Employee("Johannes")))
        assert(employees.asList.contains(Employee("Christian")))

        val students : QueryResult[Student] = Π[Student](σ[Student](persons))
        assert(students.size === 2)
        assert(students.asList.contains(john))
        assert(students.asList.contains(sally))
    }


    test("set difference")
    {
        val persons = database.persons.copy

        val employees = σ[Employee](persons)

        val students : QueryResult[Person] = persons ∖ employees

        assert(students.size === 2)
        assert(students.asList.contains(john))
        assert(students.asList.contains(sally))

        val heather = Student(25421,"heather")
        persons += heather

        assert(students.size === 3)
        assert(students.asList.contains(john))
        assert(students.asList.contains(sally))
        assert(students.asList.contains(heather))

        val tim = Employee("tim")

        persons += tim

        assert(students.size === 3)
        assert(students.asList.contains(john))
        assert(students.asList.contains(sally))
        assert(students.asList.contains(heather))

        val tom = Employee("tom")
        val heather_new_number =Student(23744,"heather")

        persons.update(tim, tom)

        persons.update(heather, heather_new_number)

        assert(students.size === 3)
        assert(students.asList.contains(john))
        assert(students.asList.contains(sally))
        assert(students.asList.contains(heather_new_number))


    }


    test("set intersection")
    {
        val persons = database.persons.copy
        val students = database.students.copy

        val employees = σ[Employee](persons)

        val person_students = σ[Student](persons)

        //val empty : QueryResult[Person] = person_students ∩ employees

        val student_data_as_persons : LazyView[Person] = Π( (s:Student) => s.asInstanceOf[Person] )(students)

        val intersect : QueryResult[Person] = student_data_as_persons ∩ person_students

        //assert(empty.size === 0)
        assert(intersect.size === 2)

        assert(intersect.asList.contains(john))
        assert(intersect.asList.contains(sally))

        val heather = Student(25421,"heather")
        persons += heather
        val tim = Employee("tim")
        persons += tim

        // students does not contain heather
        //assert(empty.size === 0)
        assert(intersect.size === 2)


        students += heather
        // students contains heather once, persons once
        assert(intersect.size === 3)
        assert(intersect.asList.contains(heather))

        persons += heather
        // students contains heather once, persons twice
        assert(intersect.size === 3)
        assert(intersect.asList.contains(heather))

        students += heather
        // students contains heather twice, persons twice
        assert(intersect.size === 4)
        assert(intersect.asList.contains(heather))

        // students contains heather twice, persons once
        persons -= heather
        assert(intersect.size === 3)
        assert(intersect.asList.contains(heather))

        // students contains heather twice, persons not
        persons -= heather
        assert(intersect.size === 2)
        assert(!intersect.asList.contains(heather))

        // students contains heather twice, persons once
        persons += heather
        assert(intersect.size === 3)
        assert(intersect.asList.contains(heather))


        students -= heather
        // students contains heather once, persons once
        assert(intersect.size === 3)
        assert(intersect.asList.contains(heather))


        // students contains heather once, persons not
        persons -= heather
        assert(intersect.size === 2)
        assert(!intersect.asList.contains(heather))

        persons += heather
        students -= heather
        assert(intersect.size === 2)
        assert(!intersect.asList.contains(heather))

        students += heather
        assert(intersect.size === 3)
        assert(intersect.asList.contains(heather))


        val heather_new_number =Student(23744,"heather")

        persons.update(heather, heather_new_number)

        assert(intersect.size === 2)
        assert(intersect.asList.contains(john))
        assert(intersect.asList.contains(sally))

        students.update(heather, heather_new_number)

        assert(intersect.size === 3)
        assert(intersect.asList.contains(john))
        assert(intersect.asList.contains(sally))
        assert(intersect.asList.contains(heather_new_number))
    }

    test("semi join")
    {   // looks a little bit like the intersection test, but semantics are slightly different for bags
        // i.e. having same element twice in two bags, yields two elements in intersection result
        val persons = database.persons.copy
        val students = database.students.copy

        val person_students = σ[Student](persons)

        val semijoin : QueryResult[Student] = ( (students, (_:Student).asInstanceOf[Person]) ⋉ (identity(_:Person), person_students) )

        assert(2 === semijoin.size)

        assert(semijoin.asList.contains(john))
        assert(semijoin.asList.contains(sally))

        val heather = Student(25421,"heather")
        persons += heather
        val tim = Employee("tim")
        persons += tim

        // students does not contain heather
        assert(semijoin.size === 2)


        students += heather
        // students contains heather once, persons once
        assert(semijoin.size === 3)
        assert(semijoin.asList.contains(heather))

        persons += heather
        // students contains heather once, persons twice
        assert(semijoin.size === 3)
        assert(semijoin.asList.contains(heather))

        students += heather
        // students contains heather twice, persons twice
        assert(semijoin.size === 4)
        assert(semijoin.asList.contains(heather))

        // students contains heather twice, persons once
        persons -= heather
        assert(semijoin.size === 4)
        assert(semijoin.asList.contains(heather))

        // students contains heather twice, persons not
        persons -= heather
        assert(semijoin.size === 2)
        assert(!semijoin.asList.contains(heather))

        // students contains heather twice, persons once
        persons += heather
        assert(semijoin.size === 4)
        assert(semijoin.asList.contains(heather))


        students -= heather
        // students contains heather once, persons once
        assert(semijoin.size === 3)
        assert(semijoin.asList.contains(heather))


        // students contains heather once, persons not
        persons -= heather
        assert(semijoin.size === 2)
        assert(!semijoin.asList.contains(heather))

        // students contains heather not, persons once
        persons += heather
        students -= heather
        assert(semijoin.size === 2)
        assert(!semijoin.asList.contains(heather))

        // students contains heather once, persons once
        students += heather
        assert(semijoin.size === 3)
        assert(semijoin.asList.contains(heather))


        val heather_new_number =Student(23744,"heather")

        persons.update(heather, heather_new_number)

        assert(semijoin.size === 2)
        assert(semijoin.asList.contains(john))
        assert(semijoin.asList.contains(sally))

        students.update(heather, heather_new_number)

        assert(semijoin.size === 3)
        assert(semijoin.asList.contains(john))
        assert(semijoin.asList.contains(sally))
        assert(semijoin.asList.contains(heather_new_number))
    }


    test("anti semi join")
    {
        val persons = database.persons.copy
        val students = database.students.copy

        val person_students = σ[Student](persons)

        val antisemijoin : QueryResult[Student] = ( (students, (_:Student).asInstanceOf[Person]) ⊳ (identity(_:Person), person_students) )

        assert(0 === antisemijoin.size)

        val heather = Student(25421,"heather")
        persons += heather
        val tim = Employee("tim")
        persons += tim

        // students does not contain heather
        assert(antisemijoin.size === 0)

        students += heather // students contains heather once, persons once
        assert(antisemijoin.size === 0)

        persons -= heather // students contains heather once, persons not
        assert(antisemijoin.size === 1)
        assert(antisemijoin.asList.contains(heather))

        students += heather // students contains heather twice, persons not
        assert(antisemijoin.size === 2)
        assert(antisemijoin.asList.contains(heather))

        persons += heather // students contains heather twice, persons once
        assert(antisemijoin.size === 0)

        persons -= heather // students contains heather twice, persons not
        assert(antisemijoin.size === 2)
        assert(antisemijoin.asList.contains(heather))

        persons += heather // students contains heather twice, persons once
        assert(antisemijoin.size === 0)

        students -= heather // students contains heather once, persons once
        // FIXME this does not work yet, because indices are not treated with higher priority during the maintenance process
        // thus the right hand side index is not updated when the set difference operator receives its maintenance call
        // the rest of the test does also not work, probably the error propagages

/*
        assert(antisemijoin.size === 0)

        students -= heather // students contains heather once, persons once
        assert(antisemijoin.size === 0)

        persons -= heather // students contains heather not, persons not
        assert(antisemijoin.size === 0)

        persons += heather // students contains heather not, persons once
        assert(antisemijoin.size === 0)

        students += heather // students contains heather once, persons once
        assert(antisemijoin.size === 0)

        persons += heather
        persons += heather // students contains heather not, persons thrice
        assert(antisemijoin.size === 0)

        students += heather // students contains heather once, persons thrice
        assert(antisemijoin.size === 0)

        val heather_new_number =Student(23744,"heather")

        persons.update(heather, heather_new_number)

        assert(antisemijoin.size === 1)
        assert(antisemijoin.asList.contains(heather))

        students.update(heather, heather_new_number)

        assert(antisemijoin.size === 0)
*/
    }
}