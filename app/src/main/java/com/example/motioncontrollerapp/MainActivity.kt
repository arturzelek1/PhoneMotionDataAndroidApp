import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.motioncontrollerapp.ui.theme.MotionControllerAppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import com.google.gson.Gson

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var webSocketClient: WebSocketClient
    private var response by mutableStateOf("")

    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null

    private var accelerometerData by mutableStateOf("Brak danych")
    private var gyroscopeData by mutableStateOf("Brak danych")

    private val gson = Gson()

    // Zmienna określająca rozmiar bufora dla średniej ruchomej
    private val MOVING_AVERAGE_WINDOW_SIZE = 200

    private val accelBufferX = ArrayDeque<Float>(MOVING_AVERAGE_WINDOW_SIZE)
    private val accelBufferY = ArrayDeque<Float>(MOVING_AVERAGE_WINDOW_SIZE)
    private val accelBufferZ = ArrayDeque<Float>(MOVING_AVERAGE_WINDOW_SIZE)

    private val gyroBufferX = ArrayDeque<Float>(MOVING_AVERAGE_WINDOW_SIZE)
    private val gyroBufferY = ArrayDeque<Float>(MOVING_AVERAGE_WINDOW_SIZE)
    private val gyroBufferZ = ArrayDeque<Float>(MOVING_AVERAGE_WINDOW_SIZE)

    // Instancje filtra Kalmana dla akcelerometru i żyroskopu
    private val kalmanFilterAccelX = KalmanFilter()
    private val kalmanFilterAccelY = KalmanFilter()
    private val kalmanFilterAccelZ = KalmanFilter()
    private val kalmanFilterGyroX = KalmanFilter()
    private val kalmanFilterGyroY = KalmanFilter()
    private val kalmanFilterGyroZ = KalmanFilter()

    // Wartości filtru dolnoprzepustowego dla akcelerometru i żyroskopu
    private var lowPassAccelX = 0f
    private var lowPassAccelY = 0f
    private var lowPassAccelZ = 0f
    private var lowPassGyroX = 0f
    private var lowPassGyroY = 0f
    private var lowPassGyroZ = 0f

    //Do delayu
    private var lastGyroX = 0f
    private var lastGyroY = 0f
    private var lastGyroZ = 0f
    private val threshold = 2.0f  // Próg zmiany


    data class MotionData(
        val type: String = "motion",
        val accelX: Float,
        val accelY: Float,
        val accelZ: Float,
        val gyroX: Float,
        val gyroY: Float,
        val gyroZ: Float
    )

    // Funkcja pomocnicza do obliczania średniej ruchomej
    private fun calculateMovingAverage(buffer: ArrayDeque<Float>, newValue: Float): Float {
        if (buffer.size >= MOVING_AVERAGE_WINDOW_SIZE) buffer.removeFirst()
        buffer.addLast(newValue)
        return buffer.average().toFloat()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        webSocketClient = object : WebSocketClient(URI("ws://192.168.0.107:8080/motion")) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                println("WebSocket połączony")
            }

            override fun onMessage(message: String?) {
                message?.let { msg ->
                    coroutineScope.launch {
                        response = msg
                        println("Odebrano wiadomość: $msg")
                    }
                }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                println("WebSocket zamknięty: $reason, kod: $code")
            }

            override fun onError(ex: Exception?) {
                ex?.printStackTrace()
                println("Błąd WebSocket: ${ex?.message}")
            }
        }
        webSocketClient.connect()

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        setContent {
            MotionControllerAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        response,
                        accelerometerData,
                        gyroscopeData
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            println("Zarejestrowano akcelerometr")
        }
        gyroscope?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            println("Zarejestrowano żyroskop")
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        println("Wyrejestrowano czujniki")
    }
    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                // Filtr dolnoprzepustowy dla akcelerometru
                lowPassAccelX = lowPassFilter(event.values[0], lowPassAccelX)
                lowPassAccelY = lowPassFilter(event.values[1], lowPassAccelY)
                lowPassAccelZ = lowPassFilter(event.values[2], lowPassAccelZ)

                // Filtr Kalmana na danych z akcelerometru
                val filteredAccelX = kalmanFilterAccelX.update(lowPassAccelX)
                val filteredAccelY = kalmanFilterAccelY.update(lowPassAccelY)
                val filteredAccelZ = kalmanFilterAccelZ.update(lowPassAccelZ)

                // Średnia ruchoma na danych z akcelerometru
                val avgAccelX = calculateMovingAverage(accelBufferX, filteredAccelX)
                val avgAccelY = calculateMovingAverage(accelBufferY, filteredAccelY)
                val avgAccelZ = calculateMovingAverage(accelBufferZ, filteredAccelZ)

                val motionData = MotionData(
                    accelX = avgAccelX,
                    accelY = avgAccelY,
                    accelZ = avgAccelZ,
                    gyroX = 0f,
                    gyroY = 0f,
                    gyroZ = 0f
                )
                sendMotionDataIfChanged(motionData)

                accelerometerData = "Akcelerometr (po filtrach i średniej): x=$avgAccelX, y=$avgAccelY, z=$avgAccelZ"
                println("Wysłano dane akcelerometru: $accelerometerData")
            }
            Sensor.TYPE_GYROSCOPE -> {
                // Filtr dolnoprzepustowy dla żyroskopu
                lowPassGyroX = lowPassFilter(event.values[0], lowPassGyroX)
                lowPassGyroY = lowPassFilter(event.values[1], lowPassGyroY)
                lowPassGyroZ = lowPassFilter(event.values[2], lowPassGyroZ)

                // Filtr Kalmana na danych z żyroskopu
                val filteredGyroX = kalmanFilterGyroX.update(lowPassGyroX)
                val filteredGyroY = kalmanFilterGyroY.update(lowPassGyroY)
                val filteredGyroZ = kalmanFilterGyroZ.update(lowPassGyroZ)

                // Średnia ruchoma na danych z żyroskopu
                val avgGyroX = calculateMovingAverage(gyroBufferX, filteredGyroX)
                val avgGyroY = calculateMovingAverage(gyroBufferY, filteredGyroY)
                val avgGyroZ = calculateMovingAverage(gyroBufferZ, filteredGyroZ)

                val motionData = MotionData(
                    accelX = 0f,
                    accelY = 0f,
                    accelZ = 0f,
                    gyroX = avgGyroX,
                    gyroY = avgGyroY,
                    gyroZ = avgGyroZ
                )
                sendMotionDataIfChanged(motionData)

                gyroscopeData = "Żyroskop (po filtrach i średniej): x=$avgGyroX, y=$avgGyroY, z=$avgGyroZ"
                println("Wysłano dane żyroskopu: $gyroscopeData")
            }
        }
    }

