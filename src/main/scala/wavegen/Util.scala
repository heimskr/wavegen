package wavegen

import chisel3._
import chisel3.util._

object Util {
	def transmitPulseFrom(pulse: Bool, sourceClock: Clock, depth: Int = 2): Bool = {
		val pipe = RegInit(VecInit.fill(depth)(false.B))

		pipe(0) := pulse

		for (i <- 1 until depth) {
			pipe(i) := pipe(i - 1)
		}

		val outPulse = RegInit(false.B)

		outPulse := false.B

		withClock (sourceClock) {
			when (pipe.reduceTree((a, b) => a || b)) {
				outPulse := true.B
			}
		}

		outPulse
	}

	def transmitPulseTo(pulse: Bool, destinationClock: Clock, depth: Int = 2): Bool = {
		val pipe = RegInit(VecInit.fill(depth)(false.B))

		pipe(0) := pulse

		for (i <- 1 until depth) {
			pipe(i) := pipe(i - 1)
		}

		val outPulse = RegInit(false.B)

		outPulse := pipe.reduceTree((a, b) => a || b)

		withClock (destinationClock) {
			outPulse := false.B
		}

		outPulse
	}
}
