package wavegen.nes

import chisel3._
import chisel3.util._

class PulseChannel(twosComplement: Boolean) extends Module {
	val io = IO(new ChannelIO {
		val reg0   = Input(UInt(8.W))
		val reg1   = Input(UInt(8.W))
		val reg2   = Input(UInt(8.W))
		val reg3   = Input(UInt(8.W))
		val writes = Input(PulseWrites())
	})

	val dutyCycle      = io.reg0(7, 6)
	val lengthHalt     = io.reg0(5)
	val constantVolume = io.reg0(4)
	val volumeParam    = io.reg0(3, 0)
	val timerValue     = Cat(io.reg3(2, 0), io.reg2)

	val sweeper = Module(new FrequencySweeper(twosComplement))
	// val period  = RegInit(0.U(11.W))
	val period = timerValue
	val counter = RegInit(0.U(11.W))

	sweeper.io.ticks    := io.ticks
	sweeper.io.register := io.reg1
	sweeper.io.reload   := io.writes.sweeper
	sweeper.io.periodIn := period

	when (sweeper.io.periodOut.valid) {
		printf(cf"Setting period to ${sweeper.io.periodOut.bits}\n")
		// period  := sweeper.io.periodOut.bits
		counter := sweeper.io.periodOut.bits // TODO: verify
	}

	val waveforms        = VecInit(Seq("b00000001".U, "b00000011".U, "b00001111".U, "b11111100".U))
	val waveform         = waveforms(dutyCycle)
	val waveformSelector = RegInit(0.U(3.W))
	val waveBit          = waveform(waveformSelector)

	val envelopeVolume = RegInit(0.U(4.W)) // Also known as the "divider"
	val decayCounter   = RegInit(0.U(4.W))
	val startFlag      = RegInit(false.B)
	val startNow       = io.writes.length
	val clockDivider   = WireInit(false.B)

	when (startNow) {
		startFlag := true.B
	}

	when (io.ticks.apu) {
		when (startNow || startFlag) {
			counter := period
			startFlag := false.B
		}

		when (counter === 0.U) {
			counter          := period
			waveformSelector := waveformSelector - 1.U
		} .otherwise {
			counter := counter - 1.U
		}
	}

	when (io.ticks.quarter) {
		when (startNow || startFlag) {
			clockDivider := true.B
			when (envelopeVolume === 0.U) {
				envelopeVolume := timerValue
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
			envelopeVolume := timerValue
		}
	}

	val volume = Mux(constantVolume, volumeParam, envelopeVolume)

	val periodHighEnough = 8.U <= timerValue

	printf(cf"${waveBit} && ${!sweeper.io.mute} && ${counter =/= 0.U} && ${periodHighEnough}? ${volume} : 0, startFlag = ${startFlag}<-${startNow}\n")

	io.out := Mux(waveBit && !sweeper.io.mute && counter =/= 0.U && periodHighEnough, volume, 0.U(4.W))
}
