package wavegen.gameboy

import chisel3._
import chisel3.util._

class FrequencySweeper extends Module {
	val io = IO(new Bundle {
		val tick      = Input(Bool())
		val trigger   = Input(Bool())
		val period    = Input(UInt(3.W))
		val negate    = Input(Bool())
		val shift     = Input(UInt(3.W))
		val out       = Output(UInt(11.W))
	})

	val sweepEnabled = RegInit(false.B)
	val shadowFreq   = RegInit(0.U(11.W))
	val frequency    = RegInit(0.U(11.W))
	val sweepTimer   = RegInit(0.U(4.W))

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

	when (io.tick) {
		when (0.U < sweepTimer) {
			sweepTimer := sweepTimer - 1.U
		}

		when (sweepTimer === 0.U) {
			when (0.U < io.period) {
				sweepTimer := io.period
			} .otherwise {
				sweepTimer := 8.U
			}

			when (sweepEnabled && 0.U < io.period) {
				val newFrequency = calcFrequency()
				when (newFrequency <= 2047.U && 0.U < io.shift) {
					frequency := newFrequency
					shadowFreq := newFrequency
					calcFrequency() // For overflow check
				}
			}
		}
	}

	when (io.trigger) {
		shadowFreq := frequency

		when (io.period === 0.U) {
			sweepTimer := 8.U
		} .otherwise {
			sweepTimer := io.period
		}

		when (io.period =/= 0.U || io.shift =/= 0.U) {
			sweepEnabled := true.B
		}

		when (io.shift =/= 0.U) {
			calcFrequency()
		}
	}

	io.out := frequency
}
