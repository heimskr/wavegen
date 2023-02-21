package wavegen.gameboy

import chisel3._
import chisel3.util._

class FrequencySweeper extends Module {
	val io = IO(new Bundle {
		val tick        = Input(Bool())
		val trigger     = Input(Bool())
		val period      = Input(UInt(3.W))
		val negate      = Input(Bool())
		val shift       = Input(UInt(3.W))
		val frequencyIn = Input(UInt(11.W))
		val out         = Output(UInt(11.W))
		val info        = Output(UInt(8.W))
	})

	val sweepEnabled = RegInit(false.B)
	val shadowFreq   = RegInit(0.U(11.W))
	val frequency    = RegInit(0.U(11.W))
	val sweepTimer   = RegInit(0.U(4.W))

	val info = RegInit(0.U(8.W))

	def calcFrequency(): UInt = {
		val newFrequency = Wire(UInt(11.W))

		when (io.negate) {
			newFrequency := shadowFreq - (shadowFreq >> io.shift)
		} .otherwise {
			newFrequency := shadowFreq + (shadowFreq >> io.shift)
		}

		when (2047.U < newFrequency) {
			sweepEnabled := false.B
		}

		newFrequency
	}

	val newInfo = Wire(UInt(8.W))

	newInfo := 1.U

	when (io.tick) {
		newInfo := 2.U
		when (0.U < sweepTimer) {
			newInfo := 3.U
			sweepTimer := sweepTimer - 1.U
		}

		when (sweepTimer === 0.U) {
			newInfo := 4.U
			when (0.U < io.period) {
				newInfo := 5.U
				sweepTimer := io.period
			} .otherwise {
				newInfo := 6.U
				sweepTimer := 8.U
			}

			when (sweepEnabled && 0.U < io.period) {
				newInfo := 7.U
				val newFrequency = calcFrequency()
				when (newFrequency <= 2047.U && 0.U < io.shift) {
					newInfo := 8.U
					frequency := newFrequency
					shadowFreq := newFrequency
					calcFrequency() // For overflow check
				}
			}
		}
	}

	when (io.trigger) {
		newInfo := 9.U
		shadowFreq := io.frequencyIn

		when (io.period === 0.U) {
			newInfo := 10.U
			sweepTimer := 8.U
		} .otherwise {
			newInfo := 11.U
			sweepTimer := io.period
		}

		when (io.period =/= 0.U || io.shift =/= 0.U) {
			newInfo := 12.U
			sweepEnabled := true.B
		}

		when (io.shift =/= 0.U) {
			newInfo := 13.U
			calcFrequency()
		}
	}

	when (newInfo =/= info) {
		// printf(cf"FrequencySweeper info: $info -> $newInfo\n")
		info := newInfo
	}

	io.out := frequency
	io.info := newInfo
}
