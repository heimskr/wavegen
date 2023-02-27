package wavegen.gameboy

import wavegen._
import chisel3._
import chisel3.util._

class Channel1(baseFreq: Int, fsFreq: Int = -1) extends Module {
	implicit val clockFreq = baseFreq

	val io = IO(new ChannelIO {
		val debug = Output(UInt(8.W))
		val nr13  = Valid(UInt(8.W))
		val nr14  = Valid(UInt(8.W))
		val buttonD = Input(Bool())
		val buttonR = Input(Bool())
		val freq = Output(UInt(11.W))
	})

	val duty         = WireInit(io.registers.NR11(7, 6))
	val lengthLoad   = WireInit(io.registers.NR11(5, 0))
	val startVolume  = WireInit(io.registers.NR12(7, 4))
	val addMode      = WireInit(io.registers.NR12(3))
	val period       = WireInit(io.registers.NR12(2, 0))
	val trigger      = WireInit(io.registers.NR14(7))
	val lengthEnable = WireInit(io.registers.NR14(6))
	val frequency    = Cat(io.registers.NR14(2, 0), io.registers.NR13)

	val sequencer = Module(new FrameSequencer(if (fsFreq == -1) baseFreq else fsFreq))

	val sweeper = Module(new FrequencySweeper)
	sweeper.io.tick        := io.tick
	sweeper.io.trigger     := trigger
	sweeper.io.period      := io.registers.NR10(6, 4)
	sweeper.io.negate      := io.registers.NR10(3)
	sweeper.io.shift       := io.registers.NR10(2, 0)
	sweeper.io.frequencyIn := frequency
	sweeper.io.nr14In      := io.registers.NR14
	io.nr13 <> sweeper.io.nr13Out
	io.nr14 <> sweeper.io.nr14Out

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

	val latestFrequency = WireInit(frequency)
	when (sweeper.io.nr13Out.valid && sweeper.io.nr14Out.valid) {
		latestFrequency := Cat(sweeper.io.nr14Out.bits(2, 0), sweeper.io.nr13Out.bits)
	}

	val sweepClocker = Module(new PeriodClocker(14))
	sweepClocker.io.tickIn       := io.tick
	sweepClocker.io.period.bits  := (2048.U - latestFrequency) << 2.U
	sweepClocker.io.period.valid := true.B

	val squareGen = Module(new SquareGenExternal(1, 8))
	squareGen.io.tick := sweepClocker.io.tickOut
	squareGen.io.max  := "b1".U
	squareGen.io.wave := waveforms(duty)

	io.out   := 0.U
	io.debug := sweeper.io.out >> 7.U
	io.freq  := latestFrequency

	when (lengthCounter.io.channelOn || io.buttonD) {
		when (io.buttonR) {
			io.out := Mux(squareGen.io.out(0), "b1111".U, "b0000".U)
		} .otherwise {
			io.out := Mux(squareGen.io.out(0), envelope.io.currentVolume, 0.U)
		}
	}
}
