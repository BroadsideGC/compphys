package compphys

import com.github.sh0nk.matplotlib4j.Plot
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.omg.SendingContext.RunTime
import java.security.SecureRandom


class Species(var fitness: Double, val neighbors: List<Int>) {
    val activityP = mutableListOf<Int>()
    val activityS = mutableListOf<Int>()
    val mutationsNumber = mutableListOf<Int>()
}

val rnd = SecureRandom()
val minFitnessesList = mutableListOf<Double>()

class World(val speciesList: List<Species>) {


    private fun getMinIndex(): Int {
        val minFitness = speciesList.minBy { it.fitness }!!.fitness
        val withMinFitness = mutableListOf<Int>()
        speciesList.forEachIndexed { index, species -> if (species.fitness == minFitness) withMinFitness.add(index) }
        return withMinFitness[rnd.nextInt(withMinFitness.size)]
    }

    private fun processStep(step: Int) {
        val ind = getMinIndex()
        //println("Species $ind: ${speciesList[ind].fitness}")
        minFitnessesList.add(speciesList[ind].fitness)
        speciesList[ind].activityP.add(step)
        speciesList[ind].fitness = rnd.nextDouble()

        speciesList[ind].neighbors.forEach {
            speciesList[it].fitness = rnd.nextDouble()
            speciesList[it].activityS.add(step)
        }
        speciesList.forEach { it.mutationsNumber.add(it.activityP.size + it.activityS.size) }
    }

    suspend fun run(steps: Int = 20000) = coroutineScope{
        (1..steps).forEach {
            processStep(it)
        }
        val c1 = async {
            val plt1 = Plot.create()
            val fli = mutableListOf(0)
            val fl = mutableListOf(0.0)
            minFitnessesList.forEachIndexed { index, d ->
                if (d > fl.last()) {
                    fl.add(d)
                    fli.add(index)
                }
            }
            fl.add(fl.last())
            fli.add(steps)
            plt1.plot().add((1..steps).toMutableList(), minFitnessesList, ".")
            plt1.plot().add(fli.toList(), fl.toList(), "r")
            plt1.xlabel("Steps")
            plt1.ylabel("Min Fitness")
            plt1.title("Min fitness plot")
            plt1.show()
        }
        val c2 = async {
            val plt2 = Plot.create()
            speciesList.forEachIndexed { index, species ->
                if (species.activityS.isNotEmpty()) {
                    plt2.plot().add(Array(species.activityS.size, { index + 1 }).toList(), species.activityS, "y.")
                }
                if (species.activityP.isNotEmpty()) {
                    plt2.plot().add(Array(species.activityP.size, { index + 1 }).toList(), species.activityP, "r.")
                }
            }
            plt2.ylabel("Steps")
            plt2.xlabel("Species")
            plt2.title("Species activity plot")
            plt2.show()
        }
        val c3 = async {
            val plt3 = Plot.create()
            plt3.plot().add((1..steps).toMutableList(), speciesList[rnd.nextInt(speciesList.size)].mutationsNumber)
            plt3.ylabel("Mutations")
            plt3.xlabel("Steps")
            //plt3.title("Species activity plot")
            plt3.show()
        }
        c2.await()
        c3.await()
        c1.await()
    }
}

fun main(args: Array<String>) = runBlocking {
    val s = mutableListOf<Species>()
    val n = 1000
    (0 until n).forEach {
        when (it) {
            0 -> s.add(Species(rnd.nextDouble(), listOf(1, n - 1)))
            n - 1 -> s.add(Species(rnd.nextDouble(), listOf(n - 2, 0)))
            else -> s.add(Species(rnd.nextDouble(), listOf(it - 1, it + 1)))
        }
    }

    val world = World(s.toList())
    world.run(20000)


}

