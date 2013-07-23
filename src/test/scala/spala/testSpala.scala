package spala

import org.scalacheck.Gen
import org.junit.runner.RunWith
import scalatestextra._
import org.scalatest.junit.JUnitRunner
import com.sksamuel.scrimage.Image

////////////////////////////////////////////////////////////////////////////////

@RunWith(classOf[JUnitRunner])
class TestFunGeneratorSuite extends FunGeneratorSuite {
  val image = Image(getClass.getResourceAsStream("/grass.png"))

  val trainingSet = (0 until 257) map { _ =>
    imageToVector(randomPatch(8, image).copy)
  }

  val observation = trainingSet.head
  val dictionary = trainingSet.tail.take(128).map(UnitVector.normalize)
  val otherObservations = trainingSet.drop(129)

  val genAtomIndex = Gen.posNum[Int] map { _ % 128 }
  val genActiveSet = Gen.listOf(genAtomIndex) map { _.toSet }

  test(
    "when the residual is in the dictionary, bestAtom should choose it",
    FastTest) {
      forAll(genAtomIndex) { atomIndex =>
        val residual = dictionary(atomIndex)
        val bestAtomIndex = bestAtom(dictionary, residual)
        assert(bestAtomIndex == atomIndex)
      }
    }

  test(
    "bestSource should only use those atoms allowed by the active set",
    FastTest) {
      forAll(genActiveSet) { activeSet =>
        val source = bestSource(dictionary, observation, activeSet)
        val trueActive =
          source.toArray.zipWithIndex.filter(_._1 != 0).map(_._2).toSet
        for (element <- trueActive) assert(activeSet.contains(element))
      }
    }

  test("updateActiveSet should add one element to the active set", FastTest) {
    forAll(genActiveSet) { activeSet =>
      val updated = updateActiveSet(dictionary, observation, activeSet)
      assert(updated.size == activeSet.size + 1)
      for (element <- activeSet) assert(updated.contains(element))
    }
  }

  test("activeSetError should be nonnegative", FastTest) {
    forAll(genActiveSet) { activeSet =>
      val error = activeSetError(dictionary, observation, activeSet)
      assert(error >= 0)
    }
  }

  test(
    "activeSetError should be zero when the observation is reconstructable",
    FastTest) {
      forAll(genActiveSet) { activeSet =>
        whenever(activeSet.size > 0) {
          val observation = dictionary(activeSet.head)
          val error = activeSetError(dictionary, observation, activeSet)
          assertNear(error, 0)
        }
      }
    }

  test(
    "the size of the active set in the ompSolutions stream should increment by one",
    FastTest) {
      ompSolutions(dictionary, observation).take(10).zipWithIndex foreach {
        case ((activeSet, error), index) => assert(index == activeSet.size)
      }
    }

  test(
    "the reconstruction error in ompSolutions should monotonically decrease",
    FastTest) {
      ompSolutions(dictionary, observation).map(_._2).sliding(2).take(10) foreach {
        case Stream(leftError, rightError) => {
          assert(leftError >= rightError)
        }
      }
    }

  test("updating a dictionary should not increase error", FastTest) {
    val penalty = 10
    //    val errorBefore =
    //      totalError(penalty, otherObservations, dictionary.take(16))

    val activeSets = otherObservations map { observation =>
      ompSolutionAtPenalty(penalty, dictionary.take(16), observation)._1
    }
//
//    def error(dictionary: Dictionary) = {
//      (otherObservations, activeSets).zipped map {
//        case (observation, activeSet) =>
//          activeSetError(dictionary, observation, activeSet)
//      } sum
//    }
//
//    val errorBefore = error(dictionary.take(16))
    
//    val updatedDictionary = updateDictionary(
//      penalty,
//      otherObservations,
//      dictionary.take(16),
//      activeSets)
  }

  test("a vanilla unit test", InstantTest) {
    val x = 1
    assert(x == 1)
  }

  test("a generator driven test", InstantTest) {
    val evenInts = for (n <- Gen.choose(-1000, 1000)) yield 2 * n
    forAll(evenInts) { x =>
      assert(x % 2 == 0)
    }
  }
}

@RunWith(classOf[JUnitRunner])
class TestFunGeneratorConfigSuite extends FunGeneratorConfigSuite {
  test("make sure the ConfigMap exists for a vanilla unit test") { configMap =>
    assert(configMap.toString.contains("Map"))
  }

  test("make sure the ConfigMap exists for a generator driven test") {
    configMap =>
      val evenInts = for (n <- Gen.choose(-1000, 1000)) yield 2 * n
      forAll(evenInts) { x =>
        assert(x % 2 == 0)
        assert(configMap.toString.contains("Map"))
      }
  }
}
