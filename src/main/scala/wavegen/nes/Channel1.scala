package wavegen.nes

import chisel3._
import chisel3.util._

class Channel1 extends Module {
	val io = IO(new ChannelIO {
		val writes = Input(PulseWrites())
	})

	val timerValue = Cat(io.registers.$4003(2, 0), io.registers.$4002)
	val sweeper = Module(new FrequencySweeper(false))

	sweeper.io.ticks    := io.ticks
	sweeper.io.register := io.registers.$4001
	sweeper.io.reload   := io.writes.sweeper
	// sweeper.io.

	val sequencer = RegInit(0.U(11.W))
	val pulse = sequencer === 0.U

	when (io.ticks.apu) {
		when (pulse) {
			sequencer := timerValue
		} .otherwise {
			sequencer := sequencer - 1.U
		}
	}

	val waveforms = VecInit(Seq("b00000001".U, "b00000011".U, "b00001111".U, "b11111100".U))


}
