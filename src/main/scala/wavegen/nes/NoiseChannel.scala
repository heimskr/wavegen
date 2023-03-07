package wavegen.nes

import chisel3._
import chisel3.util._

class NoiseChannel extends Module {
	val io = IO(new ChannelIO {
		val writes = Input(NoiseWrites())
	})

	val lengthHalt     = io.registers.$400C(5)
	val constantVolume = io.registers.$400C(4)
	val volumeParam    = io.registers.$400C(3, 0)
	val mode           = io.registers.$400E(7)
	val periodIndex    = io.registers.$400E(3, 0)
	val lengthLoad     = io.registers.$400F(7, 3)

	// NTSC values
	val periodTable   = VecInit(Seq(4, 8, 16, 32, 64, 96, 128, 160, 202, 254, 380, 508, 762, 1016, 2034, 4068).map(_.U(12.W)))
	val shiftRegister = RegInit(1.U(15.W))

	val lengthCounter = Module(new LengthCounter(4))
	lengthCounter.io.ticks     := io.ticks
	lengthCounter.io.registers := io.registers
	lengthCounter.io.loadValue := lengthLoad
	lengthCounter.io.write     := io.writes.length
	lengthCounter.io.halt      := lengthHalt

	val envelopeVolume = RegInit(0.U(4.W)) // Also known as the "divider"
	val decayCounter   = RegInit(0.U(4.W))
	val startFlag      = RegInit(false.B)
	val startNow       = io.writes.length
	val tickDivider    = WireInit(false.B)

	when (startNow) {
		startFlag := true.B
	}

	val lengthNonzero = lengthCounter.io.out =/= 0.U

	val counter = RegInit(0.U(12.W))

	def reloadCounter() = { counter := periodTable(periodIndex) }
	def doShift() = {
		val feedback = shiftRegister(0) ^ Mux(mode, shiftRegister(6), shiftRegister(1))
		shiftRegister := Cat(feedback, shiftRegister(14, 1))
	}

	when (io.ticks.apu) {
		when (startNow || startFlag) {
			reloadCounter()
			startFlag := false.B
		} .elsewhen (counter === 0.U) {
			reloadCounter()
			doShift()
		} .otherwise {
			counter := counter - 1.U
		}
	}

	when (io.ticks.quarter) {
		when (startNow || startFlag) {
			tickDivider := true.B
			when (envelopeVolume === 0.U) {
				envelopeVolume := volumeParam
				when (decayCounter === 0.U) {
					when (lengthHalt) {
						decayCounter := 15.U
					}
				} .otherwise {
					decayCounter := decayCounter - 1.U
				}
			} .otherwise {
				envelopeVolume := envelopeVolume - 1.U
			}
		} .otherwise {
			startFlag      := false.B
			decayCounter   := 15.U
			envelopeVolume := volumeParam
		}
	}

	val volume = Mux(constantVolume, volumeParam, envelopeVolume)

	io.out := Mux(shiftRegister(0) || lengthCounter.io.out === 0.U, 0.U, volume)
}
