package com.example.tsu_navigator.ml

import android.content.Context
import android.util.Log
import org.json.JSONArray
import kotlin.math.exp

class AIClassifier(private val context: Context) {
    private lateinit var w1: Array<FloatArray>
    private lateinit var b1: FloatArray
    private lateinit var w2: Array<FloatArray>
    private lateinit var b2: FloatArray
    private lateinit var w3: Array<FloatArray>
    private lateinit var b3: FloatArray

    init {
        loadWeightsFromJson()
    }

    private fun loadWeightsFromJson() {
        try {
            val jsonString = context.assets.open("weights_5x5.json")
                .bufferedReader().use { it.readText() }
            val rootArray = JSONArray(jsonString)

            val w1Array = rootArray.getJSONArray(0)
            w1 = Array(25) { i ->
                val row = w1Array.getJSONArray(i)
                FloatArray(256) { j -> row.getDouble(j).toFloat() }
            }
            val b1Array = rootArray.getJSONArray(1)
            b1 = FloatArray(256) { i -> b1Array.getDouble(i).toFloat() }

            val w2Array = rootArray.getJSONArray(2)
            w2 = Array(256) { i ->
                val row = w2Array.getJSONArray(i)
                FloatArray(128) { j -> row.getDouble(j).toFloat() }
            }
            val b2Array = rootArray.getJSONArray(3)
            b2 = FloatArray(128) { i -> b2Array.getDouble(i).toFloat() }

            val w3Array = rootArray.getJSONArray(4)
            w3 = Array(128) { i ->
                val row = w3Array.getJSONArray(i)
                FloatArray(10) { j -> row.getDouble(j).toFloat() }
            }
            val b3Array = rootArray.getJSONArray(5)
            b3 = FloatArray(10) { i -> b3Array.getDouble(i).toFloat() }

            Log.d("AIClassifier", "веса загружены")
        } catch (e: Exception) {
            Log.e("AIClassifier", "Ошибка загрузки весов", e)
        }
    }

    fun predict(pixels: Array<BooleanArray>): Int {
        val input = FloatArray(25) { i ->
            val row = i / 5
            val col = i % 5
            if (pixels[row][col]) 1.0f else 0.0f
        }
        val h1 = FloatArray(256) { i ->
            var sum = b1[i]
            for (j in 0 until 25) sum += input[j] * w1[j][i]
            relu(sum)
        }

        val h2 = FloatArray(128) { i ->
            var sum = b2[i]
            for (j in 0 until 256) sum += h1[j] * w2[j][i]
            relu(sum)
        }

        val output = FloatArray(10) { i ->
            var sum = b3[i]
            for (j in 0 until 128) sum += h2[j] * w3[j][i]
            sum
        }

        val probs = softmax(output)
        val predicted = probs.indices.maxByOrNull { probs[it] } ?: 0
        Log.d("AIClassifier", "Вероятности: ${probs.joinToString { "%.2f".format(it) }} -> $predicted")
        return predicted
    }

    private fun relu(x: Float): Float = if (x > 0) x else 0f

    private fun softmax(x: FloatArray): FloatArray {
        val max = x.maxOrNull() ?: 0f
        val expValues = x.map { exp(it - max) }
        val sumExp = expValues.sum()
        return expValues.map { (it / sumExp).toFloat() }.toFloatArray()
    }
}