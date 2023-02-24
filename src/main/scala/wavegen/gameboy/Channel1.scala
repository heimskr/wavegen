package wavegen.gameboy

import wavegen._
import chisel3._
import chisel3.util._

class Channel1(baseFreq: Int, freq256: Int = 256) extends Module {
	implicit val clockFreq = baseFreq

	val io = IO(new SquareChannelIO {
		val debug = Output(UInt(8.W))
	})

	val duty         = io.registers.NR11(7, 6)
	val lengthLoad   = io.registers.NR11(5, 0)
	val startVolume  = io.registers.NR12(7, 4)
	val addMode      = io.registers.NR12(3)
	val period       = io.registers.NR12(2, 0)
	val trigger      = io.registers.NR14(7)
	val lengthEnable = io.registers.NR14(6)
	val frequency    = Cat(io.registers.NR14(2, 0), io.registers.NR13)

	val sequencer = Module(new FrameSequencer(baseFreq))

	val sweeper = Module(new FrequencySweeper)
	sweeper.io.tick        := io.tick
	sweeper.io.trigger     := trigger
	sweeper.io.period      := io.registers.NR10(6, 4)
	sweeper.io.negate      := io.registers.NR10(3)
	sweeper.io.shift       := io.registers.NR10(2, 0)
	sweeper.io.frequencyIn := frequency

	// val sweepClocker = Module(new Clocker)
	// sweepClocker.io.enable     := true.B
	// sweepClocker.io.freq.bits  := sweeper.io.out
	// sweepClocker.io.freq.valid := true.B

	val envelope = Module(new Envelope)
	envelope.io.tick          := sequencer.io.envelope
	envelope.io.trigger       := trigger
	envelope.io.initialVolume := startVolume
	envelope.io.rising        := addMode
	envelope.io.period        := period

	val lengthCounter = Module(new LengthCounter)
	lengthCounter.io.tick      := sequencer.io.lengthCounter
	lengthCounter.io.trigger   := trigger
	lengthCounter.io.enable    := lengthEnable
	lengthCounter.io.loadValue := lengthLoad

	val waveforms = VecInit(Seq("b00000001".U, "b00000011".U, "b00001111".U, "b11111100".U))

	// val squareGen = Module(new SquareGen(1, 8))
	// squareGen.io.pause := !lengthCounter.io.channelOn
	// squareGen.io.freq  := sweeper.io.out
	// squareGen.io.max   := "b1".U
	// squareGen.io.wave  := waveforms(duty)

	val sweepClocker = Module(new PeriodClocker)
	sweepClocker.io.tickIn := io.tick
	sweepClocker.io.period.bits  := (2048.U - sweeper.io.out) << 2.U
	sweepClocker.io.period.valid := true.B

	val squareGen = Module(new SquareGenExternal(1, 8))
	squareGen.io.tick := sweepClocker.io.tickOut
	squareGen.io.max  := "b1".U
	squareGen.io.wave := waveforms(duty)

	io.out.bits  := 0.U
	io.out.valid := sweepClocker.io.period.valid
	io.debug     := sweeper.io.out >> 7.U

	when (lengthCounter.io.channelOn) {
		// io.out.bits := squareGen.io.out(0) * envelope.io.currentVolume
		io.out.bits := Mux(squareGen.io.out(0), "b1111".U, "b0000".U)
	}
}
