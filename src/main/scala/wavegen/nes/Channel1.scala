package wavegen.nes

import chisel3._
import chisel3.util._

class Channel1 extends Module {
	val io = IO(new ChannelIO {
		val writes = Input(PulseWrites())
	})

	val timerValue = Cat(io.registers.$4003(2, 0), io.registers.$4002)
	val sweeper = Module(new FrequencySweeper(false))

	val period = RegInit(0.U(11.W))

	sweeper.io.ticks    := io.ticks
	sweeper.io.register := io.registers.$4001
	sweeper.io.reload   := io.writes.sweeper
	sweeper.io.periodIn := period

	when (sweeper.io.periodOut.valid) {
		period := sweeper.io.periodOut.bits
	}

	val waveforms = VecInit(Seq("b00000001".U, "b00000011".U, "b00001111".U, "b11111100".U))
	val waveformSelector = RegInit(0.U(3.W))
	val bit = waveforms(waveformSelector)

}
