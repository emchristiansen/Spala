package spala

import breeze.linalg._
import scalatestextra._
import com.sksamuel.scrimage.Image
import com.sksamuel.scrimage.PixelTools

/////////////////////////////

trait Util {
  def randomPatch(width: Int, image: Image): Image = {
    val random = new util.Random
    val awt = image.awt.getSubimage(
      random.nextInt.abs % (image.width - width),
      random.nextInt.abs % (image.height - width),
      width,
      width)
    new Image(awt)
  }

  def imageToVector(image: Image): DenseVector[Double] = {
    def gray = (pixel: Int) =>
      (PixelTools.red(pixel) +
        PixelTools.green(pixel) +
        PixelTools.blue(pixel)) / 3

    new DenseVector(image.pixels.map(gray).map(_.toDouble))
  }
}

case class UnitVector(data: DenseVector[Double]) {
  assertNear(data.norm(2), 1)
}

object UnitVector {
  def normalize(vector: DenseVector[Double]): UnitVector =
    UnitVector(vector / vector.norm(2))

  implicit def unitVector2DenseVector(unitVector: UnitVector) = unitVector.data
}

trait OrthogonalMatchingPursuit {
  type Atom = UnitVector
  type Dictionary = IndexedSeq[Atom]
  type ActiveSet = Set[Int]

  /**
   * The dictionary element with the greatest magnitude dot product with
   * the residual.
   */
  def bestAtom(
    dictionary: Dictionary,
    residual: DenseVector[Double]): Int = {
    val scores: Seq[Double] = (0 until dictionary.size) map { atomIndex =>
      (dictionary(atomIndex) dot residual).abs
    }
    scores.zipWithIndex.maxBy(_._1)._2
  }

  /**
   * Finds the source vector with minimum error, assuming the only non-zero
   * components are given by the active set.
   */
  def bestSource(
    dictionary: Dictionary,
    observation: DenseVector[Double],
    activeSet: ActiveSet): DenseVector[Double] = {
    if (activeSet.isEmpty) DenseVector.zeros[Double](dictionary.size)
    else {
      val activeSeq = activeSet.toSeq
      val activeAtoms = activeSeq map (dictionary.apply)
      val activeMatrix = {
        val matrix = DenseMatrix.zeros[Double](observation.size, activeSet.size)
        activeAtoms.zipWithIndex foreach {
          case (atom, index) => matrix(::, index) := atom.data
        }
        matrix
      }

      val weights = pinv(activeMatrix) * observation
      assert(weights.size == activeSeq.size)

      val source = DenseVector.zeros[Double](dictionary.size)
      (weights.toArray, activeSeq).zipped map {
        case (weight, index) => source(index) = weight
      }
      source
    }

  }

  def getEstimate(
    dimension: Int,
    dictionary: Dictionary,
    source: DenseVector[Double]): DenseVector[Double] =
    ((dictionary, source.toArray).zipped map {
      case (atom, weight) => atom * weight
    }).fold(DenseVector.zeros[Double](dimension)) { _ + _ }

  def updateActiveSet(
    dictionary: Dictionary,
    observation: DenseVector[Double],
    activeSet: ActiveSet): ActiveSet = {
    val source = bestSource(dictionary, observation, activeSet)
    val estimate = getEstimate(observation.size, dictionary, source)
    val residual = observation - estimate
    val atom = bestAtom(dictionary, residual)

    assert(!activeSet.contains(atom))
    activeSet + atom
  }

  /**
   * The error under the best weighting of the atoms in this active set.
   */
  def activeSetError(
    dictionary: Dictionary,
    observation: DenseVector[Double],
    activeSet: ActiveSet): Double = {
    val source = bestSource(dictionary, observation, activeSet)
    val estimate = getEstimate(observation.size, dictionary, source)
    val residual = observation - estimate
    residual.norm(2)
  }

  /**
   * The sequence of all orthogonal matching pursuit active sets, along
   * with the errors.
   */
  def ompSolutions(
    dictionary: Dictionary,
    observation: DenseVector[Double]): Stream[(ActiveSet, Double)] = {
    def helper(activeSet: ActiveSet): Stream[(ActiveSet, Double)] = {
      (activeSet, activeSetError(dictionary, observation, activeSet)) #:: {
        helper(updateActiveSet(dictionary, observation, activeSet))
      }
    }
    helper(Set[Int]())
  }
  
  def ompSolutionAtPenalty(
      penalty: Double,
      dictionary: Dictionary,
      observation: DenseVector[Double]): (ActiveSet, Double) = {
    (ompSolutions(dictionary, observation).sliding(2).dropWhile {
      case Stream((_, leftError), (_, rightError)) => 
        leftError - rightError > penalty 
    }).toStream.head.head
  }

  def updateDictionary(
      penalty: Double,
      observations: Seq[DenseVector[Double]],
      dictionary: Dictionary): Dictionary = {
    val sourceMatrix = {
      val matrix = DenseMatrix.zeros[Double](dictionary.size, observations.size)
      for ((observation, index) <- observations.zipWithIndex) {
        val (activeSet, _) = 
          ompSolutionAtPenalty(penalty, dictionary, observation)
        val source = bestSource(dictionary, observation, activeSet)
        matrix(::, index) := source
      }
      matrix
    }
    
    val observationMatrix = {
      val matrix = 
        DenseMatrix.zeros[Double](observations.head.size, observations.size)
      for ((atom, index) <- observations.zipWithIndex) {
        matrix(::, index) := atom
      }
      matrix
    }
    
    val updatedDictionaryMatrix = (observationMatrix \ sourceMatrix)
    
    ??? 
  }
  
//  /**
//   * Infers the dictionary .
//   */
//  def inferDictionary(
//    X: DenseMatrix[Double],
//    S: DenseMatrix[Double]): DenseMatrix[Double] = {
//    assert(X.cols == S.cols)
//
//    normalizeDictionary(X \ S)
//  }
}