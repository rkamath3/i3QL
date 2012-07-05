package unisson.model

import de.tud.cs.st.vespucci.model.{IArchitectureModel, IConstraint, IEnsemble}

/**
 *
 * Author: Ralf Mitschke
 * Date: 04.07.12
 * Time: 15:36
 *
 */
trait IUnissonArchitectureModelDatabase extends IUnissonDatabase
{


    /**
     * Add all ensembles and constraints in the <code>model</code> to a concern identified to the models name.
     * TODO: rename to addConcern
     */
    def addModel(model: IArchitectureModel) {
        import scala.collection.JavaConversions._
        addEnsemblesToConcern(model.getEnsembles)(model.getName)
        addConstraintsToConcern(model.getConstraints)(model.getName)
    }

    /**
     * Remove all ensembles and constraints in the <code>model</code> from the concern identified to the models name.
     * TODO: rename to removeConcern
     */
    def removeModel(model: IArchitectureModel) {
        import scala.collection.JavaConversions._
        removeEnsemblesFromConcern(model.getEnsembles)(model.getName)
        removeConstraintsFromConcern(model.getConstraints)(model.getName)
    }

    /**
     * Update all ensembles and constraints in the <code>oldModel</code> to the values in the <code>newModel</code>.
     * TODO: rename to updateConcern
     */
    def updateModel(oldModel: IArchitectureModel, newModel: IArchitectureModel) {
        import scala.collection.JavaConversions._

        val oldEnsembles = oldModel.getEnsembles
        // remove old Ensembles
        removeEnsemblesFromConcern(
            oldEnsembles.filterNot(
                (e: IEnsemble) => newModel.getEnsembles.exists(_.getName == e.getName)
            )
        )(oldModel.getName)

        // remove old constraints
        for (constraint <- oldModel.getConstraints.filterNot(
            (c: IConstraint) => newModel.getConstraints.exists(_ == c)
        )
        ) {
            removeConstraintFromConcern(constraint)(oldModel.getName)
        }


        // update existing Ensembles
        for (oldE <- oldEnsembles;
             newE <- newModel.getEnsembles)
            if (oldE.getName == newE.getName
            ) {
                updateEnsembleInConcern(oldE, newE)(oldModel.getName)
            }
        // we currently do not update any constraints.
        // This would make sense only for constraint changes w.r.t. kinds,
        // but then removing the old constraint is probably as effective.

        // add new Ensembles

        addEnsemblesToConcern(
            newModel.getEnsembles.filterNot(
                (e: IEnsemble) => oldModel.getEnsembles.exists(_.getName == e.getName)
            )
        )

        // add new Constraints
        addConstraintsToConcern(
            newModel.getConstraints.filterNot(
                (c: IConstraint) => oldModel.getConstraints.exists(_ == c)
            )
        )
    }

    /**
     * Add all ensembles in the <code>model</code> and their children to the global list of defined ensembles.
     * TODO: rename to addRepository
     */
    def addGlobalModel(model: IArchitectureModel) {
        import scala.collection.JavaConversions._
        for (ensemble <- model.getEnsembles) {
            addEnsemble(ensemble)
        }
    }

    /**
     * Remove all ensembles in the <code>model</code> and their children to the global list of defined ensembles.
     * TODO: rename to removeRepository
     */
    def removeGlobalModel(model: IArchitectureModel) {
        import scala.collection.JavaConversions._

        for (ensemble <- model.getEnsembles) {
            removeEnsemble( ensemble)
        }
    }

    /**
     * Update all ensembles in the <code>model</code> and their children to the global list of defined ensembles.
     * TODO: rename to updateRepository
     */
    def updateGlobalModel(oldModel: IArchitectureModel, newModel: IArchitectureModel) {
        import scala.collection.JavaConversions._

        // remove old Ensembles
        removeEnsembles(
            oldModel.getEnsembles.filterNot(
                (e: IEnsemble) => newModel.getEnsembles.exists(_.getName == e.getName)
            )
        )
        // add new Ensembles
        addEnsembles(
            newModel.getEnsembles.filterNot(
                (e: IEnsemble) => oldModel.getEnsembles.exists(_.getName == e.getName)
            )
        )

        // update existing Ensembles
        for (oldE <- oldModel.getEnsembles;
             newE <- newModel.getEnsembles)
            if (oldE.getName == newE.getName
            ) {
                updateEnsemble(oldE, newE)
            }
    }
}