/*
    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                // Filtr dolnoprzepustowy dla akcelerometru
                lowPassAccelX = lowPassFilter(event.values[0], lowPassAccelX)
                lowPassAccelY = lowPassFilter(event.values[1], lowPassAccelY)
                lowPassAccelZ = lowPassFilter(event.values[2], lowPassAccelZ)

                // Filtr Kalmana na danych z akcelerometru
                val filteredAccelX = kalmanFilterAccelX.update(lowPassAccelX)
                val filteredAccelY = kalmanFilterAccelY.update(lowPassAccelY)
                val filteredAccelZ = kalmanFilterAccelZ.update(lowPassAccelZ)

                val motionData = MotionData(
                    accelX = filteredAccelX,
                    accelY = filteredAccelY,
                    accelZ = filteredAccelZ,
                    gyroX = 0f,
                    gyroY = 0f,
                    gyroZ = 0f
                )
                sendMotionDataIfChanged(motionData)

                accelerometerData = "Akcelerometr (po filtrach): x=$filteredAccelX, y=$filteredAccelY, z=$filteredAccelZ"
                println("Wysłano dane akcelerometru: $accelerometerData")
            }
            Sensor.TYPE_GYROSCOPE -> {
                // Filtr dolnoprzepustowy dla żyroskopu
                lowPassGyroX = lowPassFilter(event.values[0], lowPassGyroX)
                lowPassGyroY = lowPassFilter(event.values[1], lowPassGyroY)
                lowPassGyroZ = lowPassFilter(event.values[2], lowPassGyroZ)

                // Filtr Kalmana na danych z żyroskopu
                val filteredGyroX = kalmanFilterGyroX.update(lowPassGyroX)
                val filteredGyroY = kalmanFilterGyroY.update(lowPassGyroY)
                val filteredGyroZ = kalmanFilterGyroZ.update(lowPassGyroZ)

                val motionData = MotionData(
                    accelX = 0f,
                    accelY = 0f,
                    accelZ = 0f,
                    gyroX = filteredGyroX,
                    gyroY = filteredGyroY,
                    gyroZ = filteredGyroZ
                )
                sendMotionDataIfChanged(motionData)

                gyroscopeData = "Żyroskop (po filtrach): x=$filteredGyroX, y=$filteredGyroY, z=$filteredGyroZ"
                println("Wysłano dane żyroskopu: $gyroscopeData")
            }
        }
    }
*/
    private fun lowPassFilter(input: Float, output: Float, alpha: Float = 0.05f): Float {
        return output + alpha * (input - output)
    }

    private fun sendMotionData(motionData: MotionData) {
        if (webSocketClient.isOpen) {

            val json = gson.toJson(motionData)
            webSocketClient.send(json)
        } else {
            println("WebSocket jest zamknięty, nie można wysłać danych.")
        }
    }


    private fun sendMotionDataIfChanged(motionData: MotionData) {
        if (webSocketClient.isOpen) {
            if (Math.abs(motionData.gyroX - lastGyroX) > threshold ||
                Math.abs(motionData.gyroY - lastGyroY) > threshold ||
                Math.abs(motionData.gyroZ - lastGyroZ) > threshold) {

                lastGyroX = motionData.gyroX
                lastGyroY = motionData.gyroY
                lastGyroZ = motionData.gyroZ

                coroutineScope.launch {
                    val json = gson.toJson(motionData)
                    webSocketClient.send(json)
                }
            }
        } else {
            println("WebSocket jest zamknięty, nie można wysłać danych.")
        }
    }


    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        webSocketClient.close()
        println("Zamknięto połączenie WebSocket")
    }
}

class KalmanFilter {
    private var q = 0.05f
    private var r = 0.05f
    private var p = 0.1f
    private var x = 0.0f
    private var k = 0.0f

    fun update(measurement: Float): Float {
        p += q
        k = p / (p + r)
        x += k * (measurement - x)
        p *= (1 - k)
        return x
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    response: String,
    accelerometerData: String,
    gyroscopeData: String
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(text = "Kontroler Ruchu", style = MaterialTheme.typography.titleLarge)
        Text(text = "Odczyty z czujników:", style = MaterialTheme.typography.titleMedium)

        Text(text = accelerometerData, modifier = Modifier.padding(top = 8.dp))
        Text(text = gyroscopeData, modifier = Modifier.padding(top = 8.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MotionControllerAppTheme {
        MainScreen(response = "", accelerometerData = "Akcelerometr", gyroscopeData = "Żyroskop")
    }
}
